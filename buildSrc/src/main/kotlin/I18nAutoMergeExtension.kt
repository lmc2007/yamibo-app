import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class I18nAutoMergeExtension(project: Project) {
    val configFile: RegularFileProperty = project.objects.fileProperty()
        .convention(project.rootProject.layout.projectDirectory.file("i18n/i18n.properties"))
    val scanDirs: ListProperty<String> = project.objects.listProperty(String::class.java)
        .convention(listOf("composeApp/src", "shared/src"))
    val glossary: RegularFileProperty = project.objects.fileProperty()
        .convention(project.rootProject.layout.projectDirectory.file("i18n/glossary.csv"))
    val baseTranslations: RegularFileProperty = project.objects.fileProperty()
        .convention(project.rootProject.layout.projectDirectory.file("i18n/base.csv"))
    val composeAssetResourcesDir: DirectoryProperty = project.objects.directoryProperty()
        .convention(project.rootProject.layout.projectDirectory.dir("composeApp/src/commonMain/composeResources"))
    val outputComposeResources: DirectoryProperty = project.objects.directoryProperty()
        .convention(project.layout.buildDirectory.dir("generated/i18n/composeResources"))
    val outputKotlin: DirectoryProperty = project.objects.directoryProperty()
        .convention(project.layout.buildDirectory.dir("generated/i18n/kotlin"))
    val reportDir: DirectoryProperty = project.objects.directoryProperty()
        .convention(project.layout.buildDirectory.dir("reports/i18n"))
    val defaultLanguage: Property<String> = project.objects.property(String::class.java)
        .convention("zh-tw")
    val fallbackToSource: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(true)
    val failOnPlaceholderMismatch: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(true)
    val failOnMissingTranslation: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(false)
    val failOnMissingTerm: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(true)
    val apiFunctionName: Property<String> = project.objects.property(String::class.java)
        .convention("i18n")
    val runtimePackage: Property<String> = project.objects.property(String::class.java)
        .convention("me.thenano.yamibo.yamibo_app.i18n")
    val resImportPackage: Property<String> = project.objects.property(String::class.java)
        .convention("yamibo_app.composeapp.generated.resources")
}
