package space.yaroslav.familybot.route.models

enum class MessageContentType(val id: Int) {
    PHOTO(1),
    AUDIO(2),
    ANIMATION(3),
    DOCUMENT(4),
    VOICE(5),
    VIDEO_NOTE(6),
    LOCATION(7),
    TEXT(8),
    STICKER(9),
    CONTACT(10),
    VIDEO(11)
}
