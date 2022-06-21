package xyz.yorek.plugin.sample

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by yorek.liu on 2021/8/12
 * @author yorek.liu
 */
class MainActivity : AppCompatActivity() {
    private companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "编译时代码替换示例"
        setContentView(R.layout.activity_main)

//        findViewById<ImageView>(R.id.imageView1).setImageBitmap(
//            BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)
//        )
//        findViewById<ImageView>(R.id.imageView2).setImageBitmap(
//            BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)
//        )
        findViewById<ImageView>(R.id.imageView1).setImageResource(R.mipmap.ic_launcher_round)
        findViewById<ImageView>(R.id.imageView2).setImageResource(R.mipmap.ic_launcher_round)
        findViewById<TextView>(R.id.textView).apply {
            val pid = testPid()
            val processName = testProcessName()
            this.text =
            """编译时代码替换检测，pid应为-10086，进程名应为helloworld
                pid=${pid}
                processName=${processName}
            """.trimIndent()

            if (-10086 != pid || "helloworld" != processName) {
                this.setBackgroundColor(Color.RED)
            } else {
                this.setBackgroundColor(Color.GREEN)
            }
        }

        InnerClassTest().testPidInnerClass()
        Log.d(TAG, "C++ ? ${plus() == "C++"}")
    }

    private fun testPid(): Int {
        return android.os.Process.myPid()
    }

    private fun testProcessName(): String {
        var runningAppProcesses: List<RunningAppProcessInfo>? = null
        try {
            runningAppProcesses = (getSystemService(ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses
        } catch (e: Exception) {
            Log.e("MainActivity", e.message, e)
        }

        if (runningAppProcesses != null) {
            for (appProcess in runningAppProcesses) {
                if (appProcess.pid == android.os.Process.myPid()) {
                    return appProcess.processName
                }
            }
        }

        return "unknown"
    }

    private fun plus(): String {
        return "C++"
    }

    class InnerClassTest {
        fun testPidInnerClass() {
            val pid = android.os.Process.myPid()
            Log.d("InnerClassTest", "pid=$pid")
        }
    }
}