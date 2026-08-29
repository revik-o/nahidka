package org.orev.nahidka

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform