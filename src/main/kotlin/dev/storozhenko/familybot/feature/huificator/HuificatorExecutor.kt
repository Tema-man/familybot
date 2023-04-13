package dev.storozhenko.familybot.feature.huificator

import dev.storozhenko.familybot.common.extensions.dropLastDelimiter
import dev.storozhenko.familybot.common.extensions.randomBoolean
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.IntentExecutor
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.model.intent.TextMessageIntent
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.TalkingDensity
import dev.storozhenko.familybot.telegram.send
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class HuificatorExecutor(
    private val easyKeyValueService: EasyKeyValueService
) : IntentExecutor, Configurable {

    override fun getFunctionId(context: ExecutorContext) = FunctionId.HUIFICATE

    override val priority: Priority = Priority.LOWEST

    override fun canExecute(intent: Intent): Boolean = shouldHuificate(intent)

    override fun execute(intent: Intent): Action? {
        if (intent !is TextMessageIntent) return null
        val huifyed = huify(intent.text) ?: return null
        return SendTextAction(text = huifyed, chat = intent.chat)
    }

    private fun huify(word: String): String? {
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

    private fun shouldHuificate(intent: Intent): Boolean {
        if (intent !is TextMessageIntent) return false
        val density = getTalkingDensity(intent)
        return if (density <= 3L) true else randomBoolean(density)
    }

    private fun getTalkingDensity(intent: Intent): Long =
        easyKeyValueService.get(TalkingDensity, intent.chat.key, 7)

    companion object {
        private const val vowels = "ёэоеаяуюыи"
        private val rules = mapOf('о' to "ё", 'а' to "я", 'у' to "ю", 'ы' to "и", 'э' to "е")
        private val nonLetters = Pattern.compile(".*[^a-я]+.*")
        private val onlyDashes = Pattern.compile("^-*$")
        private val english = Pattern.compile(".*[A-Za-z]+.*")
        private val spaces = Pattern.compile("\\s+")
    }
}
