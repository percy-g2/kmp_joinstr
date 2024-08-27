package invincible.privacy.joinstr.ktx

fun String.hexToByteArray(): ByteArray =
    ByteArray(length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }