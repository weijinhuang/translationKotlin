package com.hwj.translation.net

import okhttp3.OkHttpClient
import okhttp3.Response
import okio.IOException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

object RetrofitUtil {

    val mOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // 连接：10秒
        .readTimeout(60, TimeUnit.SECONDS)     // 读取：30秒
        .writeTimeout(60, TimeUnit.SECONDS)    // 写入：30秒
        .callTimeout(120, TimeUnit.SECONDS)     // 总时间：60秒
        .addInterceptor(OkHttpLogger()).addInterceptor(OkHttpHeaderInterceptor()).build()

    val mRetrofit = Retrofit.Builder().baseUrl("http://api.fanyi.baidu.com/")
        .client(mOkHttpClient).addConverterFactory(GsonConverterFactory.create())
        .build()

    val mTranslationApi = mRetrofit.create(BaiduTranslationApi::class.java)

    fun getDeepSeekClient(): OkHttpClient {
        return mOkHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)  // 添加重试
            .addInterceptor { chain ->
                // 重试逻辑
                var response: Response? = null
                var lastException: IOException? = null

                for (attempt in 1..3) {
                    try {
                        response = chain.proceed(chain.request())
                        break
                    } catch (e: SocketTimeoutException) {
                        lastException = e
                        if (attempt == 3) throw e
                        Thread.sleep(1000L * attempt) // 递增等待
                    } catch (e: IOException) {
                        lastException = e
                        if (attempt == 3) throw e
                    }
                }
                response ?: throw lastException!!
            }
            // 可选：重用拦截器
            .addInterceptor(OkHttpLogger())
            .addInterceptor(OkHttpHeaderInterceptor())
            .build()
    }
}