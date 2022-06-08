package xyz.yorek.plugin.mt

import com.android.build.gradle.AppExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import xyz.yorek.plugin.mt.transform.SingleTransform
import xyz.yorek.plugin.mt.visitor.BaseClassVisitor
import xyz.yorek.plugin.mt.visitor.CodeScanVisitor
import xyz.yorek.plugin.mt.visitor.MethodProxyVisitor

class MethodTracePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw GradleException("MethodTracePlugin Plugin, Android Application plugin required")
        }

        val configuration: MethodTraceExtension = project.extensions.create("methodTrace", MethodTraceExtension::class.java)
        val android = project.extensions.getByType(AppExtension::class.java)

        val transform = SingleTransform(project, configuration, getVisitorList())
        android.registerTransform(transform)
    }

    private fun getVisitorList(): List<Class<out BaseClassVisitor>> {
        return listOf(
            CodeScanVisitor::class.java,
            MethodProxyVisitor::class.java
        )
    }
}