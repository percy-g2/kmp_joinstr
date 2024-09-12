package invincible.privacy.joinstr

import fr.acinq.bitcoin.Bitcoin
import fr.acinq.bitcoin.Block
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.Satoshi
import fr.acinq.bitcoin.Script
import fr.acinq.bitcoin.SigHash.SIGHASH_ALL
import fr.acinq.bitcoin.SigHash.SIGHASH_ANYONECANPAY
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.TxHash
import fr.acinq.bitcoin.TxId
import fr.acinq.bitcoin.TxIn
import fr.acinq.bitcoin.TxOut
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.secp256k1.Hex
import fr.acinq.secp256k1.Secp256k1
import fr.acinq.secp256k1.Secp256k1.Companion.pubKeyTweakMul
import invincible.privacy.joinstr.model.ListUnspentResponseItem
import invincible.privacy.joinstr.model.LocalPoolContent
import invincible.privacy.joinstr.ui.components.SnackbarController
import invincible.privacy.joinstr.utils.NodeConfig
import invincible.privacy.joinstr.utils.SettingsStore
import invincible.privacy.joinstr.utils.Theme
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
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.io.File
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
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

actual fun Float.convertFloatExponentialToString(): String {
    val mathContext = MathContext(8, RoundingMode.HALF_UP)
    return BigDecimal(toString()).round(mathContext).toPlainString()
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
            level = LogLevel.ALL
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
        outPoint = OutPoint(TxId(TxHash(unspentItem.txid)), unspentItem.vout.toLong()),
        sequence = 0xFFFFFFFFL,
        signatureScript = listOf()
    )

    val outputs = selectedPool.peersData
        .filter { it.type == "output" }
        .mapNotNull { peerData ->
            Bitcoin.addressToPublicKeyScript(Block.SignetGenesisBlock.hash, peerData.address).fold(
                { error ->
                    println("Error creating output script for address ${peerData.address}: ${error.message}")
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

    if (!((poolAmount * 100_000_000) + 500 <= selectedTxAmount * 100_000_000 &&
            selectedTxAmount * 100_000_000 <= (poolAmount * 100_000_000) + 5000)
    ) {
        SnackbarController.showMessage("Error: Selected input value is not within the specified range for this pool " +
            "(denomination: $poolAmount BTC)")
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
