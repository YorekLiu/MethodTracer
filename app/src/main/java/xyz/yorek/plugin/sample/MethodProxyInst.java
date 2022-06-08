package xyz.yorek.plugin.sample;

import android.app.ActivityManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Process;
import android.text.TextPaint;
import android.util.Log;

import androidx.annotation.Keep;

import java.util.Arrays;
import java.util.List;

import xyz.yorek.plugin.inst.anno.MethodProxy;
import xyz.yorek.plugin.inst.anno.MethodProxyEntry;

@Keep
@MethodProxyEntry
public class MethodProxyInst {
    private static final String TAG = "MethodProxyInst";

    @MethodProxy(className = "android.graphics.BitmapFactory", method = "decodeResource")
    public static Bitmap decodeResource(Resources resources, int id) {
        Log.d(TAG, "decodeResource called by: " + new Throwable().getStackTrace()[1]);

        Bitmap originalBitmap = BitmapFactory.decodeResource(resources, id);

        Bitmap bitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
        String text = "图片被加上了这段文本";
        Canvas canvas = new Canvas(bitmap);
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);

        paint.setTextSize(10);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        int x = (bitmap.getWidth() - bounds.width())/2;
        int y = (bitmap.getHeight() + bounds.height())/2;
        canvas.drawText(text, x, y, paint);

        return bitmap;
    }

    @MethodProxy(clazz = Process.class, method = "myPid")
    public static int myPid() {
        return -10086;
    }

    @MethodProxy(clazz = MainActivity.class, method = "testProcessName")
    public static String getRunningAppProcesses(
        MainActivity mainActivity
    ) {
        return "helloworld";
    }
}
