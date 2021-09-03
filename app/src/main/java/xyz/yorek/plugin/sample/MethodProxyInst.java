package xyz.yorek.plugin.sample;

import android.app.ActivityManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Keep;

import com.facebook.soloader.SoLoader;

import java.io.File;
import java.util.List;

import xyz.yorek.plugin.inst.anno.MethodProxy;
import xyz.yorek.plugin.inst.anno.MethodProxyEntry;

@Keep
@MethodProxyEntry
public class MethodProxyInst {
    private static final String TAG = "MethodProxyInst";

//    @MethodProxy(target = BitmapFactory.class, method = "decodeResource")
//    public static Bitmap decodeResource(Resources resources, int id) {
//        Log.d(TAG, "decodeResourceInBitmapFactory >>>>>>>>>>>>", new Throwable());
//        return BitmapFactory.decodeResource(resources, id);
//    }
//
//    @MethodProxy(target = Process.class, method = "myPid")
//    public static int myPid() {
//        Log.d(TAG, "myPidInProcess >>>>>>>>>>>>", new Throwable());
//        return android.os.Process.myPid();
//    }
//
//    @MethodProxy(target = ActivityManager.class, method = "getRunningAppProcesses")
//    public static List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses(
//        ActivityManager activityManager
//    ) {
//        Log.d(TAG, "getRunningAppProcessesInActivityManager >>>>>>>>>>>>", new Throwable());
//        return activityManager.getRunningAppProcesses();
//    }

    @MethodProxy(target = System.class, method = "loadLibrary")
    public static void loadLibrary(String libname) {
//        Log.e(TAG, "loadLibrary:" + libname, new Throwable());
        File extractDir = PackedAppLibsExtract.INSTANCE.getDir(SampleApp.getApplication());
        if (extractDir.exists() && extractDir.isDirectory() && extractDir.canRead()) {
            File libFile = new File(extractDir, "lib");
            if (libFile.exists() && libFile.isDirectory() && libFile.canRead()) {
                // TODO getAbi
                String abi = "arm64-v8a";
                File abiLibFile = new File(libFile, abi);
                if (abiLibFile.exists() && abiLibFile.isDirectory() && abiLibFile.canRead()) {
                    File targetPackedLib = new File(abiLibFile, "lib" + libname + ".so");
                    if (targetPackedLib.exists() && targetPackedLib.canRead()) {
//                        System.load(targetPackedLib.getAbsolutePath());
                        SoLoader.loadLibrary(libname);
                        return;
                    }
                }
            }
        }

        System.loadLibrary(libname);
    }
}
