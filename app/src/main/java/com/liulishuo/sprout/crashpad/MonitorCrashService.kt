package com.liulishuo.sprout.crashpad

import android.app.Service
import android.content.Intent
import android.os.FileObserver
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import java.io.File


class MonitorCrashService : Service() {

    private val uploadFileThread: HandlerThread = HandlerThread("upload_file")
    val uploadFileHandler: Handler
    val fileHelper: FileHelper by lazy {
        FileHelper(applicationContext)
    }

    init {
        uploadFileThread.start()
        uploadFileHandler = Handler(uploadFileThread.looper)
    }

    override fun onCreate() {
        super.onCreate()
        startObserving()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun startObserving() {
        startWatching()
        uploadUnCompletedDumpFile()
    }

    private fun uploadUnCompletedDumpFile() {
        val file = File(fileHelper.getMinidumpFileRootPath())

        file.listFiles().forEach { path ->

            if (fileHelper.isDumpFile(path.name)) {
                val fileName = fileHelper.parseFileName(path.name)
                uploadFileHandler.post {
                    val minidump = fileHelper.getMinidumpFilePath(fileName)
                    val attachment = fileHelper.getAttachmentFilePath(fileName)
                    FileUploader.uploadFile(minidump, attachment)
                }
            }
        }
    }

    private fun startWatching() {
        val eventMask = FileObserver.ALL_EVENTS


        val fileObserver = object : FileObserver(fileHelper.getMinidumpFileRootPath(), eventMask) {
            override fun onEvent(event: Int, path: String?) {
                if (event == 128) {
                    path?.let {

                        if (fileHelper.isDumpFile(it)) {

                            Log.e("kevin", "event :$event path: $path")

                            val fileName = fileHelper.parseFileName(it)

                            val minidump = fileHelper.getMinidumpFilePath(fileName)
                            val attachment = fileHelper.getAttachmentFilePath(fileName)

                            Log.e("kevin", "event :$event attachment: $attachment")

                            uploadFileHandler.postAtFrontOfQueue {
                                FileUploader.uploadFile(minidump, attachment)
                            }
                        }
                    }
                }
            }
        }
        fileObserver.startWatching()
    }
}
