package com.hwj.translation

import java.io.FileWriter
import java.io.PrintStream
import java.nio.charset.Charset


class EncodingDiagnosis {

    fun main(args: Array<String>) {
        println("===== 1. 系统属性与JVM编码 =====")
        println("file.encoding: " + System.getProperty("file.encoding"))
        println("sun.jnu.encoding: " + System.getProperty("sun.jnu.encoding"))
        System.out.println("Default Charset: " + Charset.defaultCharset())
        println("Console charset: " + System.console().charset())
        println("\n===== 2. 直接向控制台输出中文 =====")
        println("直接打印: 这是一段直接打印的中文")
        println("\n===== 3. 通过日志框架输出中文 =====")
        // 暂时不使用日志框架，用最原始方式
        val ps = PrintStream(System.out, true, "UTF-8")
        ps.println("使用PrintStream(UTF-8)打印: 这是一段通过UTF-8 PrintStream打印的中文")
        println("\n===== 4. 测试文件写入（作为对照） =====")
        FileWriter("encoding_test.txt").use { fw -> fw.write("这是一段写入文件的中文") }
        println("已创建文件 encoding_test.txt，请用记事本打开查看内容是否正常。")
    }
}