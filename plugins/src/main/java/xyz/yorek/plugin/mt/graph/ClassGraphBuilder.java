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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import xyz.yorek.plugin.mt.ASMEntry;
import xyz.yorek.plugin.mt.Log;
import xyz.yorek.plugin.mt.model.FirstTraceContext;
import xyz.yorek.plugin.mt.util.Util;
import xyz.yorek.plugin.mt.visitor.MethodTraceAnnotationVisitor;

/**
 * Created by yorek.liu on 2021/8/2
 *
 * @author yorek.liu
 */
public class ClassGraphBuilder {

    private static final String TAG = "ClassGraphBuilder";

    public void build(Collection<File> srcFolderList, Collection<File> dependencyJarList, FirstTraceContext firstTraceContext) {
        traceMethodFromSrc(srcFolderList, firstTraceContext);
        traceMethodFromJar(dependencyJarList, firstTraceContext);
    }

    private void traceMethodFromSrc(Collection<File> srcFiles, FirstTraceContext firstTraceContext) {
        if (null != srcFiles) {
            for (File file : srcFiles) {
                innerTraceMethodFromSrc(file, firstTraceContext);
            }
        }
    }

    private void traceMethodFromJar(Collection<File> dependencyFiles, FirstTraceContext firstTraceContext) {
        if (null != dependencyFiles) {
            for (File file : dependencyFiles) {
                innerTraceMethodFromJar(file, firstTraceContext);
            }
        }
    }

    private void innerTraceMethodFromSrc(File input, FirstTraceContext firstTraceContext) {
        ArrayList<File> classFileList = new ArrayList<>();
        if (input.isDirectory()) {
            listClassFiles(classFileList, input);
        } else {
            classFileList.add(input);
        }

        for (File classFile : classFileList) {
            InputStream is = null;
            try {
                if (isNeedTraceClass(classFile.getName())) {
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

    private void innerTraceMethodFromJar(File input, FirstTraceContext firstTraceContext) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(input);
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                String zipEntryName = zipEntry.getName();

                if (isNeedTraceClass(zipEntryName)) {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    buildClassGraph(inputStream, firstTraceContext);
                    Util.closeQuietly(inputStream);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "[traceMethodFromJar] err! %s", input.getAbsolutePath());
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

    private void listClassFiles(ArrayList<File> classFiles, File folder) {
        File[] files = folder.listFiles();
        if (null == files) {
            Log.e(TAG, "[listClassFiles] files is null! %s", folder.getAbsolutePath());
            return;
        }
        for (File file : files) {
            if (file == null) {
                continue;
            }
            if (file.isDirectory()) {
                listClassFiles(classFiles, file);
            } else {
                if (file.isFile()) {
                    classFiles.add(file);
                }

            }
        }
    }

    public boolean isNeedTraceClass(String fileName) {
        return fileName.endsWith(".class");
    }

    public static void buildClassGraph(InputStream is, FirstTraceContext firstTraceContext) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor classVisitor = new ClassGraphVisitor(ASMEntry.ASM_VERSION, classWriter, firstTraceContext.classGraph);
        ClassVisitor methodTraceAnnotationVisitor = new MethodTraceAnnotationVisitor(ASMEntry.ASM_VERSION, classVisitor, firstTraceContext);
        ClassReader classReader = new ClassReader(is);
        classReader.accept(methodTraceAnnotationVisitor, ClassReader.SKIP_CODE);
    }
}
