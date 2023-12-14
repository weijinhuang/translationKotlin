package com.hwj.translation.bean.param

import java.net.URLEncoder

class BaiduTranslationParam {
    var q:String? = null
    var from:String? = null
    var to:String? = null
    var appid:String? = null
    var salt:String? = null
    var sign:String? = null
    override fun toString(): String {
        return "BaiduTranslationParam(q=$q, from=$from, to=$to, appid=$appid, salt=$salt, sign=$sign)"
    }

    fun encode(){
        q = q?.let {
            URLEncoder.encode(it, "utf-8")
        }
        from = from?.let {
            URLEncoder.encode(it, "utf-8")
        }
        to = to?.let {
            URLEncoder.encode(it, "utf-8")
        }
        appid = appid?.let {
            URLEncoder.encode(it, "utf-8")
        }
        salt = salt?.let {
            URLEncoder.encode(it, "utf-8")
        }
        sign = sign?.let {
            URLEncoder.encode(it, "utf-8")
        }
    }
}