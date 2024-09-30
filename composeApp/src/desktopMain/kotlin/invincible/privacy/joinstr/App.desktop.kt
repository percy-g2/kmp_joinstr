package invincible.privacy.joinstr

import fr.acinq.bitcoin.Bitcoin
import fr.acinq.bitcoin.Block
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.Satoshi
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.ScriptWitness
import fr.acinq.bitcoin.SigHash.SIGHASH_ALL
import fr.acinq.bitcoin.SigHash.SIGHASH_ANYONECANPAY
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.TxId
import fr.acinq.bitcoin.TxIn
import fr.acinq.bitcoin.TxOut
import fr.acinq.bitcoin.psbt.Input
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.psbt.UpdateFailure
import fr.acinq.bitcoin.utils.Either
import fr.acinq.bitcoin.utils.flatMap
import fr.acinq.secp256k1.Hex
import fr.acinq.secp256k1.Secp256k1
import fr.acinq.secp256k1.Secp256k1.Companion.pubKeyTweakMul
import invincible.privacy.joinstr.model.CoinJoinHistory
import invincible.privacy.joinstr.model.ListUnspentResponseItem
import invincible.privacy.joinstr.model.LocalPoolContent
import invincible.privacy.joinstr.utils.NodeConfig
import invincible.privacy.joinstr.utils.SettingsStore
import invincible.privacy.joinstr.utils.Theme
import io.github.aakira.napier.Napier
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import kotlinx.datetime.Clock
import net.harawata.appdirs.AppDirsFactory
import okio.Path.Companion.toPath
import java.awt.Desktop
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.io.File
import java.net.URI
import javax.swing.JOptionPane
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.seconds

