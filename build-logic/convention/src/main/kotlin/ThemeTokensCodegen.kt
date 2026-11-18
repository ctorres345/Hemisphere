import groovy.json.JsonSlurper
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Turns the JSON files in `tokens/` (W3C Design Tokens, docs/design-system.md §11) into the Kotlin
 * source of `ThemeTokens`. The app's theme reads only this generated file, so it cannot drift from
 * the tokens.
 *
 * A token that is missing or malformed fails the build with its file and path.
 */
internal object ThemeTokensCodegen {
    val THEMES = listOf("light", "dark", "amoled")

    /** Token path under `color.` to its `HemisphereColors` property. */
    private val COLOR_ROLES =
        listOf(
            "surface.canvas" to "surfaceCanvas",
            "surface.raised" to "surfaceRaised",
            "surface.overlay" to "surfaceOverlay",
            "border.default" to "borderDefault",
            "border.strong" to "borderStrong",
            "text.primary" to "textPrimary",
            "text.secondary" to "textSecondary",
            "text.tertiary" to "textTertiary",
            "semantic.danger" to "danger",
            "semantic.warning" to "warning",
            "semantic.success" to "success",
        )
    private val ACCENT_ROLES = listOf("fill", "onFill", "tint", "focus", "text")
    private val DIMENSION_GROUPS = listOf("space", "radius", "row", "size")
    private val TYPE_STYLES = listOf("title", "heading", "body", "meta", "caption")
    private val HEX = Regex("^#[0-9A-Fa-f]{6}$")
    private val IDENTIFIER = Regex("^[a-z][A-Za-z0-9]*$")
    private val ALIAS =Regex("""^\{([A-Za-z0-9.]+)\}$""")

    class TokenException(
        message: String,
    ) : IllegalStateException(message)

    fun generate(
        tokensDir: File,
        packageName: String,
    ): String {
        val base = Tokens(File(tokensDir, "base.json"))
        val themes = THEMES.associateWith { Tokens(File(tokensDir, "color.$it.json")) }
        val accents = themes.getValue("light").keysOf("color.accent")
        themes.forEach { (theme, tokens) ->
            val names = tokens.keysOf("color.accent")
            if (names != accents) tokens.fail("color.accent", "accents $names differ from light's $accents ($theme)")
        }

        return buildString {
            appendLine("// Generated from the tokens/ JSON files by the hemisphere.design.tokens plugin. Do not edit.")
            appendLine("package $packageName")
            appendLine()
            appendLine("import androidx.compose.ui.graphics.Color")
            appendLine("import androidx.compose.ui.text.font.FontWeight")
            appendLine("import androidx.compose.ui.unit.dp")
            appendLine("import androidx.compose.ui.unit.sp")
            appendLine()
            appendLine("/** The curated accents (docs/design-system.md §5), in token order. */")
            appendLine("enum class Accent {")
            accents.forEach { appendLine("    ${it.pascal()},") }
            appendLine("}")
            appendLine()
            appendLine("internal object ThemeTokens {")
            themes.forEach { (theme, tokens) -> appendPalette(theme, tokens, accents) }
            DIMENSION_GROUPS.forEach { appendDimensions(base, it) }
            TYPE_STYLES.forEach { appendTextStyle(base, it) }
            appendLine("}")
        }
    }

    private fun StringBuilder.appendPalette(
        theme: String,
        tokens: Tokens,
        accents: List<String>,
    ) {
        appendLine("    private val ${theme}Accents =")
        appendLine("        mapOf(")
        accents.forEach { accent ->
            appendLine("            Accent.${accent.pascal()} to")
            appendLine("                AccentColors(")
            ACCENT_ROLES.forEach { role ->
                appendLine("                    $role = ${tokens.color("color.accent.$accent.$role").kotlin()},")
            }
            appendLine("                ),")
        }
        appendLine("        )")
        appendLine()
        appendLine("    val $theme =")
        appendLine("        ThemePalette(")
        appendLine("            colors =")
        appendLine("                HemisphereColors(")
        COLOR_ROLES.forEach { (path, property) ->
            appendLine("                    $property = ${tokens.color("color.$path").kotlin()},")
        }
        appendLine("                    accent = ${theme}Accents.getValue(Accent.${accents.first().pascal()}),")
        appendLine("                ),")
        appendLine("            accents = ${theme}Accents,")
        appendLine("        )")
        appendLine()
    }

