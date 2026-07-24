package com.prof18.rssparser

import com.prof18.rssparser.internal.JvmXmlParser
import kotlinx.coroutines.Dispatchers
import java.nio.charset.Charset


/**
 * A Builder that creates a new instance of [RssParser]
 *
 * @property charset The [Charset] of the RSS feed. The field is optional; if nothing is provided,
 *  it will be inferred from the feed
 */
public class RssParserBuilder(
    private val charset: Charset? = null,
): RssParser.Builder {
    private var recovery: XmlFeedRecovery = XmlFeedRecovery.Default

    public fun recovery(recovery: XmlFeedRecovery): RssParserBuilder = apply {
        this.recovery = recovery
    }

    override fun build(): RssParser {
        return RssParser(
            xmlParser = JvmXmlParser(
                charset = charset,
                dispatcher = Dispatchers.IO,
            ),
            recovery = recovery,
        )
    }
}

public actual fun RssParser(): RssParser = RssParserBuilder().build()
