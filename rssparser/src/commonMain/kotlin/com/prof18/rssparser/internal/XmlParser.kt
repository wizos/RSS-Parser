package com.prof18.rssparser.internal

import com.prof18.rssparser.model.RssChannel

internal interface XmlParser {
    suspend fun parseXML(input: ParserInput): RssChannel
    fun generateParserInput(rawRssFeed: String, baseUrl: String?): ParserInput
    fun generateParserInput(bytes: ByteArray, baseUrl: String?): ParserInput
}
