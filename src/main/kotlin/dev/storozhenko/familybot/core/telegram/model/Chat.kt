package dev.storozhenko.familybot.core.telegram.model

data class Chat(val id: Long, val name: String?, val idString: String = id.toString())
