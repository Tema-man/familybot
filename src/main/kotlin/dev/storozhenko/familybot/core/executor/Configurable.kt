package dev.storozhenko.familybot.core.executor

import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId

interface Configurable {
    fun getFunctionId(context: ExecutorContext): FunctionId
}
