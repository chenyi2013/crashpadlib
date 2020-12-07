package com.liulishuo.sprout.crashpad

import android.util.Log
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

object FileUploader {

    var url: String? = null
    var sentryKey: String? = null
    var fileHelper: FileHelper? = null

    fun init(url: String, sentryKey: String, fileHelper: FileHelper) {
        this.url = url
        this.sentryKey = sentryKey
        this.fileHelper = fileHelper
    }


    fun uploadFile(minidumpStr: String, attachmentStr: String) {

        if (!File(minidumpStr).exists()) {
            return
        }

        val client = getOkHttpClient()

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val apiService = retrofit.create(ApiService::class.java)


        val minidumpPart =
            createMinidumpPart(minidumpStr)
        val attachmentPart =
            createAttachmentPart(attachmentStr)

        var annotation: String? = null
        val attach = File(attachmentStr)
        val reader = FileReader(attach)

        try {
            annotation = reader.readText()
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }


        val tagPart =
            MultipartBody.Part.createFormData("sentry", annotation ?: "")

        val call = if (attach.exists())
            apiService.uploadFile(
                tagPart,
                minidumpPart,
                attachmentPart,
                sentryKey
            ) else apiService.uploadFile(
            minidumpPart,
            sentryKey
        )

        val response = call.execute()
        if (response.isSuccessful) {
            Log.i("kevin", response.body().toString())
            deleteUploadCompletedFile(minidumpStr)
        } else {
            Log.i("kevin", response.errorBody().toString())
        }


    }

    private fun deleteUploadCompletedFile(minidumpStr: String) {

        try {
            fileHelper?.let {
                val fileName = it.parseFileName(File(minidumpStr).name)
                File(it.getMetaFilePath(fileName)).delete()
                val attachmentParent = File(it.getAttachmentFileParentPath(fileName))
                attachmentParent?.deleteRecursively()
            }
            File(minidumpStr).delete()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }


    private fun createAttachmentPart(attachmentStr: String): MultipartBody.Part? {
        val attachmentFile = File(attachmentStr)

        if (!attachmentFile.exists()) {
            return null
        }

        val attachmentRequestBody =
            RequestBody.create(MediaType.parse("application/otcet-stream"), attachmentFile)
        return MultipartBody.Part.createFormData(
            "attachment.txt",
            attachmentFile.name,
            attachmentRequestBody
        )
    }

    private fun createMinidumpPart(minidumpStr: String): MultipartBody.Part {
        val minidumpFile = File(minidumpStr)

        val minidumpRequestBody =
            RequestBody.create(MediaType.parse("application/otcet-stream"), minidumpFile)
        return MultipartBody.Part.createFormData(
            "upload_file_minidump",
            minidumpFile.name,
            minidumpRequestBody
        )
    }

    private fun getOkHttpClient(): OkHttpClient? {
        //定制OkHttp
        val httpClientBuilder = OkHttpClient.Builder()
        val xtm: X509TrustManager = object : X509TrustManager {
            override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
            }

            override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate?> {
                return arrayOfNulls<X509Certificate>(0)
            }

        }
        var sslContext: SSLContext? = null
        try {
            sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, arrayOf<TrustManager>(xtm), SecureRandom())
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
        val DO_NOT_VERIFY: HostnameVerifier = object : HostnameVerifier {
            override fun verify(hostname: String?, session: SSLSession?): Boolean {
                return true
            }
        }

        if (sslContext != null) {
            httpClientBuilder.sslSocketFactory(sslContext.getSocketFactory());
        }
        httpClientBuilder.hostnameVerifier(DO_NOT_VERIFY)
        return httpClientBuilder.build()
    }
}