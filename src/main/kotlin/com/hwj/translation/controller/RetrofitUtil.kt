package com.hwj.translation.controller

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitUtil {

    val mOkHttpClient = OkHttpClient.Builder().addInterceptor(OkHttpLogger()).addInterceptor(OkHttpHeaderInterceptor()).build()

    val mRetrofit = Retrofit.Builder().baseUrl("http://api.fanyi.baidu.com/")
        .client(mOkHttpClient).addConverterFactory(GsonConverterFactory.create())
        .build()

    val mTranslationApi = mRetrofit.create(TranslationApi::class.java)


}