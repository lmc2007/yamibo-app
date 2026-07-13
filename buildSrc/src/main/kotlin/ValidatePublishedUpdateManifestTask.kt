import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class ValidatePublishedUpdateManifestTask : DefaultTask() {
    @get:Input
    abstract val publishedUpdateDirPath: Property<String>

    @TaskAction
    fun validate() {
        val publishedUpdateDir = File(publishedUpdateDirPath.get())
        val manifest = publishedUpdateDir.resolve("manifest.json")
        val stable = publishedUpdateDir.resolve("stable.json")
        if (!manifest.exists()) {
            throw GradleException("Missing published update/manifest.json")
        }
        if (!stable.exists()) {
            throw GradleException("Missing published update/stable.json")
        }

        val manifestText = manifest.readText(Charsets.UTF_8)
        val stableText = stable.readText(Charsets.UTF_8)
        if (manifestText.trim() != stableText.trim()) {
            throw GradleException("Published update/stable.json is out of sync with update/manifest.json.")
        }
        val isReady = extractBoolean(manifestText, "isReady")
            ?: throw GradleException("Published update manifest isReady must be a boolean.")
        if (!isReady) {
            throw GradleException("Published update manifest must set isReady=true.")
        }
        if (extractString(manifestText, "releaseUrl").isNullOrBlank()) {
            throw GradleException("Published update manifest must contain releaseUrl.")
        }
        if (isEmptyArray(manifestText, "assets")) {
            throw GradleException("Published update manifest must contain at least one asset.")
        }
        if (!hasField(manifestText, "url") || !hasField(manifestText, "sha256") || !hasField(manifestText, "size")) {
            throw GradleException("Published update manifest assets must include url, sha256, and size.")
        }
        val versionCode = extractLong(manifestText, "versionCode")
            ?: throw GradleException("Published update manifest versionCode must be an integer.")
        val changelog = publishedUpdateDir.resolve("changelogs/$versionCode.changelog")
        if (!changelog.exists() || changelog.readText(Charsets.UTF_8).isBlank()) {
            throw GradleException("Missing or empty published update/changelogs/$versionCode.changelog")
        }
    }

    private fun extractString(json: String, field: String): String? =
        Regex(""""${Regex.escape(field)}"\s*:\s*"([^"]*)"""")
            .find(json)
            ?.groupValues
            ?.get(1)

    private fun extractBoolean(json: String, field: String): Boolean? =
        Regex(""""${Regex.escape(field)}"\s*:\s*(true|false)""")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.toBooleanStrictOrNull()

    private fun extractLong(json: String, field: String): Long? =
        Regex("\"${Regex.escape(field)}\"\\s*:\\s*(\\d+)")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()

    private fun hasField(json: String, field: String): Boolean =
        Regex(""""${Regex.escape(field)}"\s*:""").containsMatchIn(json)

    private fun isEmptyArray(json: String, field: String): Boolean =
        Regex(""""${Regex.escape(field)}"\s*:\s*\[\s*]""", RegexOption.DOT_MATCHES_ALL).containsMatchIn(json)
}
