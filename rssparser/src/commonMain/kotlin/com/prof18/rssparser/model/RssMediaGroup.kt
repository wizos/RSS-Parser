package com.prof18.rssparser.model

/**
 * A Media RSS group attached to an RSS or Atom item.
 *
 * Ungrouped `media:content` elements are available from `RssItem.mediaContents`.
 */
public data class RssMediaGroup(
    val title: String?,
    val description: String?,
    val thumbnail: String?,
    val contents: List<RawMediaContent>,
) {
    internal class Builder {
        private var title: String? = null
        private var description: String? = null
        private var thumbnail: String? = null
        private val contents: MutableList<RawMediaContent> = mutableListOf()

        fun title(title: String?) = apply { this.title = title }
        fun description(description: String?) = apply { this.description = description }
        fun thumbnail(thumbnail: String?) = apply { this.thumbnail = thumbnail }

        fun addContent(content: RawMediaContent?) = apply {
            if (content != null) {
                contents.add(content)
            }
        }

        fun build(): RssMediaGroup? {
            if (
                title.isNullOrBlank() &&
                description.isNullOrBlank() &&
                thumbnail.isNullOrBlank() &&
                contents.isEmpty()
            ) {
                return null
            }

            return RssMediaGroup(
                title = title,
                description = description,
                thumbnail = thumbnail,
                contents = contents.toList(),
            )
        }
    }
}
