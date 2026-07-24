package com.prof18.rssparser.rss

import com.prof18.rssparser.CurrentTarget
import com.prof18.rssparser.XmlParserTestExecutor
import com.prof18.rssparser.currentTarget
import com.prof18.rssparser.parseFeed
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class XmlParserMissingNamespaceTest : XmlParserTestExecutor() {

    @Test
    fun undeclaredWordPressWfwPrefixIsAcceptedOnAndroidAndJvm() = runTest {
        if (currentTarget != CurrentTarget.ANDROID && currentTarget != CurrentTarget.JVM) {
            return@runTest
        }

        val channel = parseFeed("feed-missing-wfw-namespace.xml")

        assertEquals("Missing namespace declaration", channel.title)
        assertEquals("Still parseable on Android and JVM", channel.items.single().title)
    }
}
