package xyz.yorek.plugin.mt.transform

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import xyz.yorek.plugin.mt.Log
import xyz.yorek.plugin.mt.util.Util

class PackageNativeLibs2AssetTransform extends Transform {

    final Project mProject
    final Transform mOriginalTransform

    PackageNativeLibs2AssetTransform(Project project, Transform original) {
        mProject = project
        mOriginalTransform = original
    }

    @Override
    String getName() {
        return "PackageNativeLibs2AssetTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return mOriginalTransform == null ? TransformManager.CONTENT_NATIVE_LIBS : mOriginalTransform.inputTypes
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return mOriginalTransform == null ? TransformManager.SCOPE_FULL_PROJECT : mOriginalTransform.scopes
    }

    @Override
    boolean isIncremental() {
        // TODO
//        return mOriginalTransform == null ? true : mOriginalTransform.scopes
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        String inputPath
        if (mOriginalTransform != null) {
            mOriginalTransform.transform(transformInvocation)
            inputPath = "${mProject.getBuildDir().getAbsolutePath()}/intermediates/transforms/stripDebugSymbol/${transformInvocation.context.variantName}/0"
        } else {
            inputPath = "${mProject.getBuildDir().getAbsolutePath()}/intermediates/transforms/mergeJniLibs/${transformInvocation.context.variantName}/0"
        }

        def abiFilters = mProject.extensions.getByType(AppExtension).defaultConfig.ndk.abiFilters
        Log.v(getName(), "found abiFilters=$abiFilters")
        File inputFile = new File(inputPath)
        File outputFile = new File("${inputPath}_filter")
        collectFile(inputFile, abiFilters, outputFile)
        String _7zip = resolve7ZipPath()
        File assetFile = new File("${mProject.getBuildDir().getAbsolutePath()}/intermediates/merged_assets/${transformInvocation.context.variantName}/out/applibs")
        Util.sevenZipInputDir(outputFile, assetFile, _7zip)

        inputFile.deleteDir()

//        transformInvocation.inputs.forEach { transformInput ->
//            transformInput.directoryInputs.forEach { directoryInput ->
//                Log.v(getName(), "dir: ${directoryInput.file.absolutePath}")
//                File outputFile = new File("${directoryInput.file.absolutePath}_filter")
//                collectFile(directoryInput.file, abiFilters, outputFile)
//                String _7zip = resolve7ZipPath()
//                File assetFile = new File("${mProject.getBuildDir().getAbsolutePath()}/intermediates/merged_assets/${transformInvocation.context.variantName}/out/applibs")
//                Util.sevenZipInputDir(outputFile, assetFile, _7zip)
//                File dest = transformInvocation.outputProvider.getContentLocation(
//                        directoryInput.name,
//                        directoryInput.contentTypes,
//                        directoryInput.scopes,
//                        Format.DIRECTORY
//                )
//                FileUtils.copyDirectory(directoryInput.file, dest)
//            }
//            transformInput.jarInputs.forEach { jarInput ->
//                Log.v(getName(), "jar: ${jarInput.file.absolutePath}")
//                File dest = transformInvocation.outputProvider.getContentLocation(
//                        jarInput.name,
//                        jarInput.contentTypes,
//                        jarInput.scopes,
//                        Format.JAR
//                )
//                FileUtils.copyDirectory(jarInput.file, dest)
//            }
//        }
    }

    private static void collectFile(File inputFile, Set<String> abiFilters, File outputFile) {
        File libDir = new File(inputFile, "lib")
        File outputLibDir = new File(outputFile, "lib")
        if (libDir.exists() && libDir.isDirectory()) {
            // pick with abiFilters
            File[] abiDirs = libDir.listFiles()
            for (File singleAbiDir : abiDirs) {
                if (abiFilters == null || abiFilters.contains(singleAbiDir.name)) {
                    // copy dir
                    for (File singleAbiSoFile : singleAbiDir.listFiles()) {
                        File outputSingleAbiSoFile = new File(outputLibDir.absolutePath + File.separator + singleAbiDir.name + File.separator + singleAbiSoFile.name)
                        Util.copyFileUsingStream(singleAbiSoFile, outputSingleAbiSoFile)
                    }
//                    singleAbiDir.deleteDir()
                }
            }
        }
    }

    private String resolve7ZipPath() {
        Configuration config = mProject.configurations.create("sevenZipToolsLocator") {
            visible = false
            transitive = false
            extendsFrom = []
        }
        def notation = [
                group    : "com.tencent.mm",
                name     : "SevenZip",
                version  : "1.2.20",
                classifier: mProject.osdetector.classifier,
                ext       : 'exe'
        ]
        println "Resolving artifact: ${notation}"
        Dependency dependency = mProject.dependencies.add(config.name, notation)
        File file = config.fileCollection(dependency).singleFile
        if (!file.canExecute() && !file.setExecutable(true)) {
            throw new GradleException("Cannot set ${file} as executable")
        }
        return file.path
    }
}