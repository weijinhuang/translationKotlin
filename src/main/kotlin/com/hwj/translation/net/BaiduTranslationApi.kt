package com.hwj.translation.net

import com.hwj.translation.bean.param.BaiduTranslationResult
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Query

interface BaiduTranslationApi {

    @POST("api/trans/vip/translate")
    fun translateByBaidu(
        @Query("q" )q:String,
        @Query("salt" )salt:String,
        @Query("appid" )appid:String,
        @Query("sign" )sign:String,
        @Query("from" ) from:String,
        @Query("to" ) to:String,

    ): Call<BaiduTranslationResult>
}