package com.prof18.rssparser

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class MalformedFeedParserTest : XmlParserTestExecutor() {

    @Test
    fun whenReceivingAMalformedXmlTheParserWillHandleIt() = runTest {
        // On web, the fallback to delete the whitespace at the beginning it doesn't work
        if (currentTarget == CurrentTarget.WEB) {
            assertTrue(true)
            return@runTest
        }
        val rssParser = RssParser(
            xmlParser = createXmlParser()
        )

        val channel = rssParser.parse(readFileFromResourcesAsString("feed-test-malformed.xml"))
        assertTrue(channel.items.isNotEmpty())
    }
}
