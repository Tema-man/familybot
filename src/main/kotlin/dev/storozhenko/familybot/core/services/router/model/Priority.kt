package dev.storozhenko.familybot.core.services.router.model

enum class Priority(val score: Byte) {

    HIGHEST(Byte.MAX_VALUE),
    HIGH(64),
    MEDIUM(0),
    LOW(-64),
    LOWEST(Byte.MIN_VALUE);

    infix fun higherThan(other: Priority): Boolean = this.score > other.score
}
