package com.prof18.rssparser.internal

import com.prof18.rssparser.model.ItunesChannelData
import com.prof18.rssparser.model.ItunesItemData
import com.prof18.rssparser.model.ItunesOwner
import com.prof18.rssparser.model.RawEnclosure
import com.prof18.rssparser.model.RawMediaContent
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssImage
import com.prof18.rssparser.model.RssItem
import com.prof18.rssparser.model.RssMediaGroup
import com.prof18.rssparser.model.YoutubeChannelData
import com.prof18.rssparser.model.YoutubeItemData

internal class ChannelFactory {
    val channelBuilder = RssChannel.Builder()
    val channelImageBuilder = RssImage.Builder()
    var articleBuilder = RssItem.Builder()
    val itunesChannelBuilder = ItunesChannelData.Builder()
    var itunesArticleBuilder = ItunesItemData.Builder()
    var itunesOwnerBuilder = ItunesOwner.Builder()
    var youtubeChannelDataBuilder = YoutubeChannelData.Builder()
    var youtubeItemDataBuilder = YoutubeItemData.Builder()
    var rawEnclosureBuilder = RawEnclosure.Builder()
    private var rawMediaContent: RawMediaContent? = null
    private val mediaContents: MutableList<RawMediaContent> = mutableListOf()
    private val mediaGroupBuilders: MutableList<RssMediaGroup.Builder> = mutableListOf()
    private var activeMediaGroupBuilder: RssMediaGroup.Builder? = null

    // This image url is extracted from the content and the description of the rss item.
    // It's a fallback just in case there aren't any images in the enclosure tag.
    private var imageUrlFromContent: String? = null

    fun buildArticle() {
        val itunesItemData = itunesArticleBuilder.build()
        // Use iTunes image as fallback if no other image is set
        articleBuilder.image(imageUrlFromContent)
        articleBuilder.image(itunesItemData?.image)
        articleBuilder.itunesArticleData(itunesItemData)
        articleBuilder.youtubeItemData(youtubeItemDataBuilder.build())
        articleBuilder.rawEnclosure(rawEnclosureBuilder.build())
        articleBuilder.rawMediaContent(rawMediaContent)
        articleBuilder.mediaContents(mediaContents.toList())
        articleBuilder.mediaGroups(mediaGroupBuilders.mapNotNull { it.build() })
        articleBuilder.build()?.let { channelBuilder.addItem(it) }
        // Reset temp data
        imageUrlFromContent = null
        articleBuilder = RssItem.Builder()
        itunesArticleBuilder = ItunesItemData.Builder()
        youtubeItemDataBuilder = YoutubeItemData.Builder()
        rawEnclosureBuilder = RawEnclosure.Builder()
        rawMediaContent = null
        mediaContents.clear()
        mediaGroupBuilders.clear()
        activeMediaGroupBuilder = null
    }

    fun startMediaGroup() {
        activeMediaGroupBuilder = RssMediaGroup.Builder().also(mediaGroupBuilders::add)
    }

    fun endMediaGroup() {
        activeMediaGroupBuilder = null
    }

    fun addMediaContent(
        url: String?,
        type: String?,
        medium: String?,
        includeInLegacyFields: Boolean = true,
    ) {
        val content = RawMediaContent.Builder()
            .url(url)
            .type(type)
            .medium(medium)
            .build()
            ?: return
        activeMediaGroupBuilder?.addContent(content) ?: mediaContents.add(content)

        if (includeInLegacyFields) {
            rawMediaContent = content
            when {
                !medium.isNullOrBlank() -> when {
                    medium.equals("image", ignoreCase = true) -> articleBuilder.image(url)
                    medium.equals("audio", ignoreCase = true) -> articleBuilder.audioIfNull(url)
                    medium.equals("video", ignoreCase = true) -> articleBuilder.videoIfNull(url)
                }
                !type.isNullOrBlank() -> when {
                    type.contains("image", ignoreCase = true) -> articleBuilder.image(url)
                    type.contains("audio", ignoreCase = true) -> articleBuilder.audioIfNull(url)
                    type.contains("video", ignoreCase = true) -> articleBuilder.videoIfNull(url)
                }
            }
        }
    }

    fun setMediaGroupTitle(title: String?) {
        activeMediaGroupBuilder?.title(title)
    }

    fun setMediaGroupDescription(description: String?) {
        activeMediaGroupBuilder?.description(description)
    }

    fun setMediaGroupThumbnail(url: String?, includeInLegacyFields: Boolean = true) {
        activeMediaGroupBuilder?.thumbnail(url)
        if (includeInLegacyFields) {
            articleBuilder.image(url)
        }
    }

    fun buildItunesOwner() {
        itunesChannelBuilder.owner(itunesOwnerBuilder.build())
        itunesOwnerBuilder = ItunesOwner.Builder()
    }

    /**
     * Finds the first img tag and gets the src as the featured image.
     *
     * @param content The content in which to search for the tag
     * @return The url, if there is one
     */
    fun setImageFromContent(content: String?) {
        try {
            val decoded = content
                ?.replace("&amp;amp;", "&amp;")
                ?.replace("&amp;", "&")
                ?.replace("&quot;", "\"")
                ?.replace("&lt;", "<")
                ?.replace("&gt;", ">")

            val urlRegex = Regex(
                pattern = """https?://[^\s<>"']+\.(?:jpg|jpeg|png|gif|bmp|webp)(?:\?[^\s<>"']*)?""",
                options = setOf(RegexOption.IGNORE_CASE)
            )

            decoded
                ?.let { urlRegex.find(it) }
                ?.let {
                    it.value.trim().let { imgUrl ->
                        if (!imgUrl.contains(EMOJI_WEBSITE) && !imgUrl.contains("/smilies/")) {
                            imageUrlFromContent = imgUrl
                        }
                    }
                }
        } catch (_: Throwable) {
            // Do nothing, on iOS it could fail for too much recursion
        }
    }

    fun setChannelItunesKeywords(keywords: String?) {
        val keywordList = extractItunesKeywords(keywords)
        if (keywordList.isNotEmpty()) {
            itunesChannelBuilder.keywords(keywordList)
        }
    }

    fun setArticleItunesKeywords(keywords: String?) {
        val keywordList = extractItunesKeywords(keywords)
        if (keywordList.isNotEmpty()) {
            itunesArticleBuilder.keywords(keywordList)
        }
    }

    private fun extractItunesKeywords(keywords: String?): List<String> =
        keywords?.split(",")?.mapNotNull {
            it.trim().ifEmpty {
                null
            }
        } ?: emptyList()

    fun build(): RssChannel {
        val itunesChannelData = itunesChannelBuilder.build()
        val channelImage = channelImageBuilder.build()
        if (channelImage?.isNotEmpty() == true) {
            channelBuilder.image(channelImage)
        } else if (itunesChannelData?.image != null) {
            // Use iTunes image as fallback if no standard RSS image is set
            channelBuilder.image(
                RssImage(
                    title = null,
                    url = itunesChannelData.image,
                    link = null,
                    description = null
                )
            )
        }
        channelBuilder.itunesChannelData(itunesChannelData)
        channelBuilder.youtubeChannelData(youtubeChannelDataBuilder.build())
        return channelBuilder.build()
    }

    private companion object {
        const val EMOJI_WEBSITE = "https://s.w.org/images/core/emoji"
    }
}
