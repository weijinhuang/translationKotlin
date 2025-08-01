package com.hwj.translation.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


//@Configuration
//class FileUploadCorsConfig : WebMvcConfigurer {
//    override fun addCorsMappings(registry: CorsRegistry) {
//        registry.addMapping("/upload-translations")
//            .allowedOrigins("*")
//            .allowedMethods("POST", "OPTIONS")
//            .allowedHeaders("*")
//            .exposedHeaders("Content-Disposition")
//            .maxAge(3600)
//    }
//}