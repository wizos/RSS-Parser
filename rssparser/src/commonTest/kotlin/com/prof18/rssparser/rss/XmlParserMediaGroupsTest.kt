package com.prof18.rssparser.rss

import com.prof18.rssparser.XmlParserTestExecutor
import com.prof18.rssparser.parseFeed
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class XmlParserMediaGroupsTest : XmlParserTestExecutor() {

    @Test
    fun mediaGroupsPreserveDescriptionsThumbnailsAndAllContents() = runTest {
        val item = parseFeed("feed-media-groups.xml").items.single()

        assertEquals(2, item.mediaGroups.size)

        with(item.mediaGroups[0]) {
            assertEquals("Gallery", title)
            assertEquals("Two images", description)
            assertEquals("https://example.com/gallery-thumb.jpg", thumbnail)
            assertEquals(
                listOf(
                    "https://example.com/image-1.jpg",
                    "https://example.com/image-2.webp",
                ),
                contents.map { it.url },
            )
        }

        with(item.mediaGroups[1]) {
            assertEquals("Video", title)
            assertEquals("One video", description)
            assertEquals("https://example.com/video-thumb.jpg", thumbnail)
            assertEquals(listOf("https://example.com/video.mp4"), contents.map { it.url })
        }

        assertEquals(listOf("https://example.com/audio.mp3"), item.mediaContents.map { it.url })

        assertEquals("https://example.com/audio.mp3", item.rawMediaContent?.url)
    }
}
