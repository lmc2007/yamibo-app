import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GenerateYamiboIconsTaskTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generates stable image vector API from supported SVG`() {
        val source = tempDir.resolve("icons")
        Files.createDirectories(source)
        Files.writeString(
            source.resolve("sample_icon.svg"),
            """
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="18" viewBox="0 0 24 18">
                  <g transform="translate(2 3)">
                    <path d="M 1 2 l 3 4 A 5 6 10 0 1 7 8 Z" transform="scale(2)" fill="#123456" fill-rule="evenodd"
                          stroke="#abcdef" stroke-width="2" stroke-linecap="round" stroke-linejoin="bevel"/>
                  </g>
                </svg>
            """.trimIndent(),
        )
        val output = tempDir.resolve("generated/YamiboIcons.kt")
        generate(source, output)
        val first = Files.readString(output)
        generate(source, output)

        assertEquals(first, Files.readString(output))
        assertContains(first, "val SampleIcon: ImageVector")
        assertContains(first, "defaultHeight = 18.0f.dp")
        assertContains(first, "translationX = 2.0f")
        assertContains(first, "scaleX = 2.0f")
        assertContains(first, "PathNode.RelativeLineTo(3.0f, 4.0f)")
        assertContains(first, "PathNode.ArcTo(5.0f, 6.0f, 10.0f, false, true, 7.0f, 8.0f)")
        assertContains(first, "PathFillType.EvenOdd")
        assertContains(first, "StrokeCap.Round")
        assertContains(first, "StrokeJoin.Bevel")
    }

    @Test
    fun `rejects duplicate generated names`() {
        val source = tempDir.resolve("icons")
        Files.createDirectories(source.resolve("nested"))
        val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1 1\"><path d=\"M0 0Z\"/></svg>"
        Files.writeString(source.resolve("same-name.svg"), svg)
        Files.writeString(source.resolve("nested/same_name.svg"), svg)

        val error = assertFailsWith<IllegalStateException> {
            generate(source, tempDir.resolve("generated/YamiboIcons.kt"))
        }
        assertContains(error.message.orEmpty(), "Duplicate generated YamiboIcons names")
    }

    @Test
    fun `rejects unsupported CSS and SVG elements with source path`() {
        val source = tempDir.resolve("icons")
        Files.createDirectories(source)
        Files.writeString(
            source.resolve("bad.svg"),
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1 1\"><circle style=\"fill:black\" cx=\"0\" cy=\"0\" r=\"1\"/></svg>",
        )

        val error = assertFailsWith<GradleException> {
            generate(source, tempDir.resolve("generated/YamiboIcons.kt"))
        }
        assertContains(error.message.orEmpty(), "bad.svg")
        assertContains(error.message.orEmpty(), "CSS style attributes are not supported")
    }

    @Test
    fun `rejects invalid path characters`() {
        val source = tempDir.resolve("icons")
        Files.createDirectories(source)
        Files.writeString(
            source.resolve("bad_path.svg"),
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1 1\"><path d=\"M0 0 @ Z\"/></svg>",
        )

        val error = assertFailsWith<GradleException> {
            generate(source, tempDir.resolve("generated/YamiboIcons.kt"))
        }
        assertContains(error.message.orEmpty(), "Invalid path data")
    }

    @Test
    fun `rejects directories without SVG files`() {
        val source = tempDir.resolve("icons")
        Files.createDirectories(source)

        val error = assertFailsWith<IllegalStateException> {
            generate(source, tempDir.resolve("generated/YamiboIcons.kt"))
        }
        assertContains(error.message.orEmpty(), "No SVG icons found")
    }

    private fun generate(source: Path, output: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        val task = project.tasks.register("generate${System.nanoTime()}", GenerateYamiboIconsTask::class.java).get()
        task.sourceDir.set(source.toFile())
        task.outputFile.set(output.toFile())
        task.generate()
    }
}
