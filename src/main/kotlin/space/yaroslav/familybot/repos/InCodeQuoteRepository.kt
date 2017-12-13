package space.yaroslav.familybot.repos

import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom

@Component
class InCodeQuoteRepository : QuoteRepository {

    private val map: List<Pair<List<String>, String>> = listOf(
    Pair(listOf("Городецкий", "БАУ"), "Люцифер в аду возводит безумную ЗЛА! "),
    Pair(listOf("Городецкий", "БАУ"), "Я слышу голос овощей!!! "),
    Pair(listOf("Городецкий", "БАУ"), "УУУУУУБЕЕЕЕЕЙЙ! Кричали голоса! "),
    Pair(listOf("Пиздюк", "БАУ"), "Я ненавижу Питер! "),
    Pair(listOf("Городецкий", "БАУ"), "Купи котенка СВОЕМУ РЕБЕНКУ! "),
    Pair(listOf("Городецкий", "БАУ"), "Денчик ушел в метал! "),
    Pair(listOf("Городецкий", "БАУ"), "Безумец! Слепой! Остановиииись! "),
    Pair(listOf("Городецкий", "БАУ"), "Черный толчок смерти уубииваает... "),
    Pair(listOf("Тима", "БАУ"), "Ты не шахтер, но лазишь в шахты "),
    Pair(listOf("Тима", "БАУ"), "Не трубочист, но чистишь дымоход "),
    Pair(listOf("Тима", "БАУ"), "Лупишься в туза, но не играешь в карты "),
    Pair(listOf("Тима", "БАУ"), "Не вор, но знаешь всё про черный ход "),
    Pair(listOf("Тима", "БАУ"), "Ты не гончар, но месишь глину "),
    Pair(listOf("Тима", "БАУ"), "Ты не лесник, но шебуршишь в дупле "),
    Pair(listOf("Тима", "БАУ"), "Волосатый мотороллер едет без резины "),
    Pair(listOf("Тима", "БАУ"), "Твоя кожаная пуля в кожаном стволе ")
    )

    override fun getByTag(tag: String): String? {
        val filter = map.filter { it.first.contains(tag) }
        if(filter.isEmpty()){
            return null
        }
        return getRandom(filter.map { it.second })
    }

    override fun getRandom(): String {
        return getRandom(map.map { it.second })
    }

    private fun getRandom(set: List<String>): String {
        val nextInt = ThreadLocalRandom.current().nextInt(0, set.size)
        return set[nextInt]
    }
}


