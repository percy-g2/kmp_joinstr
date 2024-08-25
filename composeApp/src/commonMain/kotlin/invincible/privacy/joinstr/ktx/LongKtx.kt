package invincible.privacy.joinstr.ktx

import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Long.displayDateTime(): String {
    val instant = Instant.fromEpochMilliseconds(this * 1000)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val month = localDateTime.month.name.take(3).lowercase().capitalize(Locale.current)
    val date = localDateTime.dayOfMonth.toString().padStart(2, '0')
    val year = localDateTime.year.toString()

    val hour = if (localDateTime.hour % 12 == 0) 12 else localDateTime.hour % 12
    val minute = localDateTime.minute.toString().padStart(2, '0')
    val second = localDateTime.second.toString().padStart(2, '0')
    val period = if (localDateTime.hour < 12) "am" else "pm"
    return "$hour:$minute:$second $period $month $date $year"
}