package xyz.yorek.plugin.mt.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformTask
import xyz.yorek.plugin.mt.graph.ClassGraphBuilder
import xyz.yorek.plugin.mt.model.Configuration
import xyz.yorek.plugin.mt.model.FirstTraceContext
import xyz.yorek.plugin.mt.util.Util
import xyz.yorek.plugin.mt.visitor.BaseClassVisitor
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener
import xyz.yorek.plugin.mt.MethodTraceExtension
import xyz.yorek.plugin.mt.Log
import xyz.yorek.plugin.mt.MethodTracer
import xyz.yorek.plugin.mt.PreMethodTracer
import xyz.yorek.plugin.mt.ResultWriter
import xyz.yorek.plugin.mt.TransformContext

import javax.xml.crypto.dsig.TransformException
import java.lang.reflect.Field

class MethodTraceTransform extends ProxyTransformWrapper {

    Transform originalTransform
    Project project
    MethodTraceExtension extension
    Configuration configuration
    def variant
    List<Class<BaseClassVisitor>> visitorList

    MethodTraceTransform(Project project, MethodTraceExtension extension, def variant, Transform originalTransform, List<Class<BaseClassVisitor>> visitorList) {
        super(originalTransform)
        this.originalTransform = originalTransform
        this.variant = variant
        this.project = project
        this.extension = extension
        this.visitorList = visitorList
        configuration = new Configuration()
    }

    static void inject(Project project, MethodTraceExtension configuration, def variant, List<Class<BaseClassVisitor>> visitorList) {
        String hackTransformTaskName = getTransformTaskName("", "", variant.name)
        String hackTransformTaskNameForWrapper = getTransformTaskName("", "Builder", variant.name)

        project.logger.info("prepare inject dex transform: " + hackTransformTaskName +", hackTransformTaskNameForWrapper: "+hackTransformTaskNameForWrapper)

        project.getGradle().getTaskGraph().addTaskExecutionGraphListener(new TaskExecutionGraphListener() {
            @Override
            void graphPopulated(TaskExecutionGraph taskGraph) {
                for (Task task : taskGraph.getAllTasks()) {
                    if ((task.name.equalsIgnoreCase(hackTransformTaskName) || task.name.equalsIgnoreCase(hackTransformTaskNameForWrapper))
                            && !(((TransformTask) task).getTransform() instanceof MethodTraceTransform)) {
                        project.logger.warn("find dex transform. transform class: " + task.transform.getClass() + " . task name: " + task.name)
                        project.logger.info("variant name: " + variant.name)
                        Field field = TransformTask.class.getDeclaredField("transform")
                        field.setAccessible(true)
                        field.set(task, new MethodTraceTransform(project, configuration, variant, task.transform, visitorList))
                        project.logger.warn("transform class after hook: " + task.transform.getClass())
                        break
                    }
                }
            }
        })
    }

