package xyz.yorek.plugin.mt

import com.android.build.gradle.AppExtension
import xyz.yorek.plugin.mt.transform.PackageNativeLibs2AssetTransform
import xyz.yorek.plugin.mt.visitor.BaseClassVisitor
import xyz.yorek.plugin.mt.visitor.CodeScanVisitor
import xyz.yorek.plugin.mt.visitor.MethodProxyVisitor
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import xyz.yorek.plugin.mt.transform.MethodTraceTransform

/**
 * Created by yorek.liu on 2021/8/12
 *
 * @author yorek.liu
 */
class MethodTracePlugin implements Plugin<Project> {
    private static final String TAG = "MethodTracePlugin"

    @Override
    void apply(Project project) {
        project.extensions.create("methodTrace", MethodTraceExtension)

        //osdetector change its plugin name in 1.4.0
        try {
            project.apply plugin: 'osdetector'
        } catch (Throwable e) {
            project.apply plugin: 'com.google.osdetector'
        }

        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new GradleException('MethodTracePlugin Plugin, Android Application plugin required')
        }

        project.afterEvaluate {
            def android = project.extensions.android
            def configuration = project.methodTrace
            android.applicationVariants.all { variant ->
                if (configuration.enable) {
                    MethodTraceTransform.inject(project, configuration, variant, getVisitorList())
                }
            }
        }

        project.extensions.getByType(AppExtension).registerTransform(new PackageNativeLibs2AssetTransform(project))
    }

    private static List<Class<BaseClassVisitor>> getVisitorList() {
        List<Class<BaseClassVisitor>> visitorList = new ArrayList<>()
        visitorList.add(CodeScanVisitor.class)
//        visitorList.add(BitmapDetectClassVisitor.class)
//        visitorList.add(BitmapDecodeClassVisitor.class)
        visitorList.add(MethodProxyVisitor.class)
        return visitorList
    }
}
