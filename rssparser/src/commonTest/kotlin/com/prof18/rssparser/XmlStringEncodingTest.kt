package com.prof18.rssparser

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class XmlStringEncodingTest : XmlParserTestExecutor() {

    @Test
    fun stringInputIgnoresByteEncodingDeclaration() = runTest {
        val parser = RssParser(xmlParser = createXmlParser())

        val channel = parser.parse(
            """
            <?xml version="1.0" encoding="GBK"?>
            <rss>
                <channel>
                    <title>吾爱破解</title>
                    <link>https://www.52pojie.cn/</link>
                    <description>原创发布区</description>
                </channel>
            </rss>
            """.trimIndent(),
        )

        assertEquals("吾爱破解", channel.title)
    }
}
