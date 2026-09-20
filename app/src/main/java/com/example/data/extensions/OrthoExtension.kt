package com.example.data.extensions

import java.text.Normalizer

/** One installed extension: a self-contained HTML program (see guide.md). */
data class OrthoExtension(
    /** Stable identity. Importing a file with the same id replaces it and keeps its data. */
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    /** A single emoji shown in the list. */
    val icon: String,
    val author: String,
    /** Only true when the file asks for it with `ortho-ext-network="allow"`. */
    val allowNetwork: Boolean,
    val sizeBytes: Long,
    val updatedAt: Long
)

/**
 * Reads the `<meta name="ortho-ext-…" content="…">` block from an extension's
 * `<head>`. Every field is optional: a plain HTML program with only a `<title>`
 * still imports, it just gets a default icon and an id made from its title.
 */
object ExtensionMetadata {

    data class Parsed(
        val id: String?,
        val name: String?,
        val description: String?,
        val version: String?,
        val icon: String?,
        val author: String?,
        val allowNetwork: Boolean
    )

    const val MAX_ID_LENGTH = 48
    private const val PREFIX = "ortho-ext-"

    private val META = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val ATTR = Regex("""([A-Za-z_:][-A-Za-z0-9_:.]*)\s*=\s*(?:"([^"]*)"|'([^']*)')""")
    private val TITLE = Regex(
        "<title[^>]*>(.*?)</title>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val HEAD_END = Regex("</head\\s*>", RegexOption.IGNORE_CASE)
    private val ENTITY = Regex("&(#[xX][0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);")
    private val WHITESPACE = Regex("\\s+")
    private val VALID_ID = Regex("[a-z0-9](?:[a-z0-9-]{0,${MAX_ID_LENGTH - 2}}[a-z0-9])?")

    fun parse(html: String): Parsed {
        val head = HEAD_END.find(html)?.let { html.substring(0, it.range.first) } ?: html
        val values = HashMap<String, String>()
        for (tag in META.findAll(head)) {
            val attrs = HashMap<String, String>()
            for (m in ATTR.findAll(tag.value)) {
                attrs[m.groupValues[1].lowercase()] = m.groups[2]?.value ?: m.groups[3]?.value ?: ""
            }
            val name = attrs["name"]?.trim()?.lowercase() ?: continue
            if (!name.startsWith(PREFIX)) continue
            values[name.removePrefix(PREFIX)] = decodeEntities(attrs["content"].orEmpty()).trim()
        }
        val title = TITLE.find(head)?.groupValues?.get(1)
            ?.let { decodeEntities(it).replace(WHITESPACE, " ").trim() }

        return Parsed(
            id = values["id"]?.let(::slug)?.takeIf { it.isNotEmpty() },
            name = values["name"]?.takeIf { it.isNotEmpty() } ?: title?.takeIf { it.isNotEmpty() },
            description = values["description"]?.takeIf { it.isNotEmpty() },
            version = values["version"]?.takeIf { it.isNotEmpty() },
            icon = values["icon"]?.takeIf { it.isNotEmpty() },
            author = values["author"]?.takeIf { it.isNotEmpty() },
            allowNetwork = values["network"].equals("allow", ignoreCase = true)
        )
    }

    /** "Programme DEMS — v2" → "programme-dems-v2". */
    fun slug(text: String): String {
        val ascii = Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        return ascii.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(MAX_ID_LENGTH)
            .trim('-')
    }

    fun isValidId(id: String): Boolean = VALID_ID.matches(id)

    fun decodeEntities(text: String): String = ENTITY.replace(text) { m ->
        val entity = m.groupValues[1]
        when {
            entity.startsWith("#x") || entity.startsWith("#X") ->
                entity.substring(2).toIntOrNull(16)?.let(::codePoint)
            entity.startsWith("#") -> entity.substring(1).toIntOrNull()?.let(::codePoint)
            else -> NAMED[entity]
        } ?: m.value
    }

    private fun codePoint(cp: Int): String? =
        if (Character.isValidCodePoint(cp)) String(Character.toChars(cp)) else null

    private val NAMED = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
        "nbsp" to "\u00A0", "rsquo" to "\u2019", "lsquo" to "\u2018",
        "ldquo" to "\u201C", "rdquo" to "\u201D", "laquo" to "\u00AB", "raquo" to "\u00BB",
        "ndash" to "\u2013", "mdash" to "\u2014", "middot" to "\u00B7", "hellip" to "\u2026",
        "eacute" to "é", "egrave" to "è", "ecirc" to "ê", "agrave" to "à",
        "acirc" to "â", "ccedil" to "ç", "ocirc" to "ô", "ucirc" to "û", "icirc" to "î"
    )
}
