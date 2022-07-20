package xyz.yorek.plugin;

import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("unused")
public final class Util {
    private static final String TAG         = "Util";
    public static final  int    BUFFER_SIZE = 16384;

    private Util() {
    }

    public static void addZipEntry(ZipOutputStream zipOutputStream, ZipEntry zipEntry, InputStream inputStream) throws Exception {
        try {
            zipOutputStream.putNextEntry(zipEntry);
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                zipOutputStream.write(buffer, 0, length);
                zipOutputStream.flush();
            }
        } catch (ZipException e) {
//            Log.e(TAG, "addZipEntry err!");
        } finally {
            closeQuietly(inputStream);

            zipOutputStream.closeEntry();
        }
    }

    public static boolean isNullOrNil(String str) {
        return str == null || str.isEmpty();
    }

    public static Field getDeclaredFieldRecursive(Object clazz, String fieldName) throws NoSuchFieldException, ClassNotFoundException {
        Class<?> realClazz;
        if (clazz instanceof String) {
            realClazz = Class.forName((String) clazz);
        } else if (clazz instanceof Class) {
            realClazz = (Class<?>) clazz;
        } else {
            throw new IllegalArgumentException("Illegal clazz type: " + clazz.getClass());
        }
        Class<?> currClazz = realClazz;
        while (true) {
            try {
                Field field = currClazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                if (currClazz.equals(Object.class)) {
                    throw e;
                }
                currClazz = currClazz.getSuperclass();
            }
        }
    }

    public static boolean isRealZipOrJar(File input) {
        ZipFile zf = null;
        try {
            zf = new ZipFile(input);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            Util.closeQuietly(zf);
        }
    }


    /**
     * Close {@code target} quietly.
     *
     * @param obj
     *  Object to be closed.
     */
    public static void closeQuietly(@Nullable Object obj) {
        if (obj == null) {
            return;
        }
        if (obj instanceof Closeable) {
            try {
                ((Closeable) obj).close();
            } catch (Throwable ignored) {
                // ignore
            }
        } else if (obj instanceof AutoCloseable) {
            try {
                ((AutoCloseable) obj).close();
            } catch (Throwable ignored) {
                // ignore
            }
        } else {
            throw new IllegalArgumentException("obj " + obj + " is not closeable");
        }
    }

    public static String readFileAsString(String filePath) {
        StringBuilder fileData = new StringBuilder();
        Reader fileReader = null;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            fileReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            char[] buf = new char[BUFFER_SIZE];
            int numRead;
            while ((numRead = fileReader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
            }
        } catch (Exception e) {
//            Log.e(TAG, "file op readFileAsString e type:%s, e msg:%s, filePath:%s",
//                  e.getClass().getSimpleName(), e.getMessage(), filePath);
        } finally {
            try {
                closeQuietly(fileReader);
                closeQuietly(inputStream);
            } catch (Exception e) {
//                Log.e(TAG, "file op readFileAsString close e type:%s, e msg:%s, filePath:%s",
//                      e.getClass().getSimpleName(), e.getMessage(), filePath);
            }
        }
        return fileData.toString();
    }

    public static void copyFileUsingStream(File source, File dest) throws IOException {
        FileInputStream is = null;
        FileOutputStream os = null;
        File parent = dest.getParentFile();
        if (parent != null && (!parent.exists())) {
            parent.mkdirs();
        }
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest, false);

            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            closeQuietly(is);
            closeQuietly(os);
        }
    }

    public static boolean sevenZipInputDir(File inputDir, File outputFile, String sevenZipPath) {
        String outPath = inputDir.getAbsolutePath();
        String path = outPath + File.separator + "*";
        String cmd = sevenZipPath;

        ProcessBuilder pb = new ProcessBuilder(cmd, "a", "-tzip", outputFile.getAbsolutePath(), path, "-mx9");
        pb.redirectErrorStream(true);
        Process pro = null;
        LineNumberReader reader = null;
        try {
            pro = pb.start();
            reader = new LineNumberReader(new InputStreamReader(pro.getInputStream()));
            while (reader.readLine() != null) { }
        } catch (IOException e) {
            outputFile.delete();
            System.out.println("7a file failed");
            return false;
        } finally {
            //destroy the stream
            try {
                pro.waitFor();
            } catch (Throwable ignored) {
                // Ignored.
            }
            try {
                pro.destroy();
            } catch (Throwable ignored) {
                // Ignored.
            }
            closeQuietly(reader);
        }
        return true;
    }
}
