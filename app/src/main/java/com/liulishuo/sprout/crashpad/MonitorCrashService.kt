package com.liulishuo.sprout.crashpad

import android.app.Service
import android.content.Intent
import android.os.FileObserver
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import java.io.File
import java.util.*


class MonitorCrashService : Service() {

    private val timer: Timer = Timer()
    private val uploadFileThread: HandlerThread = HandlerThread("upload_file")
    var uploadFileHandler: Handler? = null
    var fileObserver: FileObserver? = null
    val fileHelper: FileHelper by lazy {
        FileHelper(applicationContext)
    }

    val task: TimerTask = object : TimerTask() {
        override fun run() {
            uploadUnCompletedDumpFile()
        }
    }

    override fun onCreate() {
        super.onCreate()
        startUploadFileThread()
        startTimer()
    }

    private fun startTimer() {
        timer.schedule(task, 3000, 1000 * 60)
    }

    private fun startUploadFileThread() {
        uploadFileThread.start()
        uploadFileHandler = Handler(uploadFileThread.looper)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.apply {
            FileUploader.init(
                getStringExtra(SproutCrashManager.URL),
                getStringExtra(SproutCrashManager.SENTRY_KEY),
                fileHelper
            )
        }
        startWatching()
        return START_REDELIVER_INTENT
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    private fun uploadUnCompletedDumpFile() {
        val file = File(fileHelper.getMinidumpFileRootPath())

        file.listFiles()?.forEach { path ->

            if (fileHelper.isDumpFile(path.name)) {
                val fileName = fileHelper.parseFileName(path.name)
                uploadFileHandler?.post {
                    val minidump = fileHelper.getMinidumpFilePath(fileName)
                    val attachment = fileHelper.getAttachmentFilePath(fileName)
                    FileUploader.uploadFile(minidump, attachment)
                }
            }
        }
    }

    private fun startWatching() {
        val eventMask = FileObserver.ALL_EVENTS
        fileObserver?.stopWatching()
        fileObserver = object : FileObserver(fileHelper.getMinidumpFileRootPath(), eventMask) {
            override fun onEvent(event: Int, path: String?) {
                if (event == MOVED_TO) {
                    path?.let {

                        if (fileHelper.isDumpFile(it)) {

                            Log.e("kevin", "event :$event path: $path")

                            val fileName = fileHelper.parseFileName(it)

                            val minidump = fileHelper.getMinidumpFilePath(fileName)
                            val attachment = fileHelper.getAttachmentFilePath(fileName)

                            Log.e("kevin", "event :$event attachment: $attachment")

                            uploadFileHandler?.postAtFrontOfQueue {
                                FileUploader.uploadFile(minidump, attachment)
                            }
                        }
                    }
                }
            }
        }
        fileObserver?.startWatching()
    }

    override fun onDestroy() {
        super.onDestroy()
        fileObserver?.stopWatching()
    }
}
