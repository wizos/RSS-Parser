package com.prof18.rssparser

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class EscapeMalformedFeedParserTest : XmlParserTestExecutor() {

    @Test
    fun whenReceivingAMalformedXmlTheParserWillHandleIt() = runTest {
        val rssParser = RssParser(
            xmlParser = createXmlParser()
        )

        val channel = rssParser.parse(readFileFromResourcesAsString("feed-escape-malformed.xml"))
        assertTrue(channel.items.isNotEmpty())
    }
}
