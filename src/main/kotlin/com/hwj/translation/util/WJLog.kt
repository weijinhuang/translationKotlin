package com.hwj.translation.util

import com.hwj.translation.println2
import java.util.*


fun log(tag: String? = "", msg: String) {
    println2("[${Date()}][$tag]:${msg}")
}