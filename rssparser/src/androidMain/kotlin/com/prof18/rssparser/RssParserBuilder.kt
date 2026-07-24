package com.prof18.rssparser

import com.prof18.rssparser.internal.AndroidXmlParser
import kotlinx.coroutines.Dispatchers
import java.nio.charset.Charset


/**
 * A Builder that creates a new instance of [RssParser]
 *
 * @property charset The [Charset] of the RSS feed. This field is optional. If nothing is provided,
 *  it will be inferred from the feed.
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
            xmlParser = AndroidXmlParser(
                charset = charset,
                dispatcher = Dispatchers.IO,
            ),
            recovery = recovery,
        )
    }
}

public actual fun RssParser(): RssParser = RssParserBuilder().build()
