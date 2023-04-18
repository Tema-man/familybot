package dev.storozhenko.familybot.core.model

import dev.storozhenko.familybot.core.services.settings.UserEasyKey

data class User(
    val id: Long,
    @Deprecated("Chat will be removed from user model", replaceWith = ReplaceWith("Intent.chat")) val chat: Chat,
    val name: String?,
    val nickname: String?,
    val role: Role = Role.USER
) {

    val key = UserEasyKey(id)

    fun getGeneralName(mention: Boolean = true): String {
        return if (mention) {
            if (nickname != null) {
                "@$nickname"
            } else {
                "<a href=\"F$id\">$name</a>"
            }
        } else {
            name ?: "хуй знает кто"
        }
    }

    enum class Role {
        BOT, DEVELOPER, ADMIN, USER, SYSTEM
    }

    companion object {
        val SYSTEM = User(
            id = Long.MIN_VALUE,
            chat = Chat(
                id = Long.MIN_VALUE,
                name = null,
                idString = "",
                isGroup = false
            ),
            name = null,
            nickname = null,
            role = Role.SYSTEM
        )
    }
}
