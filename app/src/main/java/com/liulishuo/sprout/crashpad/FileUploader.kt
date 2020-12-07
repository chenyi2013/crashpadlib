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
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

object FileUploader {
    fun uploadFile(minidumpStr: String, attachmentStr: String) {
        val client = getOkHttpClient()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://o482556.ingest.sentry.io")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val apiService = retrofit.create(ApiService::class.java)


        val minidumpPart =
            createMinidumpPart(minidumpStr)
        val attachmentPart =
            createAttachmentPart(attachmentStr)

        val attach = File(attachmentStr)
        val reader = FileReader(attach)
        val tagPart =
            MultipartBody.Part.createFormData("sentry", reader.readText())
        
        reader.close()
        val call =
            apiService.uploadFile(
                tagPart,
                minidumpPart,
                attachmentPart,
                "831426a7a35e4ed1817c6701cee85876"
            )

        val response = call.execute()

        if (response.isSuccessful) {
            Log.i("kevin", response.body().toString())
            File(minidumpStr).delete()
        } else {
            Log.i("kevin", response.errorBody().toString())
        }
    }


    fun createAttachmentPart(attachmentStr: String): MultipartBody.Part {
        val attachmentFile = File(attachmentStr)
        val attachmentRequestBody =
            RequestBody.create(MediaType.parse("application/otcet-stream"), attachmentFile)
        val attachmentPart =
            MultipartBody.Part.createFormData(
                "attachment.txt",
                attachmentFile.name,
                attachmentRequestBody
            )
        return attachmentPart
    }

    fun createMinidumpPart(minidumpStr: String): MultipartBody.Part {
        val minidumpFile = File(minidumpStr)
        val minidumpRequestBody =
            RequestBody.create(MediaType.parse("application/otcet-stream"), minidumpFile)
        val minidumpPart =
            MultipartBody.Part.createFormData(
                "upload_file_minidump",
                minidumpFile.name,
                minidumpRequestBody
            )
        return minidumpPart
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