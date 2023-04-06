package dev.storozhenko.familybot.core.model

enum class Command(
    val command: String,
    val id: Int,
    val description: String
) {
    STATS_MONTH("/stats_month", 1, "Че там по пидорам за весь месяц?"),
    STATS_YEAR("/stats_year", 2, "Че там по пидорам за весь год?"),
    STATS_TOTAL("/stats_total", 3, "Че там по пидорам за все время?"),
    PIDOR("/pidor", 4, "Сегодняшний пидор"),
    QUOTE("/quote", 5, "Цитата"),
    COMMAND_STATS("/command_stats", 6, "Статистика команд"),
    RAGE("/rage", 7, "Сделай боту больно. Пройдет через 10 минут или 20 сообщений. Работает раз в день."),
    LEADERBOARD("/leaderboard", 8, "Лучшие среди нас"),
    HELP("/help", 9, "Помогите 🙁"),
    SETTINGS("/settings", 10, "Опции внутри чата"),
    ANSWER("/answer", 11, "Преодолеть муки выбора. Использование: [вариант] или [другой вариант] (сколько угодно \"или\")"),
    QUOTE_BY_TAG("/quotebytag", 12, "Цитата по тегу"),
    ROULETTE("/legacy_roulette", 13, "Старая рулетка, новый вариант в /bet"),
    ASK_WORLD("/ask_world", 14, "Спроси мир. Для подробностей вызовите команду."),
    STATS_WORLD("/stats_world", 15, "Статистика по всем чатам"),
    ME("/me", 16, "Ваша пидорская статистика"),
    TOP_HISTORY("/top_history", 17, "Оскорбление века"),
    BET("/bet", 18, "БЫСТРЫЕ ВЫПЛАТЫ, НАДЕЖНЫЙ БУКМЕКЕР"),
    WHATS_MOOD_TODAY("/today", 19, "Какой ты сегодня?"),
    BAN("/ban", 20, "Бан, как он есть"),
    SCENARIO("/play", 21, "Коллективная пошаговая игруля для чата"),
    HAMPIK("/hampik", 22, "Какой ты Андрей?"),
    ADVANCED_SETTINGS("/advanced_settings", 23, "Опции для хакеров"),
    STATS_STRIKES("/stats_strikes", 24, "Рейтинг пидоров, выпавших подряд"),
    SHOP("/shop", 25, "Магазин"),
    MARRY("/marry", 26, "браки между участниками"),
    MARRY_LIST("/marry_list", 27, "Топ браков чата"),
    VESTNIK("/vestnik", 28, "Вестник кала"),
    TIME("/time", 29, "Мировое время");

    companion object {
        val LOOKUP = values().associateBy(Command::command)
    }
}
