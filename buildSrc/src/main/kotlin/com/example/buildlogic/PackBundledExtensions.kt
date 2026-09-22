package com.example.buildlogic

import com.example.data.extensions.ExtensionManifest
import com.example.data.extensions.otips.OTipsFormat
import com.example.data.extensions.otips.OTipsWriter
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.json.JSONObject
import java.io.File

/**
 * Packs every bundled extension source into an `.OTips` package.
 *
 * Input: `src/extensions/<id>/manifest.json` and `src/extensions/<id>/content/`.
 * Output: `<outputDir>/bundled_extensions/<id>.OTips`, added to the APK's
 * assets by the app build script. Only the packages ship; the raw sources
 * never reach the APK. Up to date when no source file changed.
 */
abstract class PackBundledExtensions : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun pack() {
        val target = File(outputDir.get().asFile, ASSET_DIR)
        target.deleteRecursively()
        target.mkdirs()

        val folders = sources.get().asFile.listFiles().orEmpty()
            .filter { File(it, MANIFEST).isFile }
            .sortedBy { it.name }
        for (folder in folders) {
            val manifest = ExtensionManifest.fromJson(JSONObject(File(folder, MANIFEST).readText(Charsets.UTF_8)))
            check(folder.name == manifest.id) { "${folder.path}: folder name must equal the id '${manifest.id}'" }
            val content = File(folder, CONTENT)
            check(content.isDirectory) { "${folder.path}: missing $CONTENT/ folder" }

            val packageFile = File(target, OTipsFormat.fileName(manifest.id))
            packageFile.outputStream().buffered().use { out ->
                OTipsWriter.write(manifest, content, out, temporaryDir, CREATED_BY, 0L)
            }
            logger.lifecycle("Packed ${manifest.id} ${manifest.version} (${packageFile.length() / 1024} KB)")
        }
    }

    private companion object {
        /** Must match BundledExtensions.ASSET_DIR in the app. */
        const val ASSET_DIR = "bundled_extensions"
        const val MANIFEST = "manifest.json"
        const val CONTENT = "content"
        const val CREATED_BY = "Ortho Tips build"
    }
}
