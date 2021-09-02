package xyz.yorek.plugin.sample;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by yorek.liu on 2021/9/2
 *
 * @author yorek.liu
 */
class Util {
    public static void copyFileUsingStream(InputStream sourceIs, File dest) throws IOException {
        BufferedInputStream is = null;
        FileOutputStream os = null;
        File parent = dest.getParentFile();
        if (parent != null && (!parent.exists())) {
            parent.mkdirs();
        }
        try {
            is = new BufferedInputStream(sourceIs);
            os = new FileOutputStream(dest, false);

            byte[] buffer = new byte[4 * 1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
//            closeQuietly(is);
            closeQuietly(os);
        }
    }

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
        } else {
            throw new IllegalArgumentException("obj " + obj + " is not closeable");
        }
    }
}
