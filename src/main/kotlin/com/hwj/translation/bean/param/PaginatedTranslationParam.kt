package com.hwj.translation.bean.param

class PaginatedTranslationParam(

){
    var projectId: String = ""
    var page: Int = 0 // 页码，从0开始
    var size: Int = 20  // 每页大小，默认20条
}