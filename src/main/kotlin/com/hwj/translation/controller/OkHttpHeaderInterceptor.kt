package com.hwj.translation.controller

import okhttp3.Interceptor
import okhttp3.Response


/**
 *@Create by H.W.J 2023/8/3/003
 */
class OkHttpHeaderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()
        val newRequest = request.newBuilder().addHeader("Content-Type", "application/x-www-form-urlencoded").build()
        return chain.proceed(newRequest)
    }


}