package xyz.yorek.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import xyz.yorek.plugin.MinifySoMode
import xyz.yorek.plugin.Util
import java.io.File

open class MinifySoTask : DefaultTask() {
    companion object {
        const val TAG = "MinifySoTask"
        const val BUILD_VARIANT = "build_variant"
        const val ABI_FILTER = "abi_filter"
        const val MINIFY_SO_MODE = "minify_so_mode"

        @JvmStatic
        fun log(tag: String, message: String, vararg obj: Any) {
            val log = if (obj.isEmpty())  message else String.format(message, obj)
            println(String.format("[INFO][%s] %s", tag, log))
        }
    }

    init {
        group = "yorek"
    }

    @TaskAction
    fun doAction() {
        val variantName = this.inputs.properties[BUILD_VARIANT] as String
        val abiFilter = this.inputs.properties[ABI_FILTER] as String
        val minifySoMode = this.inputs.properties[MINIFY_SO_MODE] as String

        log(TAG, "variant=$variantName, abiFilter=$abiFilter, mode=$minifySoMode")

        when (minifySoMode) {
            MinifySoMode.ASSET.label -> {
                minifySo2Asset(variantName, abiFilter)
            }
            MinifySoMode.ZIP.label -> {
                minifySo2Server(variantName, abiFilter)
            }
            else -> {
                log(TAG, "mode is none, skip!")
            }
        }
    }

    private fun minifySo2Asset(variantName: String, abiFilter: String) {
        val assetDir = "${project.buildDir.absolutePath}/intermediates/assets/${variantName.lowercase()}/merge${variantName}Assets/"
        val nativeLibs = "${project.buildDir.absolutePath}/intermediates/stripped_native_libs/${variantName.lowercase()}/out/lib/$abiFilter"

        val libsZipName = "libs_asset.zip"
        val libsZipAbsolutePath = File(assetDir, libsZipName)

        if (libsZipAbsolutePath.exists()) {
            libsZipAbsolutePath.delete()
        }

        // TODO 7z path
        val ret = Util.sevenZipInputDir(File(nativeLibs), libsZipAbsolutePath, "${project.rootDir}/7zz")
        if (ret) {
            replaceSoWithEmpty(File(nativeLibs))
        }

        log(TAG, "minify flag: ${ret}, libs location:$nativeLibs, asset location:${libsZipAbsolutePath.absolutePath}")
    }

    private fun minifySo2Server(variantName: String, abiFilter: String) {
        val nativeLibs = "${project.buildDir.absolutePath}/intermediates/stripped_native_libs/${variantName.lowercase()}/out/lib/$abiFilter"

        val outputDir = "${project.buildDir.absolutePath}/outputs/libs/${variantName.lowercase()}/$abiFilter"

        File(nativeLibs).listFiles()?.forEach {
            Util.copyFileUsingStream(it, File(outputDir, it.name))
        }
        replaceSoWithEmpty(File(nativeLibs))

        log(TAG, "minify , libs location:$nativeLibs, libs location:${outputDir}")
    }

    private fun replaceSoWithEmpty(file: File) {
        if (file.exists() && file.canRead() && file.canWrite()) {
            if (file.isDirectory) {
                for (f in file.listFiles()!!) {
                    replaceSoWithEmpty(f)
                }
            } else {
                file.delete()
                File(file.absolutePath).createNewFile()
            }
        }
    }
}