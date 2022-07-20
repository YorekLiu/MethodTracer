package xyz.yorek.plugin

import com.android.build.OutputFile
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import xyz.yorek.plugin.task.MinifySoTask

class ApmSamplePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw GradleException("ApmSamplePlugin Plugin, Android Application plugin required")
        }

        val android = project.extensions.getByType(AppExtension::class.java)

        val apmSampleExtension: APMSampleExtension = project.extensions.create("apmSample", APMSampleExtension::class.java)
        val minifySoEx = (apmSampleExtension as ExtensionAware).extensions.create("minifySo", MinifySoExtension::class.java)

        createMinifySoTask(project, android, apmSampleExtension, minifySoEx)
    }

    private fun createMinifySoTask(
        project: Project,
        android: AppExtension,
        apmSampleExtension: APMSampleExtension,
        minifySoEx: MinifySoExtension
    ) {
        project.afterEvaluate {
            if (!apmSampleExtension.enable) return@afterEvaluate

            android.applicationVariants.all { variant ->
                val variantName = variant.name.capitalize()
                if (variantName.equals("Release", true) || variantName.equals("Debug", true)) {
                    variant.outputs.forEach { output ->
                        val abiFilter = (output as BaseVariantOutputImpl).getFilter(OutputFile.ABI)
                        val taskName = "minifySo${variantName}${abiFilter}"
                        if (project.tasks.findByName(taskName) != null) {
                            return@forEach
                        }

                        val minifySoTask = project.tasks.create(taskName, MinifySoTask::class.java)
                        minifySoTask.inputs.property(MinifySoTask.BUILD_VARIANT, variantName)
                        minifySoTask.inputs.property(MinifySoTask.ABI_FILTER, abiFilter)
                        minifySoTask.inputs.property(MinifySoTask.MINIFY_SO_MODE, minifySoEx.mode)

                        if (minifySoEx.mode == MinifySoMode.ASSET.label) {
                            val mergeAssets = project.tasks.findByName("merge${variantName}Assets")
                            val compressAssets =
                                project.tasks.findByName("compress${variantName}Assets")
                            if (mergeAssets != null && compressAssets != null) {
                                minifySoTask.dependsOn(mergeAssets)
                                compressAssets.dependsOn(minifySoTask)
                            } else {
                                MinifySoTask.log(
                                    "ApmSamplePlugin",
                                    "can't find task merge${variantName}Assets or compress${variantName}Assets"
                                )
                            }
                        }

                        val stripDebugSymbol =
                            project.tasks.findByName("strip${variantName}DebugSymbols")
                        minifySoTask.dependsOn(stripDebugSymbol)
                        variant.packageApplication.dependsOn(minifySoTask)
                    }
                }
            }
        }
    }
}