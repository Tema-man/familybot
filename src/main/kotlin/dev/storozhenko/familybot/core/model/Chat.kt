package dev.storozhenko.familybot.core.model

data class Chat(
    val id: Long,
    val name: String?,
    val idString: String = id.toString()
)
