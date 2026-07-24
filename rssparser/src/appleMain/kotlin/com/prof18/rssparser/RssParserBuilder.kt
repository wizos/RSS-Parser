package com.prof18.rssparser

import com.prof18.rssparser.internal.IosXmlParser
import kotlinx.coroutines.Dispatchers

/**
 * A builder that creates new instances of [RssParser]
 */
public class RssParserBuilder : RssParser.Builder {
    private var recovery: XmlFeedRecovery = XmlFeedRecovery.Default

    public fun recovery(recovery: XmlFeedRecovery): RssParserBuilder = apply {
        this.recovery = recovery
    }

    override fun build(): RssParser {
        return RssParser(
            xmlParser = IosXmlParser(
                Dispatchers.Default
            ),
            recovery = recovery,
        )
    }
}

public actual fun RssParser(): RssParser = RssParserBuilder().build()
