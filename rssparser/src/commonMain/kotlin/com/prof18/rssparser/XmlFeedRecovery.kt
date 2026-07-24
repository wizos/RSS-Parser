package com.prof18.rssparser

import com.prof18.rssparser.exception.RssParsingException

/**
 * Repairs a feed after the first parsing attempt fails.
 *
 * Return `null` to propagate [failure] without retrying, or return XML to retry once.
 */
public fun interface XmlFeedRecovery {
    public fun repair(xml: String, failure: RssParsingException): String?

    public companion object {
        /**
         * Keeps the recovery behavior used by RSS Parser before recovery became configurable.
         */
        public val Default: XmlFeedRecovery = XmlFeedRecovery { xml, _ ->
            repairCommonXmlIssues(xml)
        }

        /**
         * Disables feed repair and propagates the first parsing failure.
         */
        public val None: XmlFeedRecovery = XmlFeedRecovery { _, _ -> null }
    }
}

private fun repairCommonXmlIssues(xml: String): String {
    val feedRootEnd = Regex("</(?:rss|feed|rdf:RDF)\\s*>", RegexOption.IGNORE_CASE)
        .findAll(xml)
        .lastOrNull()
        ?.range
        ?.last
    var repaired = if (feedRootEnd == null) xml else xml.substring(0, feedRootEnd + 1)

    val root = Regex("<(?:rss|feed|rdf:RDF)\\b[^>]*>", RegexOption.IGNORE_CASE).find(repaired)
    if (root != null) {
        val declaredPrefixes = Regex("""\bxmlns:([A-Za-z_][\w.-]*)\s*=""")
            .findAll(root.value)
            .map { it.groupValues[1] }
            .toSet()
        val missingPrefixes = Regex("""</?([A-Za-z_][\w.-]*):[A-Za-z_][\w.-]*\b""")
            .findAll(repaired)
            .map { it.groupValues[1] }
            .filter { it != "xml" && it !in declaredPrefixes }
            .toSet()
        if (missingPrefixes.isNotEmpty()) {
            val declarations = missingPrefixes.joinToString("") {
                """ xmlns:$it="urn:rss-parser:undeclared:$it""""
            }
            repaired = repaired.replaceRange(root.range.last, root.range.last, declarations)
        }
    }

    return repaired
        // Fix standalone ampersands in URLs and text
        .replace(
            Regex("&(?!(amp;|lt;|gt;|quot;|apos;|#[0-9]+;|#x[0-9a-fA-F]+;))"),
            "&amp;",
        )
        // Fix duplicate closing tags with content between them
        // Example: <category></category><![CDATA[News]]></category> -> <category><![CDATA[News]]></category>
        .replace(
            Regex("<([^>]+)></\\1>([^<]+|<!\\[CDATA\\[.+?\\]\\]>)</\\1>"),
            "<$1>$2</$1>",
        )
        // Fix self-closing tags, but only if they don't already have content
        .replace(
            Regex("<(link|source|category|guid|enclosure|media:content|media:thumbnail)([^>]*?)>\\s*</\\1>"),
            "<$1$2></$1>",
        )
        // Fix common HTML void tags
        .replace(
            Regex("<(meta|img|br|hr|input|area|base|col|embed|keygen|param|track|wbr)([^>]*?)/?>(?!</\\1>)"),
            "<$1$2></$1>",
        )
        // Preserve the existing additional passes for CDATA and attribute values
        .replace(
            Regex("(<!\\[CDATA\\[.*?)&(?!(amp;|lt;|gt;|quot;|apos;|#[0-9]+;|#x[0-9a-fA-F]+;))(.*?\\]\\]>)"),
            "$1&amp;$3",
        )
        .replace(
            Regex("=\"(.*?)&(?!(amp;|lt;|gt;|quot;|apos;|#[0-9]+;|#x[0-9a-fA-F]+;))(.*?)\""),
            "=\"$1&amp;$3\"",
        )
}
