package dev.storozhenko.familybot.common

import dev.storozhenko.familybot.core.executor.Executor
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.telegram.TelegramBot
import io.micrometer.core.instrument.MeterRegistry
import org.telegram.telegrambots.meta.bots.AbsSender

fun Executor.meteredExecute(
    context: ExecutorContext,
    meterRegistry: MeterRegistry
): suspend (AbsSender) -> Action? {
    return meterRegistry
        .timer("executors.${this::class.simpleName}.execute")
        .recordCallable {
            this.execute(context)
        }
        ?: throw TelegramBot.InternalException("Something has gone wrong while calling metered executor")
}

fun Executor.meteredCanExecute(context: ExecutorContext, meterRegistry: MeterRegistry): Boolean {
    return meterRegistry
        .timer("executors.${this::class.simpleName}.canExecute")
        .recordCallable {
            this.canExecute(context)
        }
        ?: throw TelegramBot.InternalException("Something has gone wrong while calling metered executor")
}

fun Executor.meteredPriority(
    context: ExecutorContext,
    meterRegistry: MeterRegistry
): Priority {
    return meterRegistry
        .timer("executors.${this::class.simpleName}.priority")
        .recordCallable {
            this.priority(context)
        }
        ?: throw TelegramBot.InternalException("Something has gone wrong while calling metered executor")
}