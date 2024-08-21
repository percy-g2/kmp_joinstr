package invincible.privacy.joinstr.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class Transaction(
    val address: String,
    val parent_descs: List<String>,
    val category: String,
    val amount: Double,
    val label: String?,
    val vout: Int,
    val abandoned: Boolean,
    val confirmations: Int,
    val blockhash: String,
    val blockheight: Int,
    val blockindex: Int,
    val blocktime: Long,
    val txid: String,
    val wtxid: String,
    val walletconflicts: List<String>,
    @Serializable(with = LongToFormattedDateSerializer::class)
    val time: String,
    @Serializable(with = LongToFormattedDateSerializer::class)
    val timereceived: String,
    @SerialName("bip125-replaceable")
    val bip125Replaceable: String
)

@Serializable
data class TransactionsResponse(
    val result: List<Transaction>,
    val error: String?,
    val id: String?
)

object LongToFormattedDateSerializer : KSerializer<String> {
    @OptIn(FormatStringsInDatetimeFormats::class)
    private val dateTimeFormat = LocalDateTime.Format {
        byUnicodePattern("yyyy-MM-dd HH:mm:ss")
    }

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LongToFormattedDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        val epochMillis = value.toLong()
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val currentZoneTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val formattedTime = dateTimeFormat.format(currentZoneTime)
        encoder.encodeString(formattedTime)
    }

    override fun deserialize(decoder: Decoder): String {
        val epochMillis = decoder.decodeString().toLong() * 1000L
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val currentZoneTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return dateTimeFormat.format(currentZoneTime)
    }
}

