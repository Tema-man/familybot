package dev.storozhenko.familybot.core.model

import dev.storozhenko.familybot.core.services.settings.ChatEasyKey

data class Chat(
    val id: Long,
    val name: String?,
    val idString: String = id.toString(),
    val isGroup: Boolean = false,
) {
    val key = ChatEasyKey(id)
}
