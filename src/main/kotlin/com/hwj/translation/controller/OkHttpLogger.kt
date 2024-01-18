package com.hwj.translation.controller

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.io.EOFException
import java.net.URLDecoder
import java.nio.charset.Charset

class OkHttpLogger : Interceptor {
    var logger: Logger = Logger.DEFAULT

    private val UTF8 = Charset.forName("UTF-8")

    @set:JvmName("level")
    @Volatile
    var level = Level.BODY_ONLY

    /**
     * 需要过滤的接口，此列表内的接口不打印日志
     */
    var requestFilterList: ArrayList<String> = arrayListOf(
        "mp4",
        "device/recentapprovals",
        "common/audio/upload-file",
        "common/device/get-device-pic"
    )

    val eventUrl = arrayListOf(
        "device/deactivatedevice",//删除设备
        "device/undoshareself",//积加删除分享设备
        "device/dormancy/switch", //积加设备休眠
        "device/deactivatedevice", //积加设备删除
        "device/updatedevicename", //积加设备更新名字
        "consumer/lp-device/set-device-name",//BV设备名字
        "consumer/lp-device/break-device",//解绑BV设备
        "consumer/lp-device/bind-share-device-with-token",//添加分享设备
        "device/otastart"//积加设备OTA升级
    )

    interface Logger {
        fun log(message: String)

        companion object {

            @JvmField
            val DEFAULT: Logger = object : Logger {
                override fun log(message: String) {
                    println("LongSeHttp:$message")
                }
            }
        }
    }

    enum class Level {
        /** 不打印日志 */
        NONE,

        /**
         * 仅打印头部
         */
        HEADERS,

        /**
         * 打印头部和body
         */
        BODY,

        /**
         * 仅打印Body
         */
        BODY_ONLY
    }

    constructor()

    constructor(requestFilterMap: ArrayList<String>) {
        this.requestFilterList.addAll(requestFilterMap)
    }

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()
        val url = request.url().toString()
        eventUrl.forEach {
            if (url.contains(it)) {
                return@forEach
            }
        }

        if (level == Level.NONE) {
            return chain.proceed(request)
        }

        val pass = requestFilterList.let {
            if (url.contains("mp4")) {
                return@let true
            }
            for (e in it) {
                if (url.contains(e)) {
                    return@let true
                }
            }
            return@let false
        }
        if (pass) {
            return chain.proceed(request)
        }
        val logTextSB = StringBuilder()
        if (level == Level.HEADERS || level == Level.BODY || level == Level.BODY_ONLY) {
            logTextSB.append(" \n${request.method()} ${URLDecoder.decode(url, "UTF-8")}\n")
        }
        val logHeaders = level == Level.HEADERS || level == Level.BODY
        if (logHeaders) {
            val headers = request.headers()
            if (headers.size() > 0) {
                for (i in 0 until headers.size()) {
                    logTextSB.append("Request Header:${headers.name(i)}:${headers.value(i)}\n")
                }
            } else {
                println("LongSeHttp Request Header empty!")
            }
        }
        val logBody = level == Level.BODY_ONLY || level == Level.BODY
        if (logBody) {
            val requestBody: RequestBody? = request.body()
            if (null != requestBody) {
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                var charset = UTF8
                val contentType: MediaType? = requestBody.contentType()
                if (contentType != null) {
                    charset = contentType.charset(UTF8)
                }
                if (null != contentType && isPlaintext(buffer)) {
                    val bodyStr = buffer.readString(charset)
                    logTextSB.append(bodyStr)
                } else {
                    logTextSB.append("--> END " + request.method() + " (binary " + requestBody.contentLength() + "-byte body omitted)")
                }
            }
        }
        logText(logTextSB.toString())
        logTextSB.clear()
        val response: Response = chain.proceed(request)
        val responseBody = response.body()
        val contentLength = responseBody?.contentLength()
        if (level == Level.HEADERS || level == Level.BODY || level == Level.BODY_ONLY) {
            logTextSB.append(
                " \nResponse:${response.code()} ${
                    if (response.message().isEmpty()) "" else ' '.toString()
                } ${response.message()} ${
                    URLDecoder.decode(
                        response.request().url().toString(),
                        "UTF-8"
                    )
                } \n"
            )
        }
        val requestHeaders = response.headers()
        val source = responseBody?.source()

        source?.request(Long.MAX_VALUE) // Buffer the entire body.
        var buffer = source?.buffer()
//        if ("gzip".equals(requestHeaders["Content-Encoding"], ignoreCase = true)) {
//            GzipSource(buffer?.clone()).use { gzippedResponseBody ->
//                buffer = Buffer()
//                buffer?.writeAll(gzippedResponseBody)
//            }
//        }
        var charset = UTF8
        val contentType: MediaType? = responseBody?.contentType()
        if (contentType != null) {
            charset = contentType.charset(UTF8)
        }
        if (!isPlaintext(buffer)) {
            logTextSB.append("<-- END HTTP (binary ${buffer?.size()} -byte body omitted)")
            logText(logTextSB.toString())
            return response
        }
        if (logHeaders) {
            for (i in 0 until requestHeaders.size()) {
                logTextSB.append("Response Header:${requestHeaders.name(i)}:${requestHeaders.value(i)}\n")
            }
        }
        if (logBody) {
            if (contentLength != 0L) {
                logTextSB.append(buffer?.clone()?.readString(charset))
            }
        }
        logText(logTextSB.toString())
        return response
    }

    private fun logText(content: String) {
        if (content.length > 3000) {//太长的话进行分片打印，否则可能会打印不全。
            val segmentSize = content.length / 3000
            for (i in 0..segmentSize) {
                if (i == segmentSize) {
                    logger.log(content.substring(i * 3000))
                } else {
                    logger.log(content.substring(i * 3000, (i + 1) * 3000))
                }
            }
        } else {
            logger.log(content)
        }
    }


    private fun isPlaintext(bufferArg: Buffer?): Boolean {
        return bufferArg?.let { buffer ->
            try {
                val prefix = Buffer()
                val byteCount = buffer.size().coerceAtMost(64)
                buffer.copyTo(prefix, 0, byteCount)
                for (i in 0 until 16) {
                    if (prefix.exhausted()) {
                        break
                    }
                    val codePoint = prefix.readUtf8CodePoint()
                    if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                        return false
                    }
                }
                return true
            } catch (_: EOFException) {
                return false // Truncated UTF-8 sequence.
            }
        } ?: false
    }
}