actual object LocalNotification {

    actual fun showNotification(title: String, message: String) {
        if (SystemTray.isSupported()) {
            val tray = SystemTray.getSystemTray()
            val image = Toolkit.getDefaultToolkit().createImage("logo.webp")
            val trayIcon = TrayIcon(image, title)
            tray.add(trayIcon)
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO)
        } else {
            // Fallback for systems that don't support SystemTray
            JOptionPane.showMessageDialog(
                null,
                message,
                title,
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    actual suspend fun requestPermission(): Boolean {
        // Desktop usually doesn't require explicit permission for notifications
        return true
    }
}

actual fun getSettingsStore(): KStore<SettingsStore> {
    val directory = AppDirsFactory.getInstance()
        .getUserDataDir("invincible.privacy.joinstr", "1.0.0", "invincible")
    if (File(directory).exists().not()) {
        File(directory).mkdirs()
    }
    return storeOf<SettingsStore>(
        file = "${directory}/settings.json".toPath(),
        default = SettingsStore(
            selectedTheme = Theme.SYSTEM.id,
            nodeConfig = NodeConfig(),
            nostrRelay = "wss://nostr.fmt.wiz.biz"
        )
    )
}

actual fun getPoolsStore(): KStore<List<LocalPoolContent>> {
    val directory = AppDirsFactory.getInstance()
        .getUserDataDir("invincible.privacy.joinstr", "1.0.0", "invincible")
    if (File(directory).exists().not()) {
        File(directory).mkdirs()
    }
    return storeOf<List<LocalPoolContent>>(
        file = "${directory}/pools.json".toPath(),
        default = emptyList()
    )
}

actual fun getHistoryStore(): KStore<List<CoinJoinHistory>> {
    val directory = AppDirsFactory.getInstance()
        .getUserDataDir("invincible.privacy.joinstr", "1.0.0", "invincible")
    if (File(directory).exists().not()) {
        File(directory).mkdirs()
    }
    return storeOf<List<CoinJoinHistory>>(
        file = "${directory}/coin_join_history.json".toPath(),
        default = emptyList()
    )
}

actual fun getWebSocketClient(): HttpClient {
    return HttpClient(CIO) {
        install(WebSockets)

        install(HttpTimeout) {
            requestTimeoutMillis = 5.seconds.inWholeMilliseconds
            connectTimeoutMillis = 5.seconds.inWholeMilliseconds
            socketTimeoutMillis = 5.seconds.inWholeMilliseconds
        }

        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.NONE
        }
    }
}

actual fun pubkeyCreate(privateKey: ByteArray): ByteArray {
    if (!Secp256k1.secKeyVerify(privateKey)) throw Exception("Invalid private key!")
    val pubKey = Secp256k1.pubkeyCreate(privateKey).drop(1).take(32).toByteArray()
    //context.cleanup()
    return pubKey
}

actual suspend fun signSchnorr(content: ByteArray, privateKey: ByteArray, freshRandomBytes: ByteArray): ByteArray {
    val contentSignature = Secp256k1.signSchnorr(content, privateKey, freshRandomBytes)
    return contentSignature
}

actual fun getSharedSecret(privateKey: ByteArray, pubKey: ByteArray): ByteArray =
    pubKeyTweakMul(Hex.decode("02") + pubKey, privateKey).copyOfRange(1, 33)

// desktop
@OptIn(ExperimentalEncodingApi::class)
actual suspend fun createPsbt(
    poolId: String,
    unspentItem: ListUnspentResponseItem
): String? {
    val activePools = getPoolsStore().get()
        ?.filter { it.timeout > (Clock.System.now().toEpochMilliseconds() / 1000) }
        ?.sortedByDescending { it.timeout }

    val selectedPool = activePools?.find { it.id == poolId }
        ?: throw IllegalStateException("Selected pool not found")

    val poolAmount = selectedPool.denomination
    val selectedTxAmount = unspentItem.amount
    val estimatedVByteSize = 100 * selectedPool.peers
    val estimatedBtcFee = (selectedPool.feeRate.toFloat() * estimatedVByteSize.toFloat()) / 100000000

    val outputAmount = poolAmount - estimatedBtcFee
    val sighashType = SIGHASH_ALL or SIGHASH_ANYONECANPAY

    val input = TxIn(
        outPoint = OutPoint(TxId(unspentItem.txid), unspentItem.vout.toLong()),
        sequence = 0xFFFFFFFFL,
        signatureScript = listOf()
    )

    val outputs = selectedPool.peersData
        .filter { it.type == "output" }
        .sortedWith(compareBy { it.address })
        .mapNotNull { peerData ->
            Bitcoin.addressToPublicKeyScript(Block.SignetGenesisBlock.hash, peerData.address ?: "").fold(
                { error ->
                    Napier.e("Error creating output script for address ${peerData.address}: ${error.message}")
                    null
                },
                { scriptElts ->
                    TxOut(
                        amount = Satoshi((outputAmount * 100_000_000).toLong()),
                        publicKeyScript = ByteVector(Script.write(scriptElts))
                    )
                }
            )
        }

    val transaction = Transaction(
        version = 1L,
        txIn = listOf(input),
        txOut = outputs,
        lockTime = 0
    )

    val unsignedPsbt = Psbt(transaction)

    val updatedPsbt = unsignedPsbt.updateWitnessInput(
        outPoint = input.outPoint,
        txOut = TxOut(
            Satoshi((selectedTxAmount * 100_000_000).toLong()),
            ByteVector(unspentItem.scriptPubKey)
        ),
        sighashType = sighashType
    ).right ?: throw IllegalStateException("Failed to update PSBT")

    // Encode PSBT to Base64
    val psbtBytes = Psbt.write(updatedPsbt)
    val psbtBase64 = Base64.encode(psbtBytes.toByteArray())

    return psbtBase64
}

fun joinUniqueOutputs(vararg psbts: Psbt): Either<UpdateFailure, Psbt> {
    return when {
        psbts.isEmpty() -> Either.Left(UpdateFailure.CannotJoin("no psbt provided"))
        psbts.map { it.global.version }.toSet().size != 1 -> Either.Left(UpdateFailure.CannotJoin("cannot join psbts with different versions"))
        psbts.map { it.global.tx.version }.toSet().size != 1 -> Either.Left(UpdateFailure.CannotJoin("cannot join psbts with different tx versions"))
        psbts.map { it.global.tx.lockTime }.toSet().size != 1 -> Either.Left(UpdateFailure.CannotJoin("cannot join psbts with different tx lockTime"))
        psbts.any { it.global.tx.txIn.size != it.inputs.size || it.global.tx.txOut.size != it.outputs.size } -> Either.Left(UpdateFailure.CannotJoin("some psbts have an invalid number of inputs/outputs"))
        psbts.flatMap { it.global.tx.txIn.map { txIn -> txIn.outPoint } }.toSet().size != psbts.sumOf { it.global.tx.txIn.size } -> Either.Left(UpdateFailure.CannotJoin("cannot join psbts that spend the same input"))
        else -> {
            val outputsWithData = psbts.flatMap { psbt ->
                psbt.global.tx.txOut.zip(psbt.outputs)
            }.distinctBy { (txOut, _) -> txOut.toString() }

            val sortedOutputsWithData = outputsWithData.sortedWith { a, b ->
                compareBy<Pair<TxOut, Any>> { (txOut, _) ->
                    extractAddressFromPublicKeyScript(txOut.publicKeyScript)
                }.compare(a, b)
            }

            val (uniqueOutputs, uniqueOutputData) = sortedOutputsWithData.unzip()

            if (uniqueOutputs.size != uniqueOutputData.size) {
                Either.Left(UpdateFailure.CannotJoin("mismatch between unique outputs and output data"))
            } else {
                val mergedInputs = psbts.flatMap { psbt ->
                    psbt.inputs.map { input ->
                        when (input) {
                            is Input.WitnessInput.PartiallySignedWitnessInput -> input.copy(
                                partialSigs = psbts.flatMap { it.inputs }.filterIsInstance<Input.WitnessInput.PartiallySignedWitnessInput>()
                                    .flatMap { it.partialSigs.entries }.associate { it.key to it.value }
                            )
                            is Input.NonWitnessInput.PartiallySignedNonWitnessInput -> input.copy(
                                partialSigs = psbts.flatMap { it.inputs }.filterIsInstance<Input.NonWitnessInput.PartiallySignedNonWitnessInput>()
                                    .flatMap { it.partialSigs.entries }.associate { it.key to it.value }
                            )
                            else -> input
                        }
                    }
                }

                val global = psbts[0].global.copy(
                    tx = psbts[0].global.tx.copy(
                        txIn = psbts.flatMap { it.global.tx.txIn },
                        txOut = uniqueOutputs
                    ),
                    extendedPublicKeys = psbts.flatMap { it.global.extendedPublicKeys }.distinct(),
                    unknown = psbts.flatMap { it.global.unknown }.distinct()
                )
                Either.Right(Psbt(global, mergedInputs, uniqueOutputData))
            }
        }
    }
}

fun extractAddressFromPublicKeyScript(publicKeyScript: ByteVector): String {
    return Bitcoin.addressFromPublicKeyScript(Block.SignetGenesisBlock.hash, publicKeyScript.toByteArray()).right!!
}

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun joinPsbts(listOfPsbts: List<String>): Pair<String?, String?> {
    val psbts = listOfPsbts.mapNotNull { Psbt.read(Base64.decode(it.toByteArray())).right }
    val joinedPsbt = joinUniqueOutputs(*psbts.toTypedArray())

    // Log information about the joined PSBT
    joinedPsbt.fold(
        { error ->  Napier.e("Error joining PSBTs: $error") },
        { psbt ->
            Napier.i("Joined PSBT information:")
            Napier.i("Number of inputs: ${psbt.inputs.size}")
            Napier.i("Number of outputs: ${psbt.outputs.size}")
            psbt.inputs.forEachIndexed { index, input ->
                Napier.i("Input $index:")
                when (input) {
                    is Input.WitnessInput.PartiallySignedWitnessInput -> {
                        Napier.i("  Type: Witness Input")
                        Napier.i("  Partial signatures: ${input.partialSigs.size}")
                        input.partialSigs.forEach { (pubKey, sig) ->
                            Napier.i("    Public Key: ${pubKey.value.toHex()}")
                            Napier.i("    Signature: ${sig.toHex()}")
                        }
                        Napier.i("  Witness UTXO: ${input.txOut}")
                        Napier.i("  Redeem script: ${input.redeemScript}")
                        Napier.i("  Witness script: ${input.witnessScript}")
                    }
                    else ->  Napier.i("  Type: Other (${input.javaClass.simpleName})")
                }
            }
        }
    )

    // Finalize inputs and preserve witness data
    val finalizedPsbt = joinedPsbt.flatMap { psbt ->
        psbt.inputs.foldIndexed(Either.Right(psbt) as Either<UpdateFailure, Psbt>) { index, accPsbt, input ->
            accPsbt.flatMap { currentPsbt ->
                when (input) {
                    is Input.WitnessInput.PartiallySignedWitnessInput -> {
                        val pubKeyHash = input.txOut.publicKeyScript.drop(2) // Remove the first two bytes (00 and 14)
                        val matchingEntry = input.partialSigs.entries.find { (pubKey, _) ->
                            val hash = pubKey.value.sha256().ripemd160()
                            hash.contentEquals(pubKeyHash.toByteArray())
                        }
                        if (matchingEntry != null) {
                            val (pubKey, sig) = matchingEntry
                            val scriptWitness = ScriptWitness(listOf(sig, pubKey.value))
                            currentPsbt.finalizeWitnessInput(index, scriptWitness)
                        } else {
                            Either.Left(UpdateFailure.CannotFinalizeInput(index, "No matching public key found for input"))
                        }
                    }
                    else -> Either.Right(currentPsbt) // Already finalized or cannot be finalized
                }
            }
        }
    }

    finalizedPsbt.fold(
        { error ->
            Napier.e("Error finalizing PSBT: $error")
            null
        },
        { psbt ->
            // Log information about the finalized PSBT
            Napier.i("Finalized PSBT information:")
            psbt.inputs.forEachIndexed { index, input ->
                Napier.i("Input $index:")
                when (input) {
                    is Input.WitnessInput.FinalizedWitnessInput -> {
                        Napier.i("  Type: Finalized Witness Input")
                        Napier.i("  Script Witness: ${input.scriptWitness}")
                        Napier.i("  Script Sig: ${input.scriptSig}")
                    }
                    else ->  Napier.i("  Type: Other (${input.javaClass.simpleName})")
                }
            }

            // Extract the final transaction
            val extractedTx = psbt.extract()
            extractedTx.fold(
                { error ->
                    Napier.e("Error extracting transaction: $error")
                    null
                },
                { tx ->
                    // Convert the transaction to hex string
                    val txHex = Transaction.write(tx)
                    Napier.i("Joined and finalized transaction: $txHex")
                    // Log detailed information about the extracted transaction
                    Napier.i("Extracted transaction information:")
                    Napier.i("Version: ${tx.version}")
                    Napier.i("Locktime: ${tx.lockTime}")
                    tx.txIn.forEachIndexed { index, txIn ->
                        Napier.i("Input $index:")
                        Napier.i("  Outpoint: ${txIn.outPoint}")
                        Napier.i("  Sequence: ${txIn.sequence}")
                        Napier.i("  ScriptSig: ${txIn.signatureScript.toHex()}")
                        Napier.i("  Witness: ${txIn.witness}")
                    }
                    tx.txOut.forEachIndexed { index, txOut ->
                        Napier.i("Output $index:")
                        Napier.i("  Amount: ${txOut.amount}")
                        Napier.i("  ScriptPubKey: ${txOut.publicKeyScript.toHex()}")
                    }
                    txHex
                }
            )
        }
    )

    val psbtBytes = finalizedPsbt.right?.let { Psbt.write(it) }
    val psbtBase64 = psbtBytes?.toByteArray()?.let { Base64.encode(it) }
    return Pair(psbtBase64, finalizedPsbt.right?.extract()?.right.toString())
}

actual fun openLink(link: String) {
    Desktop.getDesktop().browse(URI(link))
}