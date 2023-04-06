package dev.storozhenko.familybot.feature.huificator

import dev.storozhenko.familybot.common.extensions.dropLastDelimiter
import dev.storozhenko.familybot.common.extensions.randomBoolean
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.Executor
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.core.model.message.SimpleTextMessage
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.TalkingDensity
import dev.storozhenko.familybot.telegram.send
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender
import java.util.regex.Pattern

@Component
class HuificatorExecutor(
    private val easyKeyValueService: EasyKeyValueService
) : Executor, Configurable {

    override fun getFunctionId(context: ExecutorContext) = FunctionId.HUIFICATE

    override fun priority(context: ExecutorContext) = Priority.LOWEST

    override fun canExecute(context: ExecutorContext): Boolean = shouldHuificate(context)

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Message? {
        val huifyed = huify(context.message.text.orEmpty()) ?: return { null }
        return { it ->
//            it.send(context, huifyed, shouldTypeBeforeSend = true)
            SimpleTextMessage(huifyed, context)
        }
    }

    fun huify(word: String): String? {
        val lastWord = getLastWord(word).lowercase()
        return when {
            lastWord.length < 5 -> null
            english.matcher(lastWord).matches() -> null
            nonLetters.matcher(lastWord.dropLast(lastWord.length - 3)).matches() -> null
            onlyDashes.matcher(lastWord).matches() -> null
            lastWord.startsWith("ху", true) -> null
            else -> {
                val postfix = String(lastWord.toCharArray().dropWhile { !vowels.contains(it) }.toCharArray())
                when {
                    postfix.isEmpty() -> "хуе" + lastWord.drop(2)
                    rules.containsKey(postfix[0]) -> "ху" + rules[postfix[0]] + postfix.drop(1).dropLastDelimiter()
                    else -> "ху$postfix"
                }
            }
        }
    }

    private fun getLastWord(text: String) = text.split(regex = spaces).last()

    private fun shouldHuificate(context: ExecutorContext): Boolean {
        val density = getTalkingDensity(context)
        return if (density <= 3L) true else randomBoolean(density)
    }

    private fun getTalkingDensity(context: ExecutorContext): Long =
        easyKeyValueService.get(TalkingDensity, context.chatKey, 7)

    companion object {
        private const val vowels = "ёэоеаяуюыи"
        private val rules = mapOf('о' to "ё", 'а' to "я", 'у' to "ю", 'ы' to "и", 'э' to "е")
        private val nonLetters = Pattern.compile(".*[^a-я]+.*")
        private val onlyDashes = Pattern.compile("^-*$")
        private val english = Pattern.compile(".*[A-Za-z]+.*")
        private val spaces = Pattern.compile("\\s+")
    }
}
