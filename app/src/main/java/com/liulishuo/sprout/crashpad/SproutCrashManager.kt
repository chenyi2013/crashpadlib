package com.liulishuo.sprout.crashpad

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.androidcrasher.BuildConfig
import com.google.gson.Gson
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock


object SproutCrashManager {

    var applicatonContext: Context? = null
    val thread = Executors.newSingleThreadExecutor()
    val lock = ReentrantLock()

    const val URL = "sentry_url"
    const val SENTRY_KEY = "sentry_key"

    init {
        System.loadLibrary("native-lib")
    }

    fun setExtraData(map: HashMap<String, String>) {


        val tag = mutableMapOf<String, Any>()
        tag["release"] = BuildConfig.VERSION_NAME
        tag["tags"] = map

        thread.execute {
            try {
                lock.lock()
                val outputStreamWriter = OutputStreamWriter(
                    FileOutputStream(
                        applicatonContext?.getDir("crashpad", Context.MODE_PRIVATE)
                            .toString() + "/attachment.txt"
                    )
                )
                outputStreamWriter.write(Gson().toJson(tag))
                outputStreamWriter.close()
            } catch (e: IOException) {
                Log.e("Exception", "File write failed: " + e.toString())
            } finally {
                lock.unlock()
            }
        }
    }

    fun initializeCrashpad(context: Context, url: String, sentryKey: String) {
        applicatonContext = context.applicationContext
        initializeCrashpad(
            "${context.applicationContext.applicationInfo.nativeLibraryDir}/libcrashpad_handler.so",
            context.getDir("crashpad", Context.MODE_PRIVATE).toString()
        )
        context.startService(Intent(context, MonitorCrashService::class.java).apply {
            putExtra(SENTRY_KEY, sentryKey)
            putExtra(URL, url)
        })

    }


    private external fun initializeCrashpad(handlerPath: String, dataDir: String): Boolean
    external fun crash(): Boolean


}