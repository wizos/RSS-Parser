package com.prof18.rssparser

import com.prof18.rssparser.exception.RssParsingException
import com.prof18.rssparser.internal.ParserInput
import com.prof18.rssparser.internal.XmlParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

public class RssParser internal constructor(
    private val xmlParser: XmlParser,
    private val recovery: XmlFeedRecovery = XmlFeedRecovery.Default,
) {

    private val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.Default

    internal interface Builder {
        /**
         * Creates a [RssParser] object
         */
        fun build(): RssParser
    }

    /**
     * Parses an RSS feed provided by [rawRssFeed] and returns an [RssChannel].
     *
     * If parsing fails, the configured [XmlFeedRecovery] may return repaired XML for one retry.
     */
    public suspend fun parse(rawRssFeed: String, baseUrl: String? = null): RssChannel =
        parseWithRecovery(
            parserInput = xmlParser.generateParserInput(rawRssFeed, baseUrl),
            rawRssFeed = rawRssFeed,
            baseUrl = baseUrl,
        )

    /**
     * Parses raw feed [bytes], preserving XML encoding detection on platforms that support it.
     */
    public suspend fun parse(bytes: ByteArray, baseUrl: String? = null): RssChannel =
        parseWithRecovery(
            parserInput = xmlParser.generateParserInput(bytes, baseUrl),
            rawRssFeed = bytes.decodeToString(),
            baseUrl = baseUrl,
        )

    private suspend fun parseWithRecovery(
        parserInput: ParserInput,
        rawRssFeed: String,
        baseUrl: String?,
    ): RssChannel = withContext(coroutineContext) {
        return@withContext try {
            xmlParser.parseXML(parserInput)
        } catch (failure: RssParsingException) {
            val repairedXml = recovery.repair(rawRssFeed, failure) ?: throw failure
            val input = xmlParser.generateParserInput(repairedXml, baseUrl)
            xmlParser.parseXML(input)
        }
    }
}

/**
 * Returns a default [RssParser] instance.
 *
 * Check the platform specific RssParserBuilder for details on the default behaviour.
 */
public expect fun RssParser(): RssParser
