package com.prof18.rssparser

import com.prof18.rssparser.exception.RssParsingException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class XmlFeedRecoveryTest : XmlParserTestExecutor() {

    @Test
    fun customRecoveryCanRepairAndRetryOnce() = runTest {
        if (currentTarget == CurrentTarget.WEB) {
            return@runTest
        }

        var recoveryCalls = 0
        val parser = RssParser(
            xmlParser = createXmlParser(),
            recovery = XmlFeedRecovery { xml, failure ->
                assertNotNull(failure.cause)
                recoveryCalls += 1
                xml.replace("<rss broken=\"unterminated>", "<rss>")
            },
        )

        val channel = parser.parse(malformedFeed.encodeToByteArray())

        assertEquals(1, recoveryCalls)
        assertEquals("Recovered title", channel.title)
    }

    @Test
    fun nullRecoveryResultPropagatesFirstFailureWithLocation() = runTest {
        if (currentTarget == CurrentTarget.WEB) {
            return@runTest
        }

        val parser = RssParser(
            xmlParser = createXmlParser(),
            recovery = XmlFeedRecovery.None,
        )

        val failure = assertFailsWith<RssParsingException> {
            parser.parse(malformedFeed)
        }

        if (currentTarget == CurrentTarget.ANDROID || currentTarget == CurrentTarget.JVM) {
            assertNotNull(failure.lineNumber)
            assertNotNull(failure.columnNumber)
        }
    }

    @Test
    fun defaultRecoveryDropsMalformedContentAfterFeedRoot() = runTest {
        val parser = RssParser(
            xmlParser = createXmlParser(),
            recovery = XmlFeedRecovery.Default,
        )

        val channel = parser.parse("$validFeed\n<!--Cached 1784850516--->")

        assertEquals("Recovered title", channel.title)
    }

    @Test
    fun defaultRecoveryDeclaresMissingNamespacePrefixes() = runTest {
        val malformed = validFeed.replace(
            "</channel>",
            "<category:no_buffer/></channel>",
        )
        val repaired = assertNotNull(
            XmlFeedRecovery.Default.repair(
                malformed,
                RssParsingException("test", null),
            ),
        )

        assertTrue("""xmlns:category="urn:rss-parser:undeclared:category"""" in repaired)
        val channel = RssParser(
            xmlParser = createXmlParser(),
            recovery = XmlFeedRecovery.None,
        ).parse(repaired)
        assertEquals("Recovered title", channel.title)
    }

    private companion object {
        const val validFeed = """
            <rss>
                <channel>
                    <title>Recovered title</title>
                    <link>https://example.com</link>
                    <description>Recovered description</description>
                </channel>
            </rss>
        """

        const val malformedFeed = """
            <rss broken="unterminated>
                <channel>
                    <title>Recovered title</title>
                    <link>https://example.com</link>
                    <description>Mismatched closing tag</description>
                </channel>
            </rss>
        """
    }
}
