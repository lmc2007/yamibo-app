import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@CacheableTask
abstract class GenerateYamiboIconsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val root = sourceDir.get().asFile
        check(root.isDirectory) { "Yamibo icon source directory does not exist or is not a directory: ${root.absolutePath}" }
        val files = root.walkTopDown()
            .filter { it.isFile && it.extension.equals("svg", ignoreCase = true) }
            .sortedBy { it.relativeTo(root).invariantSeparatorsPath.lowercase() }
            .toList()
        check(files.isNotEmpty()) { "No SVG icons found under: ${root.absolutePath}" }

        val icons = files.map { parseIcon(root, it) }
        val duplicates = icons.groupBy { it.kotlinName }.filterValues { it.size > 1 }
        check(duplicates.isEmpty()) {
            duplicates.entries.joinToString(
                prefix = "Duplicate generated YamiboIcons names:\n",
                separator = "\n",
            ) { (name, matches) -> "$name: ${matches.joinToString { it.sourcePath }}" }
        }

        val destination = outputFile.get().asFile
        destination.parentFile.mkdirs()
        destination.writeText(renderKotlin(icons), Charsets.UTF_8)
    }

    private fun parseIcon(root: File, file: File): SvgIcon {
        val relativePath = file.relativeTo(root).invariantSeparatorsPath
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "")
                setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "")
            }
            val document = file.inputStream().buffered().use { factory.newDocumentBuilder().parse(it) }
            val svg = document.documentElement
            require(svg.localNameOrTag() == "svg") { "Root element must be <svg>" }
            rejectStyle(svg)
            require(!svg.hasAttribute("opacity")) { "Root SVG opacity is not supported; apply opacity to individual paths" }
            val viewBox = svg.attribute("viewBox")?.let(::parseNumbers)
                ?: throw IllegalArgumentException("Missing viewBox")
            require(viewBox.size == 4) { "viewBox must contain exactly four numbers" }
            require(viewBox[0] == 0f && viewBox[1] == 0f) {
                "Only viewBox values starting at 0 0 are supported; found ${viewBox.joinToString(" ")}" 
            }
            require(viewBox[2] > 0f && viewBox[3] > 0f) { "viewBox width and height must be positive" }
            val width = parseDimension(svg.attribute("width")) ?: viewBox[2]
            val height = parseDimension(svg.attribute("height")) ?: viewBox[3]
            require(width > 0f && height > 0f) { "width and height must be positive" }
            val state = SvgStyle.from(svg, SvgStyle())
            val children = parseChildren(svg, state, relativePath)
            require(children.isNotEmpty()) { "SVG contains no drawable paths" }
            return SvgIcon(
                kotlinName = file.nameWithoutExtension.toKotlinIdentifier(),
                sourcePath = relativePath,
                width = width,
                height = height,
                viewportWidth = viewBox[2],
                viewportHeight = viewBox[3],
                children = children,
            )
        } catch (error: Exception) {
            if (error is GradleException) throw error
            throw GradleException("Invalid Yamibo icon SVG '$relativePath': ${error.message}", error)
        }
    }

    private fun parseChildren(parent: Element, inherited: SvgStyle, sourcePath: String): List<SvgNode> {
        val result = mutableListOf<SvgNode>()
        val nodes = parent.childNodes
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node.nodeType == Node.TEXT_NODE || node.nodeType == Node.COMMENT_NODE) continue
            require(node.nodeType == Node.ELEMENT_NODE) { "Unsupported XML node in $sourcePath" }
            val element = node as Element
            rejectStyle(element)
            when (element.localNameOrTag()) {
                "path" -> {
                    val pathData = element.attribute("d") ?: throw IllegalArgumentException("<path> is missing d")
                    val style = SvgStyle.from(element, inherited)
                    val path = SvgPath(
                        nodes = SvgPathParser(pathData).parse(),
                        fill = parsePaint(style.fill, defaultBlack = true),
                        fillAlpha = style.opacity * style.fillOpacity,
                        fillType = when (style.fillRule.lowercase()) {
                            "nonzero" -> "NonZero"
                            "evenodd" -> "EvenOdd"
                            else -> throw IllegalArgumentException("Unsupported fill-rule '${style.fillRule}'")
                        },
                        stroke = parsePaint(style.stroke, defaultBlack = false),
                        strokeAlpha = style.opacity * style.strokeOpacity,
                        strokeWidth = style.strokeWidth,
                        strokeCap = when (style.strokeLineCap.lowercase()) {
                            "butt" -> "Butt"
                            "round" -> "Round"
                            "square" -> "Square"
                            else -> throw IllegalArgumentException("Unsupported stroke-linecap '${style.strokeLineCap}'")
                        },
                        strokeJoin = when (style.strokeLineJoin.lowercase()) {
                            "miter" -> "Miter"
                            "round" -> "Round"
                            "bevel" -> "Bevel"
                            else -> throw IllegalArgumentException("Unsupported stroke-linejoin '${style.strokeLineJoin}'")
                        },
                        strokeMiter = style.strokeMiter,
                    )
                    result += wrapTransforms(path, element.attribute("transform"))
                }
                "g" -> {
                    require(!element.hasAttribute("opacity")) {
                        "Group opacity is not supported because ImageVector groups have no alpha; apply opacity to individual paths"
                    }
                    val style = SvgStyle.from(element, inherited)
                    val children = parseChildren(element, style, sourcePath)
                    require(children.isNotEmpty()) { "Empty <g> is not supported" }
                    result += wrapTransforms(SvgGroup(children), element.attribute("transform"))
                }
                else -> throw IllegalArgumentException("Unsupported SVG element <${element.localNameOrTag()}>")
            }
        }
        return result
    }

    private fun renderKotlin(icons: List<SvgIcon>): String = buildString {
        appendLine("// Generated by :composeApp:generateYamiboIcons. Do not edit manually.")
        appendLine("import androidx.compose.ui.graphics.Color")
        appendLine("import androidx.compose.ui.graphics.PathFillType")
        appendLine("import androidx.compose.ui.graphics.SolidColor")
        appendLine("import androidx.compose.ui.graphics.StrokeCap")
        appendLine("import androidx.compose.ui.graphics.StrokeJoin")
        appendLine("import androidx.compose.ui.graphics.vector.ImageVector")
        appendLine("import androidx.compose.ui.graphics.vector.PathNode")
        appendLine("import androidx.compose.ui.graphics.vector.group")
        appendLine("import androidx.compose.ui.unit.dp")
        appendLine()
        appendLine("object YamiboIcons {")
        icons.forEach { icon -> appendLine("    val ${icon.kotlinName}: ImageVector by lazy { build${icon.kotlinName}() }") }
        appendLine()
        icons.forEach { icon ->
            appendLine("    private fun build${icon.kotlinName}(): ImageVector =")
            appendLine("        ImageVector.Builder(")
            appendLine("            name = \"${icon.kotlinName}\",")
            appendLine("            defaultWidth = ${icon.width.kotlinFloat()}.dp,")
            appendLine("            defaultHeight = ${icon.height.kotlinFloat()}.dp,")
            appendLine("            viewportWidth = ${icon.viewportWidth.kotlinFloat()},")
            appendLine("            viewportHeight = ${icon.viewportHeight.kotlinFloat()},")
            appendLine("        ).apply {")
            icon.children.forEach { renderNode(it, "            ") }
            appendLine("        }.build()")
            appendLine()
        }
        appendLine("}")
    }

    private fun StringBuilder.renderNode(node: SvgNode, indent: String) {
        when (node) {
            is SvgPath -> {
                appendLine("${indent}addPath(")
                appendLine("$indent    pathData = listOf(")
                node.nodes.forEach { appendLine("$indent        $it,") }
                appendLine("$indent    ),")
                appendLine("$indent    pathFillType = PathFillType.${node.fillType},")
                appendLine("$indent    fill = ${node.fill?.let { "SolidColor(${it.kotlinColor()})" } ?: "null"},")
                appendLine("$indent    fillAlpha = ${node.fillAlpha.kotlinFloat()},")
                appendLine("$indent    stroke = ${node.stroke?.let { "SolidColor(${it.kotlinColor()})" } ?: "null"},")
                appendLine("$indent    strokeAlpha = ${node.strokeAlpha.kotlinFloat()},")
                appendLine("$indent    strokeLineWidth = ${node.strokeWidth.kotlinFloat()},")
                appendLine("$indent    strokeLineCap = StrokeCap.${node.strokeCap},")
                appendLine("$indent    strokeLineJoin = StrokeJoin.${node.strokeJoin},")
                appendLine("$indent    strokeLineMiter = ${node.strokeMiter.kotlinFloat()},")
                appendLine("${indent})")
            }
            is SvgGroup -> {
                val transform = node.transform
                if (transform == null) {
                    node.children.forEach { renderNode(it, indent) }
                } else {
                    appendLine("${indent}group(")
                    when (transform) {
                        is SvgTransform.Translate -> {
                            appendLine("$indent    translationX = ${transform.x.kotlinFloat()},")
                            appendLine("$indent    translationY = ${transform.y.kotlinFloat()},")
                        }
                        is SvgTransform.Scale -> {
                            appendLine("$indent    scaleX = ${transform.x.kotlinFloat()},")
                            appendLine("$indent    scaleY = ${transform.y.kotlinFloat()},")
                        }
                        is SvgTransform.Rotate -> {
                            appendLine("$indent    rotate = ${transform.degrees.kotlinFloat()},")
                            appendLine("$indent    pivotX = ${transform.pivotX.kotlinFloat()},")
                            appendLine("$indent    pivotY = ${transform.pivotY.kotlinFloat()},")
                        }
                    }
                    appendLine("${indent}) {")
                    node.children.forEach { renderNode(it, "$indent    ") }
                    appendLine("${indent}}")
                }
            }
        }
    }

    private fun parseTransforms(raw: String?): List<SvgTransform> {
        if (raw.isNullOrBlank()) return emptyList()
        val expression = Regex("([A-Za-z]+)\\s*\\(([^)]*)\\)")
        val matches = expression.findAll(raw).toList()
        require(matches.isNotEmpty()) { "Invalid transform '$raw'" }
        val leftover = expression.replace(raw, "").replace(",", "").trim()
        require(leftover.isEmpty()) { "Invalid transform '$raw'" }
        return matches.map { match ->
            val name = match.groupValues[1].lowercase()
            val numbers = parseNumbers(match.groupValues[2])
            when (name) {
                "translate" -> {
                    require(numbers.size in 1..2) { "translate() requires one or two numbers" }
                    SvgTransform.Translate(numbers[0], numbers.getOrElse(1) { 0f })
                }
                "scale" -> {
                    require(numbers.size in 1..2) { "scale() requires one or two numbers" }
                    SvgTransform.Scale(numbers[0], numbers.getOrElse(1) { numbers[0] })
                }
                "rotate" -> {
                    require(numbers.size == 1 || numbers.size == 3) { "rotate() requires one or three numbers" }
                    SvgTransform.Rotate(numbers[0], numbers.getOrElse(1) { 0f }, numbers.getOrElse(2) { 0f })
                }
                else -> throw IllegalArgumentException("Unsupported transform '$name'")
            }
        }
    }

    private fun wrapTransforms(node: SvgNode, raw: String?): SvgNode =
        parseTransforms(raw).asReversed().fold(node) { child, transform -> SvgGroup(listOf(child), transform) }

    private fun rejectStyle(element: Element) {
        require(!element.hasAttribute("style")) { "CSS style attributes are not supported; use SVG presentation attributes" }
        require(!element.hasAttribute("class")) { "CSS classes are not supported" }
    }

    private fun parsePaint(raw: String, defaultBlack: Boolean): Int? {
        val value = raw.trim().lowercase()
        if (value == "none") return null
        if (value.isBlank()) return if (defaultBlack) 0xff000000.toInt() else null
        if (value == "black" || value == "currentcolor") return 0xff000000.toInt()
        require(value.startsWith('#')) { "Unsupported paint '$raw'; use none, black, currentColor, or a hex color" }
        return when (val hex = value.drop(1)) {
            else -> when (hex.length) {
                3 -> ("ff" + hex.flatMap { listOf(it, it) }.joinToString("")).toLong(16).toInt()
                4 -> {
                    val rgba = hex.flatMap { listOf(it, it) }.joinToString("")
                    (rgba.takeLast(2) + rgba.dropLast(2)).toLong(16).toInt()
                }
                6 -> ("ff$hex").toLong(16).toInt()
                8 -> (hex.takeLast(2) + hex.dropLast(2)).toLong(16).toInt()
                else -> throw IllegalArgumentException("Unsupported hex color '$raw'")
            }
        }
    }

    private fun parseDimension(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim().removeSuffix("px")
        require(!value.endsWith('%')) { "Percentage dimensions are not supported" }
        return value.toFloatOrNull() ?: throw IllegalArgumentException("Invalid dimension '$raw'")
    }

    private fun parseNumbers(raw: String): List<Float> {
        val matches = numberRegex.findAll(raw).toList()
        var cursor = 0
        matches.forEach { match ->
            require(raw.substring(cursor, match.range.first).all { it.isWhitespace() || it == ',' }) {
                "Invalid numeric list '$raw'"
            }
            cursor = match.range.last + 1
        }
        require(raw.substring(cursor).all { it.isWhitespace() || it == ',' }) { "Invalid numeric list '$raw'" }
        return matches.map { it.value.toFloat() }
    }

    private fun Element.localNameOrTag(): String = localName ?: tagName.substringAfter(':')

    private fun String.toKotlinIdentifier(): String {
        val words = split(Regex("[^A-Za-z0-9]+"), limit = 0).filter { it.isNotEmpty() }
        require(words.isNotEmpty()) { "Icon filename must contain letters or digits" }
        val name = words.joinToString("") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }
        require(name.first().isLetter() || name.first() == '_') { "Generated name '$name' is not a Kotlin identifier" }
        return name
    }

    private data class SvgIcon(
        val kotlinName: String,
        val sourcePath: String,
        val width: Float,
        val height: Float,
        val viewportWidth: Float,
        val viewportHeight: Float,
        val children: List<SvgNode>,
    )

    private sealed interface SvgNode

    private data class SvgGroup(
        val children: List<SvgNode>,
        val transform: SvgTransform? = null,
    ) : SvgNode

    private data class SvgPath(
        val nodes: List<String>,
        val fill: Int?,
        val fillAlpha: Float,
        val fillType: String,
        val stroke: Int?,
        val strokeAlpha: Float,
        val strokeWidth: Float,
        val strokeCap: String,
        val strokeJoin: String,
        val strokeMiter: Float,
    ) : SvgNode

    private sealed interface SvgTransform {
        data class Translate(val x: Float, val y: Float) : SvgTransform
        data class Scale(val x: Float, val y: Float) : SvgTransform
        data class Rotate(val degrees: Float, val pivotX: Float, val pivotY: Float) : SvgTransform
    }

    private data class SvgStyle(
        val fill: String = "",
        val fillOpacity: Float = 1f,
        val fillRule: String = "nonzero",
        val stroke: String = "none",
        val strokeOpacity: Float = 1f,
        val strokeWidth: Float = 1f,
        val strokeLineCap: String = "butt",
        val strokeLineJoin: String = "miter",
        val strokeMiter: Float = 4f,
        val opacity: Float = 1f,
    ) {
        companion object {
            fun from(element: Element, inherited: SvgStyle): SvgStyle {
                fun number(name: String, fallback: Float): Float =
                    element.attribute(name)?.let { raw ->
                        raw.toFloatOrNull() ?: throw IllegalArgumentException("Invalid $name '$raw'")
                    } ?: fallback
                return SvgStyle(
                    fill = element.attribute("fill") ?: inherited.fill,
                    fillOpacity = number("fill-opacity", inherited.fillOpacity),
                    fillRule = element.attribute("fill-rule") ?: inherited.fillRule,
                    stroke = element.attribute("stroke") ?: inherited.stroke,
                    strokeOpacity = number("stroke-opacity", inherited.strokeOpacity),
                    strokeWidth = number("stroke-width", inherited.strokeWidth),
                    strokeLineCap = element.attribute("stroke-linecap") ?: inherited.strokeLineCap,
                    strokeLineJoin = element.attribute("stroke-linejoin") ?: inherited.strokeLineJoin,
                    strokeMiter = number("stroke-miterlimit", inherited.strokeMiter),
                    opacity = inherited.opacity * number("opacity", 1f),
                ).also {
                require(it.fillOpacity in 0f..1f) { "fill-opacity must be between 0 and 1" }
                require(it.strokeOpacity in 0f..1f) { "stroke-opacity must be between 0 and 1" }
                require(it.opacity in 0f..1f) { "opacity must be between 0 and 1" }
                require(it.strokeWidth >= 0f) { "stroke-width must not be negative" }
                require(it.strokeMiter >= 0f) { "stroke-miterlimit must not be negative" }
            }
            }
        }
    }

    private class SvgPathParser(source: String) {
        private val tokens = tokenize(source)
        private var index = 0

        fun parse(): List<String> {
            require(tokens.isNotEmpty()) { "Path data is empty" }
            val result = mutableListOf<String>()
            var command: Char? = null
            while (index < tokens.size) {
                if (tokens[index].length == 1 && tokens[index][0].isLetter()) command = tokens[index++][0]
                val active = command ?: throw IllegalArgumentException("Path data must begin with a command")
                val upper = active.uppercaseChar()
                if (upper == 'Z') {
                    result += "PathNode.Close"
                    command = null
                    continue
                }
                val arity = commandArity[upper] ?: throw IllegalArgumentException("Unsupported path command '$active'")
                var first = true
                do {
                    require(index + arity <= tokens.size) { "Path command '$active' is missing parameters" }
                    val values = (0 until arity).map { offset -> tokens[index + offset] }
                    require(values.none { it.length == 1 && it[0].isLetter() }) { "Path command '$active' is missing parameters" }
                    index += arity
                    val actual = if (upper == 'M' && !first) if (active.isLowerCase()) 'l' else 'L' else active
                    result += renderPathNode(actual, values)
                    first = false
                } while (index < tokens.size && !(tokens[index].length == 1 && tokens[index][0].isLetter()))
            }
            return result
        }

        private fun renderPathNode(command: Char, values: List<String>): String {
            fun f(position: Int): String = values[position].toFloat().kotlinFloat()
            fun flag(position: Int): String = when (values[position]) {
                "0", "0.0" -> "false"
                "1", "1.0" -> "true"
                else -> throw IllegalArgumentException("Arc flags must be 0 or 1")
            }
            return when (command) {
                'M' -> "PathNode.MoveTo(${f(0)}, ${f(1)})"
                'm' -> "PathNode.RelativeMoveTo(${f(0)}, ${f(1)})"
                'L' -> "PathNode.LineTo(${f(0)}, ${f(1)})"
                'l' -> "PathNode.RelativeLineTo(${f(0)}, ${f(1)})"
                'H' -> "PathNode.HorizontalTo(${f(0)})"
                'h' -> "PathNode.RelativeHorizontalTo(${f(0)})"
                'V' -> "PathNode.VerticalTo(${f(0)})"
                'v' -> "PathNode.RelativeVerticalTo(${f(0)})"
                'C' -> "PathNode.CurveTo(${f(0)}, ${f(1)}, ${f(2)}, ${f(3)}, ${f(4)}, ${f(5)})"
                'c' -> "PathNode.RelativeCurveTo(${f(0)}, ${f(1)}, ${f(2)}, ${f(3)}, ${f(4)}, ${f(5)})"
                'S' -> "PathNode.ReflectiveCurveTo(${f(0)}, ${f(1)}, ${f(2)}, ${f(3)})"
                's' -> "PathNode.RelativeReflectiveCurveTo(${f(0)}, ${f(1)}, ${f(2)}, ${f(3)})"
                'Q' -> "PathNode.QuadTo(${f(0)}, ${f(1)}, ${f(2)}, ${f(3)})"
                'q' -> "PathNode.RelativeQuadTo(${f(0)}, ${f(1)}, ${f(2)}, ${f(3)})"
                'T' -> "PathNode.ReflectiveQuadTo(${f(0)}, ${f(1)})"
                't' -> "PathNode.RelativeReflectiveQuadTo(${f(0)}, ${f(1)})"
                'A' -> "PathNode.ArcTo(${f(0)}, ${f(1)}, ${f(2)}, ${flag(3)}, ${flag(4)}, ${f(5)}, ${f(6)})"
                'a' -> "PathNode.RelativeArcTo(${f(0)}, ${f(1)}, ${f(2)}, ${flag(3)}, ${flag(4)}, ${f(5)}, ${f(6)})"
                else -> throw IllegalArgumentException("Unsupported path command '$command'")
            }
        }

        private fun tokenize(pathData: String): List<String> {
            val matches = tokenRegex.findAll(pathData).toList()
            var cursor = 0
            matches.forEach { match ->
                require(pathData.substring(cursor, match.range.first).all { it.isWhitespace() || it == ',' }) {
                    "Invalid path data near '${pathData.substring(cursor, match.range.first).trim()}'"
                }
                cursor = match.range.last + 1
            }
            require(pathData.substring(cursor).all { it.isWhitespace() || it == ',' }) {
                "Invalid path data near '${pathData.substring(cursor).trim()}'"
            }
            return matches.map { it.value }
        }
    }

    companion object {
        private val numberRegex = Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?")
        private val tokenRegex = Regex("[AaCcHhLlMmQqSsTtVvZz]|[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?")
        private val commandArity = mapOf(
            'M' to 2, 'L' to 2, 'H' to 1, 'V' to 1,
            'C' to 6, 'S' to 4, 'Q' to 4, 'T' to 2, 'A' to 7,
        )
    }
}

private fun Element.attribute(name: String): String? =
    getAttribute(name).takeIf { hasAttribute(name) }

private fun Float.kotlinFloat(): String =
    if (this == toLong().toFloat()) "${toLong()}.0f" else "${toString()}f"

private fun Int.kotlinColor(): String = "Color(0x${toUInt().toString(16).padStart(8, '0')}L)"