    @Override
    String getName() {
        return "MethodTraceTransform"
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        long start = System.currentTimeMillis()
        final boolean isIncremental = transformInvocation.isIncremental() && this.isIncremental()
        final File asmOutput = new File(project.getBuildDir().getAbsolutePath() + File.separator + "asmoutput")
        final File rootOutput = new File(asmOutput, "classes/${getName()}/")

        if (Util.isNullOrNil(extension.output)) {
            configuration.output = new File(asmOutput, "result.txt").absolutePath
        }
        if (!extension.apiList.isEmpty()) {
            configuration.apiList = extension.apiList
        } else {
            configuration.apiList = Collections.emptyMap()
        }

        if (!rootOutput.exists()) {
            rootOutput.mkdirs()
        }

        Map<File, File> jarInputMap = new HashMap<>()
        Map<File, File> scrInputMap = new HashMap<>()

        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput ->
                collectAndIdentifyDir(scrInputMap, dirInput, rootOutput, isIncremental)
            }
            input.jarInputs.each { JarInput jarInput ->
                if (jarInput.getStatus() != Status.REMOVED) {
                    collectAndIdentifyJar(jarInputMap, scrInputMap, jarInput, rootOutput, isIncremental)
                }
            }
        }

        // build class graph
        long begin = System.currentTimeMillis()
        ClassGraphBuilder classGraphBuilder = new ClassGraphBuilder()
        FirstTraceContext firstTraceBridge = new FirstTraceContext()
        classGraphBuilder.build(scrInputMap.keySet(), jarInputMap.keySet(), firstTraceBridge)
        TransformContext context = new TransformContext(configuration, firstTraceBridge, visitorList)
        Log.i("ASM." + getName(), "[build class graph] cost time: %dms", System.currentTimeMillis() - begin)

        // pre trace
        new PreMethodTracer(context).execute()

        // trace
        MethodTracer methodTracer = new MethodTracer(context)
        methodTracer.trace(scrInputMap, jarInputMap)

        // write result to file
        try {
            ResultWriter.write(configuration)
        } catch (Exception e) {
            Log.e("ASM." + getName(), "CodeScanner write result file error: %s", e.toString())
        }
        Log.i("ASM." + getName(), "[plugin self] cost time: %dms", System.currentTimeMillis() - begin)

        originalTransform.transform(transformInvocation)
        Log.i("ASM." + getName(), "[transform] cost time: %dms", System.currentTimeMillis() - start)
    }

    private static void collectAndIdentifyDir(Map<File, File> dirInputMap, DirectoryInput input, File rootOutput, boolean isIncremental) {
        final File dirInput = input.file
        final File dirOutput = new File(rootOutput, input.file.getName())
        if (!dirOutput.exists()) {
            dirOutput.mkdirs()
        }
        if (isIncremental) {
            if (!dirInput.exists()) {
                dirOutput.deleteDir()
            } else {
                final Map<File, Status> obfuscatedChangedFiles = new HashMap<>()
                final String rootInputFullPath = dirInput.getAbsolutePath()
                final String rootOutputFullPath = dirOutput.getAbsolutePath()
                input.changedFiles.each { Map.Entry<File, Status> entry ->
                    final File changedFileInput = entry.getKey()
                    final String changedFileInputFullPath = changedFileInput.getAbsolutePath()
                    final File changedFileOutput = new File(
                            changedFileInputFullPath.replace(rootInputFullPath, rootOutputFullPath)
                    )
                    final Status status = entry.getValue()
                    switch (status) {
                        case Status.NOTCHANGED:
                            break
                        case Status.ADDED:
                        case Status.CHANGED:
                            dirInputMap.put(changedFileInput, changedFileOutput)
                            break
                        case Status.REMOVED:
                            changedFileOutput.delete()
                            break
                    }
                    obfuscatedChangedFiles.put(changedFileOutput, status)
                }
                replaceChangedFile(input, obfuscatedChangedFiles)
            }
        } else {
            dirInputMap.put(dirInput, dirOutput)
        }
        replaceFile(input, dirOutput)
    }

    private void collectAndIdentifyJar(Map<File, File> jarInputMaps, Map<File, File> dirInputMaps, JarInput input, File rootOutput, boolean isIncremental) {
        final File jarInput = input.file
        final File jarOutput = new File(rootOutput, getUniqueJarName(jarInput))
        if (Util.isRealZipOrJar(jarInput)) {
            switch (input.status) {
                case Status.NOTCHANGED:
                    if (isIncremental) {
                        break
                    }
                case Status.ADDED:
                case Status.CHANGED:
                    jarInputMaps.put(jarInput, jarOutput)
                    break
                case Status.REMOVED:
                    break
            }
        } else {
            // Special case for WeChat AutoDex. Its rootInput jar file is actually
            // a txt file contains path list.
            BufferedReader br = null
            BufferedWriter bw = null
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(jarInput)))
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(jarOutput)))
                String realJarInputFullPath
                while ((realJarInputFullPath = br.readLine()) != null) {
                    // src jar.
                    final File realJarInput = new File(realJarInputFullPath)
                    // dest jar, moved to extraguard intermediate output dir.
                    File realJarOutput = new File(rootOutput, getUniqueJarName(realJarInput))

                    if (realJarInput.exists() && Util.isRealZipOrJar(realJarInput)) {
                        jarInputMaps.put(realJarInput, realJarOutput)
                    } else {
                        realJarOutput.delete()
                        if (realJarInput.exists() && realJarInput.isDirectory()) {
                            realJarOutput = new File(rootOutput, realJarInput.getName())
                            if (!realJarOutput.exists()) {
                                realJarOutput.mkdirs()
                            }
                            dirInputMaps.put(realJarInput, realJarOutput)
                        }

                    }
                    // write real output full path to the fake jar at rootOutput.
                    final String realJarOutputFullPath = realJarOutput.getAbsolutePath()
                    bw.writeLine(realJarOutputFullPath)
                }
            } catch (FileNotFoundException e) {
                Log.e("ASM." + getName(), "FileNotFoundException:%s", e.toString())
            } finally {
                Util.closeQuietly(br)
                Util.closeQuietly(bw)
            }
            jarInput.delete() // delete raw inputList
        }

        replaceFile(input, jarOutput)
    }

    static private String getTransformTaskName(String customDexTransformName, String wrapperSuffix, String buildTypeSuffix) {
        if (customDexTransformName != null && customDexTransformName.length() > 0) {
            return customDexTransformName+"For${buildTypeSuffix}"
        }
        return "transformClassesWithDex${wrapperSuffix}For${buildTypeSuffix}"
    }
}
