package xyz.yorek.plugin.mt;

import org.objectweb.asm.ClassWriter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import xyz.yorek.plugin.mt.util.Util;

public class MethodTracer {

    private static final String TAG = "MethodTracer";
    private final TransformContext context;

    public MethodTracer(TransformContext context) {
        this.context = context;
    }

    public void trace(Map<File, File> srcFolderList, Map<File, File> dependencyJarList) throws ExecutionException, InterruptedException {
        List<Future<?>> futures = new LinkedList<>();
        traceMethodFromSrc(srcFolderList, futures);
        traceMethodFromJar(dependencyJarList, futures);

        for (Future<?> future : futures) {
            future.get();
        }
        futures.clear();
    }

    private void traceMethodFromSrc(Map<File, File> srcMap, List<Future<?>> futures) {
        if (null != srcMap) {
            for (Map.Entry<File, File> entry : srcMap.entrySet()) {
                futures.add(context.executor.submit(() -> innerTraceMethodFromSrc(entry.getKey(), entry.getValue())));
            }
        }
    }

    private void traceMethodFromJar(Map<File, File> dependencyMap, List<Future<?>> futures) {
        if (null != dependencyMap) {
            for (Map.Entry<File, File> entry : dependencyMap.entrySet()) {
                futures.add(context.executor.submit(() -> innerTraceMethodFromJar(entry.getKey(), entry.getValue())));
            }
        }
    }

    private void innerTraceMethodFromSrc(File input, File output) {
        ArrayList<File> classFileList = new ArrayList<>();
        if (input.isDirectory()) {
            listClassFiles(classFileList, input);
        } else {
            classFileList.add(input);
        }

        for (File classFile : classFileList) {
            InputStream is = null;
            FileOutputStream os = null;
            try {
                final String changedFileInputFullPath = classFile.getAbsolutePath();
                final File changedFileOutput = new File(
                        changedFileInputFullPath.replace(input.getAbsolutePath(), output.getAbsolutePath())
                );
                if (!changedFileOutput.exists()) {
                    changedFileOutput.getParentFile().mkdirs();
                }
                changedFileOutput.createNewFile();

                if (isNeedTraceClass(classFile.getName())) {
                    is = new FileInputStream(classFile);

                    ClassWriter classWriter = ASMEntry.run(is, context);
                    is.close();

                    if (output.isDirectory()) {
                        os = new FileOutputStream(changedFileOutput);
                    } else {
                        os = new FileOutputStream(output);
                    }
                    os.write(classWriter.toByteArray());
                    os.close();
                } else {
                    Util.copyFileUsingStream(classFile, changedFileOutput);
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    Files.copy(input.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } finally {
                Util.closeQuietly(is);
                Util.closeQuietly(os);
            }
        }
    }

    private void innerTraceMethodFromJar(File input, File output) {
        ZipOutputStream zipOutputStream = null;
        ZipFile zipFile = null;
        try {
            zipOutputStream = new ZipOutputStream(new FileOutputStream(output));
            zipFile = new ZipFile(input);
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                String zipEntryName = zipEntry.getName();

                if (isNeedTraceClass(zipEntryName)) {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    ClassWriter classWriter = ASMEntry.run(inputStream, context);
                    byte[] data = classWriter.toByteArray();
                    InputStream byteArrayInputStream = new ByteArrayInputStream(data);
                    ZipEntry newZipEntry = new ZipEntry(zipEntryName);
                    Util.addZipEntry(zipOutputStream, newZipEntry, byteArrayInputStream);
                } else {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    ZipEntry newZipEntry = new ZipEntry(zipEntryName);
                    Util.addZipEntry(zipOutputStream, newZipEntry, inputStream);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "[innerTraceMethodFromJar] err! %s", output.getAbsolutePath());
            if (e instanceof ZipException) {
                e.printStackTrace();
            }
            try {
                if (input.length() > 0) {
                    Files.copy(input.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Log.e(TAG, "[innerTraceMethodFromJar] input:%s is empty", input);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                if (zipOutputStream != null) {
                    zipOutputStream.finish();
                    zipOutputStream.flush();
                    zipOutputStream.close();
                }
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "close stream err!");
            }
        }
    }

    public static void listClassFiles(ArrayList<File> classFiles, File folder) {
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
            } else if (isNeedTraceClass(file.getName())) {
                classFiles.add(file);
            }
        }
    }

    private static final String[] UN_TRACE_CLASS = {"R.class", "R$", "Manifest", "BuildConfig"};
    public static boolean isNeedTraceClass(String fileName) {
        if (fileName.endsWith(".class")) {
            for (String unTraceClass : UN_TRACE_CLASS) {
                if (fileName.contains(unTraceClass)) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }
}
