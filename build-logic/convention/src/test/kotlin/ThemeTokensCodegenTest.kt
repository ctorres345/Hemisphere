import groovy.json.JsonSlurper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ThemeTokensCodegenTest {
    @get:Rule
    val temp = TemporaryFolder()

    /** The repo's real tokens; Gradle runs build-logic tests from build-logic/convention. */
    private val repoTokens = File("../../tokens").canonicalFile

    private fun generate(dir: File = repoTokens) = ThemeTokensCodegen.generate(dir, "test.pkg")

    /** A copy of the real tokens with one file's text edited. */
    private fun tokensWith(
        file: String,
        edit: (String) -> String,
    ): File {
        val dir = temp.newFolder("tokens")
        repoTokens.copyRecursively(dir)
        val target = File(dir, file)
        target.writeText(edit(target.readText()))
        return dir
    }

    @Test
    fun `every color in every theme file reaches the generated source`() {
        val source = generate()
        ThemeTokensCodegen.THEMES.forEach { theme ->
            val hexes = Regex("#[0-9A-Fa-f]{6}").findAll(File(repoTokens, "color.$theme.json").readText()).map { it.value }
            hexes.forEach { hex ->
                assertTrue("$theme $hex", "Color(0xFF${hex.substring(1).uppercase()})" in source)
            }
        }
    }

    @Test
    fun `each theme maps its own surface colors`() {
        val source = generate()
        assertTrue(Regex("""val light =\s+ThemePalette\(\s+colors =\s+HemisphereColors\(\s+surfaceCanvas = Color\(0xFFF7F7F8\)""").containsMatchIn(source))
        assertTrue(Regex("""val dark =\s+ThemePalette\(\s+colors =\s+HemisphereColors\(\s+surfaceCanvas = Color\(0xFF111214\)""").containsMatchIn(source))
        assertTrue(Regex("""val amoled =\s+ThemePalette\(\s+colors =\s+HemisphereColors\(\s+surfaceCanvas = Color\(0xFF000000\)""").containsMatchIn(source))
    }

    @Test
    fun `sizes become dp and type becomes sp with the line height ratio applied`() {
        val source = generate()
        assertTrue("val spaceXs = 4.dp" in source)
        assertTrue("val radiusSheet = 12.dp" in source)
        assertTrue("val rowCompact = 40.dp" in source)
        assertTrue("val sizeAppBar = 56.dp" in source)
        assertTrue(Regex("""val typeTitle =\s+TokenTextStyle\(\s+fontSize = 20.sp,\s+lineHeight = 28.sp,\s+fontWeight = FontWeight\(500\),""").containsMatchIn(source))
        assertTrue(Regex("""val typeCaption =\s+TokenTextStyle\(\s+fontSize = 11.sp,\s+lineHeight = 14.sp,\s+fontWeight = FontWeight\(400\),""").containsMatchIn(source))
    }

    @Test
    fun `the accent enum follows the token order`() {
        val enumBody = generate().substringAfter("enum class Accent {").substringBefore("}")
        assertEquals(
            listOf("Blue", "Teal", "Green", "Amber", "Orange", "Red", "Pink", "Violet"),
            enumBody.lines().map { it.trim().removeSuffix(",") }.filter { it.isNotEmpty() },
        )
    }

    @Test
    fun `an alias resolves to its target color`() {
        val dir =
            tokensWith("color.dark.json") {
                it.replaceFirst(""""tint": {
          "${'$'}value": "#2B3345"""", """"tint": {
          "${'$'}value": "{color.semantic.danger}"""")
            }
        val blueDark = generate(dir).substringAfter("private val darkAccents =").substringAfter("Accent.Blue to").substringBefore("Accent.Teal to")
        assertTrue(blueDark, "tint = Color(0xFFF2827A)" in blueDark)
    }

    @Test
    fun `a malformed color fails with its file and path`() {
        val dir = tokensWith("color.amoled.json") { it.replaceFirst("#000000", "#00000") }
        val error = assertThrows(ThemeTokensCodegen.TokenException::class.java) { generate(dir) }
        assertTrue(error.message, "color.amoled.json: color.surface.canvas" in error.message!!)
    }

    @Test
    fun `a missing token fails the build`() {
        val dir = tokensWith("color.light.json") { it.replaceFirst("\"warning\"", "\"warn\"") }
        val error = assertThrows(ThemeTokensCodegen.TokenException::class.java) { generate(dir) }
        assertTrue(error.message, "color.semantic.warning: missing" in error.message!!)
    }

    @Test
    fun `an alias loop fails instead of hanging`() {
        val dir =
            tokensWith("color.light.json") {
                it.replaceFirst("\"#F7F7F8\"", "\"{color.surface.canvas}\"")
            }
        val error = assertThrows(ThemeTokensCodegen.TokenException::class.java) { generate(dir) }
        assertTrue(error.message, "alias loop" in error.message!!)
    }

    @Test
    fun `a token name that is not a Kotlin identifier fails`() {
        val dir = tokensWith("base.json") { it.replaceFirst("\"appBar\"", "\"app-bar\"") }
        val error = assertThrows(ThemeTokensCodegen.TokenException::class.java) { generate(dir) }
        assertTrue(error.message, "base.json: size.app-bar" in error.message!!)
    }

    @Test
    fun `every color role in every theme gets the value at its own token path`() {
        val source = generate()
        ThemeTokensCodegen.THEMES.forEach { theme ->
            val json = JsonSlurper().parse(File(repoTokens, "color.$theme.json")) as Map<*, *>
            val palette = source.substringAfter("    val $theme =").substringBefore("\n        )\n")
            assertTrue("$theme palette slice", "accents = ${theme}Accents" in palette && "private val" !in palette)
            expectedRoles.forEach { (path, property) ->
                val hex = json.colorAt("color.$path").substring(1).uppercase()
                assertTrue("$theme $property", "$property = Color(0xFF$hex)" in palette)
            }
            val accents = source.substringAfter("private val ${theme}Accents =").substringBefore("\n        )")
            json.keysAt("color.accent").forEach { accent ->
                val block = accents.substringAfter("Accent.${accent.replaceFirstChar(Char::titlecase)} to").substringBefore("\n                ),")
                expectedAccentRoles.forEach { role ->
                    val hex = json.colorAt("color.accent.$accent.$role").substring(1).uppercase()
                    assertTrue("$theme $accent $role", "$role = Color(0xFF$hex)" in block)
                }
            }
        }
    }

    /** Written out here, not read from the codegen, so a swapped role there fails this test. */
    private val expectedRoles =
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
    private val expectedAccentRoles = listOf("fill", "onFill", "tint", "focus", "text")

    private fun Map<*, *>.nodeAt(path: String): Map<*, *> = path.split('.').fold(this) { node, key -> node[key] as Map<*, *> }

    private fun Map<*, *>.keysAt(path: String) = nodeAt(path).keys.map { it.toString() }.filterNot { it.startsWith("$") }

    /** The hex at a path, following `{alias}` values the way the codegen does. */
    private fun Map<*, *>.colorAt(path: String): String {
        val value = nodeAt(path)["\$value"] as String
        return if (value.startsWith("{")) colorAt(value.removeSurrounding("{", "}")) else value
    }
}
