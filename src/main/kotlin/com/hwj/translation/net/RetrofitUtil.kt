package com.hwj.translation.net

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitUtil {

    val mOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)  // 连接：10秒
        .readTimeout(30, TimeUnit.SECONDS)     // 读取：30秒
        .writeTimeout(30, TimeUnit.SECONDS)    // 写入：30秒
        .callTimeout(60, TimeUnit.SECONDS)     // 总时间：60秒
        .addInterceptor(OkHttpLogger()).addInterceptor(OkHttpHeaderInterceptor()).build()

    val mRetrofit = Retrofit.Builder().baseUrl("http://api.fanyi.baidu.com/")
        .client(mOkHttpClient).addConverterFactory(GsonConverterFactory.create())
        .build()

    val mTranslationApi = mRetrofit.create(BaiduTranslationApi::class.java)


}