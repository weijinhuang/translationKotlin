package com.hwj.translation

import java.io.PrintStream

class MyExt {
}

fun <T> List<T>.print() {
    val sb = java.lang.StringBuilder("[")
    forEach {
        sb.append(it.toString())
    }
    sb.append("]")
    println(sb.toString())
}

val ps = PrintStream(System.out, true, "UTF-8")
fun println2(msg:String){
    ps.println(msg)
}