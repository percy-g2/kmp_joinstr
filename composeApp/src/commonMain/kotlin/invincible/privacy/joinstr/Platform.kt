package invincible.privacy.joinstr

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform