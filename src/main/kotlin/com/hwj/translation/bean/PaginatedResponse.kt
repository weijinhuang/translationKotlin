package com.hwj.translation.bean

data class PaginatedResponse<T>(
    val content: List<T>,           // 当前页数据
    val page: Int,                  // 当前页码（从0开始）
    val size: Int,                  // 每页大小
    val totalElements: Long,        // 总记录数
    val totalPages: Int,            // 总页数
    val hasNext: Boolean,           // 是否有下一页
    val hasPrevious: Boolean        // 是否有上一页
)