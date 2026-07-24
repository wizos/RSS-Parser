package com.prof18.rssparser.exception

/**
 * An exception thrown whe the parsing of the RSS feed fails
 *
 * @property message the detail message string.
 * @property cause the cause of this throwable.
 */
public class RssParsingException(
    override val message: String?,
    override val cause: Throwable?,
    lineNumber: Int?,
    columnNumber: Int?,
) : Exception(message, cause) {
    public constructor(message: String?, cause: Throwable?) : this(message, cause, null, null)

    /**
     * One-based line number reported by the platform XML parser, when available.
     */
    public val lineNumber: Int? = lineNumber?.takeIf { it > 0 }

    /**
     * One-based column number reported by the platform XML parser, when available.
     */
    public val columnNumber: Int? = columnNumber?.takeIf { it > 0 }
}
