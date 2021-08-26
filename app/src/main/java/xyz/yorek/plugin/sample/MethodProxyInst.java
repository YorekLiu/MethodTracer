package xyz.yorek.plugin.sample;

import android.app.ActivityManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Keep;

import java.util.List;

import xyz.yorek.plugin.inst.anno.MethodProxy;
import xyz.yorek.plugin.inst.anno.MethodProxyEntry;

@Keep
@MethodProxyEntry
public class MethodProxyInst {
    private static final String TAG = "MethodProxyInst";

    @MethodProxy(target = BitmapFactory.class, method = "decodeResource")
    public static Bitmap decodeResource(Resources resources, int id) {
        Log.d(TAG, "decodeResourceInBitmapFactory >>>>>>>>>>>>", new Throwable());
        return BitmapFactory.decodeResource(resources, id);
    }

    @MethodProxy(target = Process.class, method = "myPid")
    public static int myPid() {
        Log.d(TAG, "myPidInProcess >>>>>>>>>>>>", new Throwable());
        return android.os.Process.myPid();
    }

    @MethodProxy(target = ActivityManager.class, method = "getRunningAppProcesses")
    public static List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses(
        ActivityManager activityManager
    ) {
        Log.d(TAG, "getRunningAppProcessesInActivityManager >>>>>>>>>>>>", new Throwable());
        return activityManager.getRunningAppProcesses();
    }
}
