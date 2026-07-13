import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class I18nAutoMergePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("i18nAutoMerge", I18nAutoMergeExtension::class.java, project)

        val generate = project.tasks.register<GenerateI18nResourcesTask>("generateI18nResources") {
            group = "i18n"
            description = "Scans lowercase i18n calls and generates Compose resources plus runtime mapping."
            rootProjectDir.set(project.rootProject.layout.projectDirectory)
            configFile.set(extension.configFile)
            scanDirs.set(extension.scanDirs)
            glossary.set(extension.glossary)
            baseTranslations.set(extension.baseTranslations)
            composeAssetResourcesDir.set(extension.composeAssetResourcesDir)
            outputComposeResources.set(extension.outputComposeResources)
            outputKotlin.set(extension.outputKotlin)
            reportDir.set(extension.reportDir)
            defaultLanguage.set(extension.defaultLanguage)
            fallbackToSource.set(extension.fallbackToSource)
            failOnPlaceholderMismatch.set(extension.failOnPlaceholderMismatch)
            failOnMissingTranslation.set(extension.failOnMissingTranslation)
            failOnMissingTerm.set(extension.failOnMissingTerm)
            apiFunctionName.set(extension.apiFunctionName)
            runtimePackage.set(extension.runtimePackage)
            resImportPackage.set(extension.resImportPackage)
            outputs.upToDateWhen { false }
        }

        project.tasks.register("mergeI18nResources") {
            group = "i18n"
            description = "Merges scanned i18n sources, glossary entries, and existing translations."
            dependsOn(generate)
        }

        project.tasks.register("checkI18n") {
            group = "i18n"
            description = "Validates generated i18n keys, placeholders, XML, and warnings."
            dependsOn(generate)
        }

        project.tasks.configureEach {
            val taskName = name
            val shouldDepend =
                taskName == "preBuild" ||
                    taskName == "assembleDebug" ||
                    taskName == "assembleRelease" ||
                    taskName == "installDebug" ||
                    taskName.startsWith("compileKotlin") ||
                    taskName.contains("XmlValueResources", ignoreCase = true) ||
                    taskName.contains("ComposeResources", ignoreCase = true) ||
                    (taskName.contains("generate", ignoreCase = true) && taskName.contains("Res", ignoreCase = true)) ||
                    taskName == "generateResourceAccessorsForCommonMain"
            if (shouldDepend && taskName != "generateI18nResources") {
                dependsOn(generate)
            }
        }
    }
}
