package com.hwj.translation.controller

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException




class MD5Util {
    fun getMD5Encoding(s: String): String? {
        val input = s.toByteArray()
        var output: String? = null
        val hexChar = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f'
        )
        try {
            val md: MessageDigest = MessageDigest.getInstance("MD5")
            md.update(input)
            /*
             */
            val tmp: ByteArray = md.digest()
            val str = CharArray(32)
            var b: Byte = 0
            for (i in 0..15) {
                b = tmp[i]
                str[2 * i] = hexChar[b.toInt() ushr 4 and 0xf]
                str[2 * i + 1] = hexChar[b.toInt() and 0xf]
            }
            output = String(str)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return output
    }

}