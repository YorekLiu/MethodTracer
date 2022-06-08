package xyz.yorek.plugin.mt.graph;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import xyz.yorek.plugin.mt.ASMEntry;
import xyz.yorek.plugin.mt.Log;
import xyz.yorek.plugin.mt.MethodTracer;
import xyz.yorek.plugin.mt.model.FirstTraceContext;
import xyz.yorek.plugin.mt.util.Util;
import xyz.yorek.plugin.mt.visitor.ParseProxyEntryVisitor;

/**
 * Created by yorek.liu on 2021/8/2
 *
 * @author yorek.liu
 */
public class ClassGraphBuilder {

    private static final String TAG = "ClassGraphBuilder";

    public void build(Collection<File> srcFolderList, Collection<File> dependencyJarList, FirstTraceContext firstTraceContext, ExecutorService executor) throws ExecutionException, InterruptedException {
        List<Future<?>> futures = new LinkedList<>();
        parseAnnotationFromSrc(srcFolderList, firstTraceContext, futures, executor);
        parseAnnotationFromJar(dependencyJarList, firstTraceContext, futures, executor);

        for (Future<?> future : futures) {
            future.get();
        }
        futures.clear();
    }

    private void parseAnnotationFromSrc(Collection<File> srcFiles, FirstTraceContext firstTraceContext, List<Future<?>> futures, ExecutorService executor) {
        if (null != srcFiles) {
            for (File file : srcFiles) {
                futures.add(executor.submit(() -> innerParseAnnotationFromSrc(file, firstTraceContext)));
            }
        }
    }

    private void parseAnnotationFromJar(Collection<File> dependencyFiles, FirstTraceContext firstTraceContext, List<Future<?>> futures, ExecutorService executor) {
        if (null != dependencyFiles) {
            for (File file : dependencyFiles) {
                futures.add(executor.submit(() -> innerParseAnnotationFromJar(file, firstTraceContext)));
            }
        }
    }

    private void innerParseAnnotationFromSrc(File input, FirstTraceContext firstTraceContext) {
        ArrayList<File> classFileList = new ArrayList<>();
        if (input.isDirectory()) {
            MethodTracer.listClassFiles(classFileList, input);
        } else {
            classFileList.add(input);
        }

        for (File classFile : classFileList) {
            InputStream is = null;
            try {
                if (MethodTracer.isNeedTraceClass(classFile.getName())) {
                    is = new FileInputStream(classFile);
                    buildClassGraph(is, firstTraceContext);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Util.closeQuietly(is);
            }
        }
    }

    private void innerParseAnnotationFromJar(File input, FirstTraceContext firstTraceContext) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(input);
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                String zipEntryName = zipEntry.getName();

                if (MethodTracer.isNeedTraceClass(zipEntryName)) {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    buildClassGraph(inputStream, firstTraceContext);
                    Util.closeQuietly(inputStream);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "[innerParseAnnotationFromJar] err! %s", input.getAbsolutePath());
        } finally {
            try {
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "close stream err!");
            }
        }
    }

    public static void buildClassGraph(InputStream is, FirstTraceContext firstTraceContext) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor classVisitor = new ClassGraphVisitor(ASMEntry.ASM_VERSION, classWriter, firstTraceContext.classGraph);
        ClassVisitor methodTraceAnnotationVisitor = new ParseProxyEntryVisitor(ASMEntry.ASM_VERSION, classVisitor, firstTraceContext);
        ClassReader classReader = new ClassReader(is);
        classReader.accept(methodTraceAnnotationVisitor, ClassReader.SKIP_CODE);
    }
}
