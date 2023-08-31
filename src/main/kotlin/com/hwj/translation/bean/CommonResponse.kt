package com.hwj.translation.bean

data class CommonResponse<DATA>(val code: Int, val msg: String?, val data: DATA?)
