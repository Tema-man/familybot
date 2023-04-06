package dev.storozhenko.familybot.core.services.talking

import dev.storozhenko.familybot.common.extensions.readTomlFromStatic
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.services.talking.model.PhraseTheme
import dev.storozhenko.familybot.telegram.TelegramBot
import org.springframework.stereotype.Component
import org.tomlj.TomlTable

@Component
class DictionaryReader {
    private val dictionary: Map<Phrase, Map<PhraseTheme, List<String>>>

    init {
        val toml = readTomlFromStatic("dictionary.toml")

        dictionary = Phrase.values()
            .map { phrase ->
                phrase to (toml.getTable(phrase.name)
                    ?: throw TelegramBot.InternalException("Phrase $phrase is missing"))
            }
            .associate { (phrase, table) -> phrase to parsePhrasesByTheme(table) }
        checkDefaults(dictionary)
    }

    fun getPhrases(phrase: Phrase, theme: PhraseTheme): List<String> {
        val phrasesByTheme = getPhraseContent(phrase)
        val requiredPhrases = phrasesByTheme[theme]
        return if (requiredPhrases.isNullOrEmpty()) {
            phrasesByTheme[PhraseTheme.DEFAULT]
                ?: throw TelegramBot.InternalException("Default value for phrase $phrase is missing")
        } else {
            requiredPhrases
        }
    }

    fun getAllPhrases(phrase: Phrase): List<String> {
        val byThemes = getPhraseContent(phrase)
        return byThemes.map(Map.Entry<PhraseTheme, List<String>>::value).flatten()
    }

    private fun getPhraseContent(phrase: Phrase): Map<PhraseTheme, List<String>> {
        return dictionary[phrase]
            ?: throw TelegramBot.InternalException("Phrase $phrase is missing")
    }

    private fun parsePhrasesByTheme(table: TomlTable): Map<PhraseTheme, List<String>> {
        return PhraseTheme
            .values()
            .associateWith { theme -> tableToList(table, theme) }
    }

    private fun tableToList(
        table: TomlTable,
        theme: PhraseTheme
    ): List<String> {
        return table
            .getArray(theme.name)
            ?.toList()
            ?.map(Any::toString)
            ?.map { line -> line.replace("\r", "") }
            ?: emptyList()
    }

    private fun checkDefaults(dictionary: Map<Phrase, Map<PhraseTheme, List<String>>>) {
        val missingDefaultPhrases = dictionary
            .filter { entry -> entry.value[PhraseTheme.DEFAULT].isNullOrEmpty() }
            .map { (key) -> key }

        if (missingDefaultPhrases.isNotEmpty()) {
            throw TelegramBot.InternalException(
                "Some dictionary defaults missing. " +
                        "Check $missingDefaultPhrases"
            )
        }
    }
}
