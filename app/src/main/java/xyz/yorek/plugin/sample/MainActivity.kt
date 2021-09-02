package xyz.yorek.plugin.sample

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

/**
 * Created by yorek.liu on 2021/8/12
 *
 * @author yorek.liu
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Glide.with(this)
            .load("https://mathiasbynens.be/demo/animated-webp-supported.webp")
            .into(findViewById<ImageView>(R.id.imageView1))

//        findViewById<ImageView>(R.id.imageView1).setImageBitmap(
//            BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)
//        )
        findViewById<ImageView>(R.id.imageView2).setImageBitmap(
            BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)
        )

//        testPid()
//        testProcessName()
    }

    private fun testPid() {
        val pid = android.os.Process.myPid()
        Log.d("MainActivity", "pid=$pid")
    }

    private fun testProcessName() {
        var runningAppProcesses: List<RunningAppProcessInfo>? = null
        try {
            runningAppProcesses = (getSystemService(ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses
        } catch (e: Exception) {
            Log.e("MainActivity", e.message, e)
        }

        if (runningAppProcesses != null) {
            for (appProcess in runningAppProcesses) {
                if (appProcess.pid == android.os.Process.myPid()) {
                    Log.d("MainActivity", "processName=${appProcess.processName}")
                    break
                }
            }
        }
    }
}