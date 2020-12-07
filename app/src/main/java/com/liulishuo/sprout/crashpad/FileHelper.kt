package com.liulishuo.sprout.crashpad

import android.content.Context
import java.io.File

class FileHelper(context: Context) {

    val context = context

    val rootPath = "/data/data/${context.packageName}/crashpad/"

    fun parseFileName(it: String): String {
        val index = it.indexOf(".")
        val fileName = it.substring(0, index)
        return fileName
    }

    fun getMinidumpFileRootPath(): String {
        return rootPath + "completed"
    }

    fun getAttachmentFilePath(fileName: String): String {
        return StringBuffer(rootPath)
            .append("attachments")
            .append(File.separator)
            .append(fileName)
            .append(File.separator)
            .append("attachment.txt")
            .toString()
    }

    fun getMinidumpFilePath(fileName: String): String {
        return StringBuffer(rootPath)
            .append("completed")
            .append(File.separator)
            .append(fileName)
            .append(".dmp")
            .toString()
    }


    fun isDumpFile(it: String) = it.endsWith(".dmp")


}