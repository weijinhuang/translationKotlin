package com.hwj.translation

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