package com.hwj.translation.net

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

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()
        val url = request.url.toString()

        if (level == Level.NONE) {
            return chain.proceed(request)
        }

        val logTextSB = StringBuilder()
        if (level == Level.HEADERS || level == Level.BODY || level == Level.BODY_ONLY) {
            logTextSB.append(" \n${request.method} ${URLDecoder.decode(url, "UTF-8")}\n")
        }
        val logHeaders = level == Level.HEADERS || level == Level.BODY
        if (logHeaders) {
            val headers = request.headers
            if (headers.size > 0) {
                for (i in 0 until headers.size) {
                    logTextSB.append("Request Header:${headers.name(i)}:${headers.value(i)}\n")
                }
            } else {
                println("LongSeHttp Request Header empty!")
            }
        }
        val logBody = level == Level.BODY_ONLY || level == Level.BODY
        if (logBody) {
            val requestBody: RequestBody? = request.body
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
                    logTextSB.append("--> END " + request.method + " (binary " + requestBody.contentLength() + "-byte body omitted)")
                }
            }
        }
        logText(logTextSB.toString())
        logTextSB.clear()
        val response: Response = chain.proceed(request)
        val responseBody = response.body
        val contentLength = responseBody?.contentLength()
        if (level == Level.HEADERS || level == Level.BODY || level == Level.BODY_ONLY) {
            logTextSB.append(
                " \nResponse:${response.code} ${
                    if (response.message.isEmpty()) "" else ' '.toString()
                } ${response.message} ${
                    URLDecoder.decode(
                        response.request.url.toString(),
                        "UTF-8"
                    )
                } \n"
            )
        }
        val requestHeaders = response.headers
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
            logTextSB.append("<-- END HTTP (binary ${buffer?.size} -byte body omitted)")
            logText(logTextSB.toString())
            return response
        }
        if (logHeaders) {
            for (i in 0 until requestHeaders.size) {
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
                val byteCount = buffer.size.coerceAtMost(64)
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