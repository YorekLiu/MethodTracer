package xyz.yorek.plugin.mt.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.builder.model.AndroidProject.FD_OUTPUTS
import com.android.utils.FileUtils
import com.google.common.base.Joiner
import com.google.common.hash.Hashing
import org.gradle.api.Project
import xyz.yorek.plugin.mt.*
import xyz.yorek.plugin.mt.graph.ClassGraphBuilder
import xyz.yorek.plugin.mt.model.Configuration
import xyz.yorek.plugin.mt.model.FirstTraceContext
import xyz.yorek.plugin.mt.util.Util
import xyz.yorek.plugin.mt.visitor.BaseClassVisitor
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class SingleTransform(
    private val project: Project,
    private val extension: MethodTraceExtension,
    private val visitorList: List<Class<out BaseClassVisitor>>,
    private val configuration: Configuration = Configuration()
) : Transform() {

    private companion object {
        private const val TAG = "SingleTransform"

        @Suppress("DEPRECATION", "UnstableApiUsage")
        fun getUniqueJarName(jarFile: File): String {
            val origJarName = jarFile.name
            val hashing = Hashing.sha1().hashString(jarFile.path, Charsets.UTF_16LE).toString()
            val dotPos = origJarName.lastIndexOf('.')
            return if (dotPos < 0) {
                String.format("%s_%s", origJarName, hashing)
            } else {
                val nameWithoutDotExt = origJarName.substring(0, dotPos)
                val dotExt = origJarName.substring(dotPos)
                String.format("%s_%s%s", nameWithoutDotExt, hashing, dotExt)
            }
        }

        fun appendSuffix(jarFile: File, suffix: String): String {
            val origJarName = jarFile.name
            val dotPos = origJarName.lastIndexOf('.')
            return if (dotPos < 0) {
                String.format("%s_%s", origJarName, suffix)
            } else {
                val nameWithoutDotExt = origJarName.substring(0, dotPos)
                val dotExt = origJarName.substring(dotPos)
                String.format("%s_%s%s", nameWithoutDotExt, suffix, dotExt)
            }
        }
    }

    override fun getName(): String = TAG

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun isIncremental(): Boolean = true

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)

        val outputProvider = transformInvocation.outputProvider!!
        val isIncremental = transformInvocation.isIncremental && this.isIncremental

        if (!isIncremental) {
            outputProvider.deleteAll()
        }

        val changedFiles = ConcurrentHashMap<File, Status>()
        val inputToOutput = ConcurrentHashMap<File, File>()
        val inputFiles = ArrayList<File>()
        val transformDirectory: File? = collectFiles(
            transformInvocation,
            changedFiles,
            inputFiles,
            outputProvider,
            inputToOutput
        )

        if (inputFiles.size == 0 || transformDirectory == null) {
            Log.i(TAG, "do not find any input files")
            return
        }

        initConfig(transformInvocation)

        doTransform(
            classInputs = inputFiles,
            changedFiles = changedFiles,
            isIncremental = isIncremental,
            traceClassDirectoryOutput = transformDirectory,
            inputToOutput = inputToOutput,
            uniqueOutputName = true
        )
    }

    private fun collectFiles(
        transformInvocation: TransformInvocation,
        changedFiles: ConcurrentHashMap<File, Status>,
        inputFiles: ArrayList<File>,
        outputProvider: TransformOutputProvider,
        inputToOutput: ConcurrentHashMap<File, File>
    ): File? {
        var transformDirectory: File? = null

        for (input in transformInvocation.inputs) {
            for (directoryInput in input.directoryInputs) {
                changedFiles.putAll(directoryInput.changedFiles)
                val inputDir = directoryInput.file
                inputFiles.add(inputDir)
                val outputDirectory = outputProvider.getContentLocation(
                    directoryInput.name,
                    directoryInput.contentTypes,
                    directoryInput.scopes,
                    Format.DIRECTORY
                )

                inputToOutput[inputDir] = outputDirectory
                if (transformDirectory == null) transformDirectory = outputDirectory.parentFile
            }

            for (jarInput in input.jarInputs) {
                val inputFile = jarInput.file
                changedFiles[inputFile] = jarInput.status
                inputFiles.add(inputFile)
                val outputJar = outputProvider.getContentLocation(
                    jarInput.name,
                    jarInput.contentTypes,
                    jarInput.scopes,
                    Format.JAR
                )

                inputToOutput[inputFile] = outputJar
                if (transformDirectory == null) transformDirectory = outputJar.parentFile
            }
        }
        return transformDirectory
    }

    private fun initConfig(transformInvocation: TransformInvocation) {
        val buildDir = project.buildDir.absolutePath
        val dirName = transformInvocation.context.variantName
        val reportOutDir = Joiner.on(File.separatorChar).join(buildDir, FD_OUTPUTS, "report")
        if (extension.output.isEmpty()) {
            configuration.output = File(reportOutDir, "report_${dirName}_${System.currentTimeMillis()}.txt").absolutePath
        } else {
            configuration.output = extension.output
        }
        val reportOutputFile = File(configuration.output)
        reportOutputFile.parentFile?.mkdirs()

        if (extension.apiList.isNotEmpty()) {
            configuration.apiList = extension.apiList
        } else {
            configuration.apiList = emptyMap()
        }

        // convert className to internalName
        // xyz.yorek.plugin.sample.MainActivity$InnerClassTest -> xyz/yorek/plugin/sample/MainActivity$InnerClassTest
        val internalNameApiList = mutableMapOf<String, List<String>>()
        for (entry in configuration.apiList) {
            val internalName = entry.key.replace('.', '/')
            internalNameApiList[internalName] = entry.value
        }
        configuration.apiList = internalNameApiList
    }

    @Suppress("SameParameterValue")
    private fun doTransform(classInputs: Collection<File>,
                            changedFiles: Map<File, Status>,
                            inputToOutput: Map<File, File>,
                            isIncremental: Boolean,
                            traceClassDirectoryOutput: File,
                            uniqueOutputName: Boolean
    ) {
        val executor: ExecutorService = Executors.newFixedThreadPool(16)

        /**
         * step 1
         */
        var start = System.currentTimeMillis()
        val futures = LinkedList<Future<*>>()
        val dirInputOutMap = ConcurrentHashMap<File, File>()
        val jarInputOutMap = ConcurrentHashMap<File, File>()

        for (file in classInputs) {
            if (file.isDirectory) {
                futures.add(executor.submit(CollectDirectoryInputTask(
                    directoryInput = file,
                    mapOfChangedFiles = changedFiles,
                    mapOfInputToOutput = inputToOutput,
                    isIncremental = isIncremental,
                    traceClassDirectoryOutput = traceClassDirectoryOutput,
                    // result
                    resultOfDirInputToOut = dirInputOutMap
                )))
            } else {
                val status = Status.CHANGED
                futures.add(executor.submit(CollectJarInputTask(
                    inputJar = file,
                    inputJarStatus = status,
                    inputToOutput = inputToOutput,
                    isIncremental = isIncremental,
                    traceClassFileOutput = traceClassDirectoryOutput,
                    uniqueOutputName = uniqueOutputName,
                    // result
                    resultOfDirInputToOut = dirInputOutMap,
                    resultOfJarInputToOut = jarInputOutMap
                )))
            }
        }

        for (future in futures) {
            future.get()
        }
        futures.clear()

        Log.i(TAG, "[doTransform] Step(1)[Parse]... cost:%sms", System.currentTimeMillis() - start)

        // build class graph
        start = System.currentTimeMillis()
        val classGraphBuilder = ClassGraphBuilder()
        val firstTraceBridge = FirstTraceContext()
        classGraphBuilder.build(dirInputOutMap.keys, jarInputOutMap.keys, firstTraceBridge, executor)
        val context = TransformContext(configuration, firstTraceBridge, visitorList, executor)
        Log.i("ASM.${name}", "[build class graph] cost time: %dms", System.currentTimeMillis() - start)

        // pre trace
        PreMethodTracer(context).execute()

        // trace
        val methodTracer = MethodTracer(context)
        methodTracer.trace(dirInputOutMap, jarInputOutMap)

        // write result to file
        try {
            ResultWriter.write(configuration)
        } catch (e: Exception) {
            Log.e("ASM.${name}", "CodeScanner write result file error: %s", e.toString())
        }
        Log.i("ASM.${name}", "[transform] cost time: %dms", System.currentTimeMillis() - start)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    class CollectDirectoryInputTask(
        private val directoryInput: File,
        private val mapOfChangedFiles: Map<File, Status>,
        private val mapOfInputToOutput: Map<File, File>,
        private val isIncremental: Boolean,
        private val traceClassDirectoryOutput: File,
        private val resultOfDirInputToOut: MutableMap<File, File>
    ) : Runnable {

        override fun run() {
            try {
                handle()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "%s", e.toString())
            }
        }

        private fun handle() {
            val dirInput = directoryInput
            val dirOutput = if (mapOfInputToOutput.containsKey(dirInput)) {
                mapOfInputToOutput[dirInput]!!
            } else {
                File(traceClassDirectoryOutput, dirInput.name)
            }
            val inputFullPath = dirInput.absolutePath
            val outputFullPath = dirOutput.absolutePath

            if (!dirOutput.exists()) {
                dirOutput.mkdirs()
            }

            if (!dirInput.exists() && dirOutput.exists()) {
                if (dirOutput.isDirectory) {
                    FileUtils.deletePath(dirOutput)
                } else {
                    FileUtils.delete(dirOutput)
                }
            }

            if (isIncremental) {
                val outChangedFiles = HashMap<File, Status>()

                for ((changedFileInput, status) in mapOfChangedFiles) {
                    val changedFileInputFullPath = changedFileInput.absolutePath

                    // mapOfChangedFiles is contains all. each collectDirectoryInputTask should handle itself, should not handle other file
                    if (!changedFileInputFullPath.contains(inputFullPath)) {
                        continue
                    }

                    val changedFileOutput = File(changedFileInputFullPath.replace(inputFullPath, outputFullPath))

                    if (status == Status.ADDED || status == Status.CHANGED) {
                        resultOfDirInputToOut[changedFileInput] = changedFileOutput
                    } else if (status == Status.REMOVED) {
                        changedFileOutput.delete()
                    }
                    outChangedFiles[changedFileOutput] = status
                }
            } else {
                resultOfDirInputToOut[dirInput] = dirOutput
            }
        }
    }

    class CollectJarInputTask(
        private val inputJar: File,
        private val inputJarStatus: Status,
        private val inputToOutput: Map<File, File>,
        private val isIncremental: Boolean,
        private val traceClassFileOutput: File,
        private val uniqueOutputName: Boolean,
        private val resultOfDirInputToOut: MutableMap<File, File>,
        private val resultOfJarInputToOut: MutableMap<File, File>
    ) : Runnable {

        override fun run() {
            try {
                handle()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "%s", e.toString())
            }
        }

        private fun handle() {

            val jarInput = inputJar
            val jarOutput = if (inputToOutput.containsKey(jarInput)) {
                inputToOutput[jarInput]!!
            } else {
                val outputJarName = if (uniqueOutputName)
                    getUniqueJarName(jarInput)
                else
                    appendSuffix(jarInput, "traced")
                File(traceClassFileOutput, outputJarName)
            }

//            Log.d(TAG, "CollectJarInputTask input %s -> output %s", jarInput, jarOutput)

            if (!isIncremental && jarOutput.exists()) {
                jarOutput.delete()
            }
            if (!jarOutput.parentFile.exists()) {
                jarOutput.parentFile.mkdirs()
            }

            if (Util.isRealZipOrJar(jarInput)) {
                if (isIncremental) {
                    if (inputJarStatus == Status.ADDED || inputJarStatus == Status.CHANGED) {
                        resultOfJarInputToOut[jarInput] = jarOutput
                    } else if (inputJarStatus == Status.REMOVED) {
                        jarOutput.delete()
                    }
                } else {
                    resultOfJarInputToOut[jarInput] = jarOutput
                }
            } else {
                // TODO for wechat
                Log.i(TAG, "Special case for WeChat AutoDex. Its rootInput jar file is actually a txt file contains path list.")
                // Special case for WeChat AutoDex. Its rootInput jar file is actually
                // a txt file contains path list.
                jarInput.inputStream().bufferedReader().useLines { lines ->
                    lines.forEach { realJarInputFullPath ->
                        val realJarInput = File(realJarInputFullPath)
                        // dest jar, moved to extra guard intermediate output dir.
                        val realJarOutput = File(traceClassFileOutput, getUniqueJarName(realJarInput))

                        if (realJarInput.exists() && Util.isRealZipOrJar(realJarInput)) {
                            resultOfJarInputToOut[realJarInput] = realJarOutput
                        } else {
                            realJarOutput.delete()
                            if (realJarInput.exists() && realJarInput.isDirectory) {
                                val realJarOutputDir = File(traceClassFileOutput, realJarInput.name)
                                if (!realJarOutput.exists()) {
                                    realJarOutput.mkdirs()
                                }
                                resultOfDirInputToOut[realJarInput] = realJarOutputDir
                            }

                        }
                        // write real output full path to the fake jar at rootOutput.
                        jarOutput.outputStream().bufferedWriter().use { bw ->
                            bw.write(realJarOutput.absolutePath)
                            bw.newLine()
                        }
                    }
                }

                jarInput.delete() // delete raw inputList
            }
        }
    }
}