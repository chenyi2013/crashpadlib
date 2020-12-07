package com.example.androidcrasher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.liulishuo.sprout.crashpad.MonitorCrashService
import com.liulishuo.sprout.crashpad.SproutCrashManager
import java.util.*


class MainActivity : AppCompatActivity() {

    var count: Int = 0
    var count1: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i("kevin","package"+getDir("crashpad1", Context.MODE_PRIVATE).toString())

        getDir("crashpad1", Context.MODE_PRIVATE).createNewFile()
        SproutCrashManager.initializeCrashpad(this)

        var annotations = HashMap<String, String>()
        annotations["format"] =
            "minidump";           // Required: Crashpad setting to save crash as a minidump
        annotations["database"] = "252019161_qq_com";             // Required: BugSplat appName
        annotations["product"] = "AndroidCrasher"; // Required: BugSplat appName
        annotations["version"] = "1.0.1";             // Required: BugSplat appVersion
        annotations["key"] = "Samplekey";            // Optional: BugSplat key field
        annotations["user"] = "fred@bugsplat.com";    // Optional: BugSplat user email
        annotations["list_annotations"] = "Sample comment"; // Optional: BugSplat crash description
        SproutCrashManager.setExtraData(annotations)

        startService(Intent(this, MonitorCrashService::class.java))



    }

    public fun btnCrashClick(view: View) {
//        SproutCrashManager.crash()
        Log.i("kevin", "dir:" + this.applicationContext.applicationInfo.packageName)

        for(i in 0..99){
            Thread{
                b(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
            }.start()
        }

        Thread.sleep(10000)
        SproutCrashManager.crash()

//        a(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

    }

    fun a(
        p0: Long,
        p1: Long,
        p2: Long,
        p3: Long,
        p4: Long,
        p5: Long,
        p6: Long,
        p7: Long,
        p8: Long,
        p9: Long
    ) {
        if (count < 1000) {
            count++
            a(p0, p1, p2, p3, p4, p5, p6, p7, p8, p9)
        } else {
            SproutCrashManager.crash()
        }
    }

    fun b(
        p0: Long,
        p1: Long,
        p2: Long,
        p3: Long,
        p4: Long,
        p5: Long,
        p6: Long,
        p7: Long,
        p8: Long,
        p9: Long
    ) {
        if (count1 < 1000) {
            count1++
            b(p0, p1, p2, p3, p4, p5, p6, p7, p8, p9)
        } else {
            while (true);
        }
    }

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
