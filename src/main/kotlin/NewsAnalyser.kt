import com.google.cloud.language.v1.Document
import com.google.cloud.language.v1.LanguageServiceClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

fun main() {

    val rssText = getRss()

    val newsList = getNewsListFromRss(rssText)

    tweetPositiveNews(newsList)
}

fun getRss(): String {
    val client = HttpClient( Apache )

    val response = runBlocking {
        client.get<String>(
            scheme = "https",
            host = "news.google.com",
            path = "/rss/topics/CAAqIQgKIhtDQkFTRGdvSUwyMHZNRE5mTTJRU0FtcGhLQUFQAQ?hl=ja&gl=JP&ceid=JP:ja")
    }
    client.close()
    return response
}

fun getNewsListFromRss(rssText: String): List<News> {
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val xmlInput = InputSource(StringReader(rssText))
    val doc = dBuilder.parse(xmlInput)
    val root = doc.documentElement
    val itemList = root.getElementsByTagName("item")
    val newsList = mutableListOf<News>()
    for (i in 0..itemList.length-1) {
        val element = itemList.item(i) as Element
        val itemTitle = element.getElementsByTagName("title")
        val title = itemTitle.item(0).firstChild.nodeValue
        println(title)
        val itemLink = element.getElementsByTagName("link")
        val link = itemLink.item(0).firstChild.nodeValue
        newsList.add(News(title, link))
    }
    return newsList
}

fun analyseSentiment(text: String): Float? {
    val language = LanguageServiceClient.create()
    val doc = Document.newBuilder()
        .setContent(text)
        .setType(Document.Type.PLAIN_TEXT)
        .setLanguage("ja")
        .build()
    val response = language.analyzeSentiment(doc)
    val sentiment = response.documentSentiment?: return null

    println("score: ${sentiment.score}")
    language.close()
    return sentiment.score
}

fun tweetPositiveNews(list: List<News>) {
    list.forEach {
        val score = analyseSentiment(it.title)?: return@forEach
        if (score >= 0.1) {
            println("tweet!! ${it.title}")
        }
    }
}