    private fun StringBuilder.appendDimensions(
        base: Tokens,
        group: String,
    ) {
        base.keysOf(group).forEach { name ->
            appendLine("    val $group${name.pascal()} = ${base.px(base.value("$group.$name"), "$group.$name").plain()}.dp")
        }
        appendLine()
    }

    private fun StringBuilder.appendTextStyle(
        base: Tokens,
        name: String,
    ) {
        val path = "typography.$name"
        val value = base.value(path) as? Map<*, *> ?: base.fail(path, "expected a typography value")
        val size = base.px(value["fontSize"], "$path.fontSize")
        val ratio = base.number(value["lineHeight"], "$path.lineHeight")
        val weight = base.number(base.resolve(value["fontWeight"], "$path.fontWeight"), "$path.fontWeight")
        val lineHeight = (size * ratio).setScale(2, RoundingMode.HALF_UP)
        appendLine("    val type${name.pascal()} =")
        appendLine("        TokenTextStyle(")
        appendLine("            fontSize = ${size.plain()}.sp,")
        appendLine("            lineHeight = ${lineHeight.plain()}.sp,")
        appendLine("            fontWeight = FontWeight(${weight.toInt()}),")
        appendLine("        )")
    }

    private fun String.pascal() = replaceFirstChar { it.titlecase(Locale.ROOT) }

    private fun BigDecimal.plain(): String = stripTrailingZeros().toPlainString().let { if ('.' in it) it + "f" else it }

    private fun String.kotlin() = "Color(0xFF${substring(1).uppercase(Locale.ROOT)})"

    /** One token file, read once. Paths are dotted, as in DTCG aliases: `color.surface.canvas`. */
    private class Tokens(
        val file: File,
    ) {
        private val root: Map<*, *> = parse()

        private fun parse(): Map<*, *> {
            val parsed =
                try {
                    JsonSlurper().parse(file)
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    throw TokenException("${file.path}: missing or not valid JSON (${e.message})")
                }
            return parsed as? Map<*, *> ?: throw TokenException("${file.path}: not a JSON object")
        }

        fun fail(
            path: String,
            why: String,
        ): Nothing = throw TokenException("${file.path}: $path: $why")

        fun node(path: String): Map<*, *> =
            path.split('.').fold(root) { node, key ->
                node[key] as? Map<*, *> ?: fail(path, "missing")
            }

        fun value(path: String): Any? = node(path)["\$value"]

        /** Child token names, which become Kotlin identifiers, so anything but `camelCase` fails. */
        fun keysOf(path: String): List<String> =
            node(path).keys.map { it.toString() }.filterNot { it.startsWith("$") }.onEach { key ->
                if (!IDENTIFIER.matches(key)) fail("$path.$key", "token names must be camelCase letters and digits")
            }

        fun resolve(
            raw: Any?,
            path: String,
            seen: Set<String> = emptySet(),
        ): Any? {
            val alias = (raw as? String)?.let { ALIAS.matchEntire(it) }?.groupValues?.get(1) ?: return raw
            if (alias in seen) fail(path, "alias loop through {$alias}")
            return resolve(value(alias), alias, seen + alias)
        }

        fun color(path: String): String {
            val value = resolve(value(path), path)
            return (value as? String)?.takeIf { HEX.matches(it) } ?: fail(path, "expected #RRGGBB, got $value")
        }

        fun px(
            raw: Any?,
            path: String,
        ): BigDecimal {
            val map = raw as? Map<*, *> ?: fail(path, "expected {value, unit}")
            if (map["unit"] != "px") fail(path, "unit must be px")
            return number(map["value"], path)
        }

        fun number(
            raw: Any?,
            path: String,
        ): BigDecimal = (raw as? Number)?.let { BigDecimal(it.toString()) } ?: fail(path, "expected a number, got $raw")
    }
}
