package xyz.yorek.plugin.sample;

import android.app.ActivityManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.text.TextPaint;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Keep;

import java.util.Arrays;
import java.util.List;

import kotlin.jvm.JvmStatic;
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

        return addHookText2Bitmap(originalBitmap);
    }

    @MethodProxy(clazz = ImageView.class, method = "setImageResource")
    public static void setImageResource(ImageView imageView, int resId) {
        Log.d(TAG, "setImageResource called by: " + new Throwable().getStackTrace()[1]);
        imageView.setImageResource(resId);

        if (imageView.getDrawable() instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            Bitmap hookedBitmap = addHookText2Bitmap(bitmap);
            imageView.setImageBitmap(hookedBitmap);
        }
    }

    @MethodProxy(clazz = ImageView.class, method = "setImageBitmap")
    public static void setImageBitmap(ImageView imageView, Bitmap bitmap) {
        Log.d(TAG, "setImageBitmap called by: " + new Throwable().getStackTrace()[1]);
        imageView.setImageBitmap(bitmap);

        Bitmap hookedBitmap = addHookText2Bitmap(bitmap);
        imageView.setImageBitmap(hookedBitmap);
    }

    @MethodProxy(clazz = ImageView.class, method = "setImageDrawable")
    public static void setImageDrawable(ImageView imageView, Drawable drawable) {
        Log.d(TAG, "setImageDrawable called by: " + new Throwable().getStackTrace()[1]);
        imageView.setImageDrawable(drawable);

        if (drawable instanceof BitmapDrawable) {
            Bitmap hookedBitmap = addHookText2Bitmap(((BitmapDrawable) drawable).getBitmap());
            imageView.setImageBitmap(hookedBitmap);
        }
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

    private static Bitmap addHookText2Bitmap(Bitmap originalBitmap) {
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
}
