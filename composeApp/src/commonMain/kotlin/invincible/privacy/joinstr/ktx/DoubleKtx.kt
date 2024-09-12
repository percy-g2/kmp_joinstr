package invincible.privacy.joinstr.ktx


fun Double.toExactString(): String {
    return this.toString().let { str ->
        if ('e' in str || 'E' in str) {
            val parts = str.split('e', 'E')
            val base = parts[0].replace(".", "")
            val exponent = parts[1].toInt()
            if (exponent > 0) {
                base.padEnd(exponent + 1, '0').replaceFirst("^0+(?!$)".toRegex(), "")
            } else {
                "0." + "0".repeat(-exponent - 1) + base.replace("-", "")
            }
        } else {
            str
        }
    }
}