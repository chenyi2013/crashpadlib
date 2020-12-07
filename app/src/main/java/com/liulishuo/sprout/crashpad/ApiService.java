package com.liulishuo.sprout.crashpad;

//https://www.cnblogs.com/zhujiabin/p/7601658.html

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @Multipart
    @POST("/api/5536234/minidump/")
    Call<ResponseBody> uploadFile(@Part MultipartBody.Part tag, @Part MultipartBody.Part minidump, @Part MultipartBody.Part attachment, @Query("sentry_key") String sentry_key);
}
