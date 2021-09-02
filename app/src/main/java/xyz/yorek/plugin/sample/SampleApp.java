package xyz.yorek.plugin.sample;

import android.app.Application;
import android.content.Context;
/**
 * Created by yorek.liu on 2021/9/2
 *
 * @author yorek.liu
 */
public class SampleApp extends Application {

    private static Application sApp;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sApp = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PackedAppLibsExtract.INSTANCE.extract(this);
    }

    public static Application getApplication() {
        return sApp;
    }
}
