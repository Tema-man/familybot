package dev.storozhenko.familybot.feature.tiktok

import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.executor.Executor
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.TikTokDownload
import dev.storozhenko.familybot.getLogger
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.bots.AbsSender
import java.io.File
import java.util.*

@Component
class TikTokDownloadExecutor(
    private val easyKeyValueService: EasyKeyValueService,
    private val botConfig: BotConfig
) : Executor {
    private val log = getLogger()

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        val urls = getTikTokUrls(context)
        return { sender ->
            urls.forEach { url ->
                sender.execute(SendChatAction(context.chat.idString, "upload_video", null))
                val downloadedFile = download(url)
                val video = SendVideo
                    .builder()
                    .video(InputFile(downloadedFile))
                    .chatId(context.chat.id)
                    .replyToMessageId(context.message.messageId)
                    .build()
                sender.execute(video)
                downloadedFile.delete()
            }
            null
        }
    }

    override fun canExecute(context: ExecutorContext): Boolean =
        botConfig.ytdlLocation != null &&
            getTikTokUrls(context).isNotEmpty() &&
            easyKeyValueService.get(TikTokDownload, context.chatKey, false)

    override fun priority(context: ExecutorContext) = Priority.MEDIUM

    private fun getTikTokUrls(context: ExecutorContext): List<String> {
        return context
            .message
            .entities
            ?.filter { it.type == "url" }
            ?.mapNotNull { it.text }
            ?.filter(::containsUrl)
            ?: emptyList()
    }

    private fun download(url: String): File {
        val filename = "/tmp/${UUID.randomUUID()}.mp4"
        val process = ProcessBuilder(botConfig.ytdlLocation, url, "-o", filename).start()
        log.info("Running yt-dlp...")
        process.inputStream.reader(Charsets.UTF_8).use {
            log.info(it.readText())
        }
        process.waitFor()
        log.info("Finished running yt-dlp")
        return File(filename)
    }

    private fun containsUrl(text: String): Boolean =
        text.contains("instagram.com/reel", ignoreCase = true)
            || text.contains("tiktok", ignoreCase = true)
}
