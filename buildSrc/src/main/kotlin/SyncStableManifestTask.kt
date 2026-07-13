import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class SyncStableManifestTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: RegularFileProperty

    @get:OutputFile
    abstract val stableFile: RegularFileProperty

    @TaskAction
    fun sync() {
        val source = manifestFile.get().asFile
        require(source.exists()) { "Missing update/manifest.json" }
        val destination = stableFile.get().asFile
        destination.parentFile.mkdirs()
        destination.writeText(source.readText(Charsets.UTF_8), Charsets.UTF_8)
    }
}
