package dev.storozhenko.familybot.core.services.talking

import dev.storozhenko.familybot.core.services.router.model.ExecutorContext

interface TalkingService {

    suspend fun getReplyToUser(context: ExecutorContext, shouldBeQuestion: Boolean = false): String

}
