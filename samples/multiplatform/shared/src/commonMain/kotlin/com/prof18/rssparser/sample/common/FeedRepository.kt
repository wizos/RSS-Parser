package com.prof18.rssparser.sample.common

import com.prof18.rssparser.RssParser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class FeedRepository(
    private val parser: RssParser,
    private val httpClient: HttpClient = HttpClient(),
) {
    @Throws(Throwable::class)
    suspend fun getFeed(url: String): Feed {
        val response = httpClient.get(url)
        val channel = parser.parse(response.body<ByteArray>(), url)
        return Feed(
            title = channel.title ?: "",
            items = channel.items.mapNotNull {
                val title = it.title
                val subtitle = it.description
                val pubDate = it.pubDate

                if (title == null || pubDate == null) {
                    return@mapNotNull null
                }

                FeedItem(
                    title = title,
                    subtitle = subtitle,
                    content = it.content,
                    imageUrl = it.image,
                    dateString = pubDate,
                )
            }
        )
    }
}
