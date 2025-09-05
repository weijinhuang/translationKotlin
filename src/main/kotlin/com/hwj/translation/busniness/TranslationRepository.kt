package com.hwj.translation.busniness

import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.hwj.translation.bean.*
import com.hwj.translation.bean.param.*
import com.hwj.translation.controller.proxyHost
import com.hwj.translation.controller.proxyPort
import com.hwj.translation.dao.TranslationDao
import com.hwj.translation.util.handleSingleQuotes

class TranslationRepository(translationDao: TranslationDao) : BaseRepository(translationDao) {


    fun checkTranslationByKeyInProject(param: CommonParam<*>): CommonResponse<Int> {
        return parseRealParam(param, CheckTranslationCountOfKeyInProjectParam::class.java)?.let { realParam ->
            val count = mTranslationDao.getTranslationCountOfKeyInProject(realParam.projectId ?: "", realParam.translationKey ?: "")
            CommonResponse(200, "", count)
        } ?: CommonResponse(-1, "", null)
    }

    fun getTranslationListV2(param: CommonParam<*>): CommonResponse<List<Translation>> {
        return parseRealParam(param, GetTranslationParam::class.java)?.let { realParam ->
            val startTime = System.nanoTime()
            val translationList = if (null == realParam.moduleId) {
                mTranslationDao.getAllTranslationByProjectId(realParam.projectId)

            } else {
                mTranslationDao.queryTranslationByModule(realParam.moduleId!!, realParam.projectId)
            }
            val endTime = System.nanoTime()
            println("查詢到翻譯：${translationList.size} 花费时间：${endTime - startTime} 纳秒")
            CommonResponse(200, "", translationList.filter { it.hide == 0 })
        } ?: CommonResponse(-1, "参数错误", emptyList())
    }

    fun getTranslationListV3(param: CommonParam<*>): CommonResponse<List<TranslationRow>> {
        return parseRealParam(param, GetTranslationParam::class.java)?.let { realParam ->
            val translationList: List<Translation> = if (null == realParam.moduleId) {
                mTranslationDao.getAllTranslationByProjectId(realParam.projectId)
            } else {
                mTranslationDao.queryTranslationByModule(realParam.moduleId!!, realParam.projectId)
            }
            val translationRowList: List<TranslationRow> = translationList
                .groupBy { it.translationKey!! }
                .map { (key, translations) ->
                    TranslationRow(
                        translationKey = key,
                        translations = translations
                    )
                }
            CommonResponse(200, "", translationRowList)
        } ?: CommonResponse(-1, "参数错误", emptyList())
    }


    fun deleteTranslationByTranslationKeyV2(param: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(param, DeleteTranslationParam::class.java)?.let { deleteTranslationParam ->
            if (deleteTranslationParam.translationKey.isNullOrBlank() || deleteTranslationParam.projectId.isNullOrBlank()) {
                return CommonResponse(-1, "参数错误", null)
            }
            return try {
                var deleteTranslationSuccess = mTranslationDao.deleteTranslationByTranslationKey(
                    deleteTranslationParam.projectId!!, deleteTranslationParam.translationKey!!
                )
                if (!deleteTranslationSuccess) {
                    CommonResponse(-1, "删除翻译错误", null)
                } else {
                    CommonResponse(200, "删除翻译成功", null)
                }
            } catch (e: Exception) {
                CommonResponse(-1, e.message, null)
            }
        } ?: CommonResponse(-1, "参数错误", null)

    }


    fun addTranslationV3(commonParam: CommonParam<*>): CommonResponse<TranslationRow?> {
        return parseRealParam(commonParam, CommitTranslationRowParam::class.java)?.let { realParam ->
            realParam.projectId?.let { projectId ->
                realParam.translationKey?.let { translationKey ->
                    realParam.translations?.let { translationList ->
                        val module = getModule(projectId)

                        if (realParam.action == "ADD") {
                            val queryTranslationList = mTranslationDao.queryTranslationByKey(translationKey, projectId)
                            if (queryTranslationList.isNotEmpty()) {//key已存在
                                CommonResponse(-1, "key已存在", TranslationRow(translationKey, queryTranslationList))
                            }else{
                                val optSuccess = mTranslationDao.batchImportTranslation(translationList, module?.moduleId ?: 0)
                                if (optSuccess) {
                                    CommonResponse(200, "", null)
                                } else {
                                    CommonResponse(-1, "数据库错误", null)
                                }
                            }
                        } else {//更新
                            val conflictTranslationList = realParam.oldTranslationKey?.let { mTranslationDao.queryTranslationByKey(realParam.oldTranslationKey, projectId) } ?: emptyList()
                            if (conflictTranslationList.isEmpty()) {//key没有变化或者没有冲突
                                translationList.forEach { it.translationKey = translationKey }
                                val optSuccess = mTranslationDao.batchImportTranslation(translationList, module?.moduleId ?: 0)
                                if (optSuccess) {
                                    CommonResponse(200, "", null)
                                } else {
                                    CommonResponse(-1, "数据库错误", null)
                                }
                            } else {
                                CommonResponse(-1, "key已存在", TranslationRow(translationKey, conflictTranslationList))
                            }
                        }
                    } ?: CommonResponse(200, "", null)
                } ?: CommonResponse(-1, "参数解析错误,key为空", null)
            } ?: CommonResponse(-1, "参数解析错误,projectId为空", null)
        } ?: CommonResponse(-1, "参数解析错误", null)
    }

    val addTranslationSB = java.lang.StringBuilder()
    fun addTranslationsV2(commonParam: CommonParam<*>): CommonResponse<List<Translation>> {

        return parseRealListPram(commonParam, Translation::class.java)?.let { translationList ->
            val failedList = mutableListOf<Translation>()
            return try {
                val startTime = System.currentTimeMillis()
                translationList.forEach { translation ->
                    translation.projectId?.let { projectId ->
                        translation.languageId?.let { languageId ->
                            translation.translationKey?.let { translationKey ->
                                translation.translationContent?.let { translationContent ->
                                    translation.translationContent = handleSingleQuotes(addTranslationSB, translationContent)
                                    val module = getModule(projectId) ?: return CommonResponse(-1, "添加模块失败", null)
                                    if (translation.translationId == null || translation.translationId == -1) {//新增翻译
                                        translation.moduleId = module.moduleId
                                        //先查询是否有冲突的key
                                        val translationDB = mTranslationDao.queryTranslationByKeyInLanguage(translationKey, projectId, languageId)
                                        if (translationDB.isNotEmpty()) {
                                            if (translation.forceAdd) {
                                                print(" ${translation.translationKey} 已存在 更新内容")
                                                translation.translationId = translationDB[0].translationId
                                                var newTranslationSuccess = mTranslationDao.updateTranslation(translation)
                                                if (!newTranslationSuccess) {
                                                    print("更新失败 $translation")
                                                }
                                            } else {
                                                if (translation.translationContent != translationDB[0].translationContent) {
                                                    translation.oldTranslationContent = translationDB[0].translationContent
                                                    failedList.add(translation)
                                                }
                                            }
                                        } else {//本地没有冲突直接添加
                                            val resultTranslation = mTranslationDao.addTranslation(translation)
                                            if (resultTranslation != null) {
                                                println(" ${translation.translationKey} 添加成功, content:${translation.translationContent}")
                                            } else {
                                                print(" ${translation.translationKey} 添加失败, content:${translation.translationContent}")
                                            }
                                        }


                                    } else {//translation Id不为空 更新翻译
                                        val translationDB = mTranslationDao.queryTranslationByKeyInLanguage(translationKey, projectId, languageId)
                                        //先查询一边本地是否有key冲突（防止改key冲突）
                                        if (translationDB.isNotEmpty()) {
                                            //有冲突
                                            val localTranslation = translationDB[0]
                                            if (translation.translationId == localTranslation.translationId) {//冲突的翻译是同一个，更新即可
                                                var updateSuccess = mTranslationDao.updateTranslation(translation)
                                                print("更新 $updateSuccess")
                                            } else {////冲突的翻译不是同一个
                                                print(" ${translation.translationKey} 已存在")
                                                return CommonResponse(-1, "Key冲突", emptyList())
                                            }
                                        } else {//key没有冲突，直接更新
                                            translation.moduleId = module.moduleId
                                            val success = mTranslationDao.updateTranslation(translation)
                                            if (success) {
                                                print(" ${translation.translationKey} 添加失败, content:${translation.translationContent}")
                                            } else {
                                                print(" ${translation.translationKey} 添加成功, content:${translation.translationContent}")
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
                val endTime = System.currentTimeMillis()
                print("添加翻译结束，花费时间：${endTime - startTime}ms")
                CommonResponse(200, "success", failedList)
            } catch (e: Exception) {
                CommonResponse(-1, e.message, emptyList())
            }
        } ?: CommonResponse(-1, "参数解析错误", emptyList())
    }

    fun updateTranslationsV2(commonParam: CommonParam<*>): CommonResponse<List<Translation>> {

        return parseRealListPram(commonParam, Translation::class.java)?.let { translationList ->
            if (translationList.isNullOrEmpty()) {
                CommonResponse(200, "", emptyList())
            } else {
                val failedList = mutableListOf<Translation>()
                try {
                    translationList.forEach {
                        val success = mTranslationDao.updateTranslation(it)
                        print("更新结果：$success $it")
                        if (!success) {
                            failedList.add(it)
                        }
                    }
                    CommonResponse(200, "", failedList)
                } catch (e: Exception) {
                    e.printStackTrace()
                    CommonResponse(200, e.message, failedList)
                }
            }

        } ?: CommonResponse(-1, "解析參數錯誤", emptyList())

    }

    val moduleCaches = HashMap<String, Module>()//暂时没有用，为以后增加模块扩展预留

    fun getModule(projectId: String): Module? {
        var module = moduleCaches[projectId]
        if (module == null) {
            println("未找到内存缓存，查找数据库")
            var moduleDB = mTranslationDao.queryModuleById(projectId)
            if (moduleDB.isEmpty()) {
                println("未找到数据库缓存，创建新module")
                module = Module()
                module.moduleName = ""
                module.projectId = projectId
                var addModuleResult = mTranslationDao.addModule(module.moduleName, module.projectId!!)

                if (addModuleResult != null) {
                    println("已创建module：${module.moduleId}")
                    module = addModuleResult
                    moduleCaches[projectId] = module

                }
            } else {
                module = moduleDB[0]
                println("数据库缓存module：${module.moduleId}")
                moduleCaches[projectId] = module
            }
        } else {
            println("内存缓存module：${module.moduleId}")
        }
        return module
    }

    fun mergeTranslationV2(commonParam: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(commonParam, MergeTranslationParam::class.java)?.let { realParam ->
            realParam.projectId?.let { projectId ->
                realParam.mainTranslationKey?.let { mainTranslationKey ->
                    val mainTranslationList = mTranslationDao.queryTranslationByKey(mainTranslationKey, projectId)
                    if (mainTranslationList.isNotEmpty()) {
                        realParam.translationToBeHideKeyList?.let { deleteTranslationKeyList ->
                            var commentBuilder = java.lang.StringBuilder()
                            deleteTranslationKeyList.forEach { deleteTranslationKey ->
                                val translationToBeHideList = mTranslationDao.queryTranslationByKey(deleteTranslationKey, projectId)
                                if (translationToBeHideList.isNotEmpty()) {
                                    translationToBeHideList.forEach { translationToBeHide ->
                                        translationToBeHide.hide = 1
                                        translationToBeHide.referto = mainTranslationKey
                                        val updateTranslationResult = mTranslationDao.updateTranslation(translationToBeHide)
                                        println("${translationToBeHide.translationKey} updateTranslationResult:$updateTranslationResult")
                                    }
                                }
                                val deleteSuccess = mTranslationDao.deleteTranslationByKey(deleteTranslationKey, projectId)
                                if (deleteSuccess) {
                                    commentBuilder.append(deleteTranslationKey).append(",")
                                }
                            }
                            mainTranslationList.forEach { mainTranslation ->
                                mainTranslation.comment = commentBuilder.toString()
                                mTranslationDao.updateTranslation(mainTranslation)
                            }
                            CommonResponse(200, "", null)
                        } ?: CommonResponse(-1, "没有要删除的翻译", null)
                    } else {
                        CommonResponse(-1, "未查询到主翻译", null)
                    }
                } ?: CommonResponse(-1, "没有主翻译", null)
            } ?: CommonResponse(-1, "项目id为空", null)
        } ?: CommonResponse(-1, "参数解析出错", null)
    }

    fun batchImportTranslation(commonParam: CommonParam<*>): CommonResponse<Void> {
        return parseRealListPram(commonParam, Translation::class.java)?.let { translationList ->
            if (translationList.isNotEmpty()) {
                getModule(translationList.first().projectId ?: "")?.let { module ->
                    mTranslationDao.batchImportTranslation(translationList, module.moduleId)
                    CommonResponse(200, "", null)
                } ?: CommonResponse(-1, "添加模块失败", null)


            } else {
                CommonResponse(200, "", null)
            }
        } ?: CommonResponse(-1, "参数解析出错", null)
    }

    fun copyTranslation(param: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(param, CopyTranslationParam::class.java)?.let { realParam ->

            realParam.targetProjectId?.let { targetProjectId ->
                println("目标项目：$targetProjectId")
                realParam.sourceProjectId?.let { sourceProjectId ->
                    println("源项目：$sourceProjectId")
                    getModule(targetProjectId)?.let { module ->
                        val sourceTranslationKeyList = realParam.mSourceTranslationKeyList ?:listOf(
                            "hs_device_offline_time",
                            "hs_login_failed",
                            "hs_language_zh",
                            "hs_language_es",
                            "hs_language_pt",
                            "hs_language_pl",
                            "hs_language_vi",
                            "hs_language_ko",
                            "hs_language_jp",
                            "hs_language_en",
                            "hs_Flipped",
                            "hs_Unflipped",
                            "hs_area_select_is_empty",
                            "hs_timezone_set_successful",
                            "hs_network_switch_prompt",
                            "hs_device_wifi_switching",
                            "hs_return_to_device_list",
                            "hs_event_type_picture_change_tip",
                            "hs_event_type_human_appeared_tip",
                            "hs_MotionDetectCars",
                            "hs_AreaAll",
                            "hs_autoTrackOnTip",
                            "hs_no_sim",
                            "hs_battery",
                            "hs_power",
                            "hs_solar",
                            "hs_setting_minute",
                            "hs_common_every_day",
                            "hs_Anomaly",
                            "hs_VideoSettings",
                            "hs_detection_time_range",
                            "hs_TimeSlot",
                            "hs_set_repentition_rule",
                            "hs_detecting_delete_tip",
                            "hs_all_day_recording_save",
                            "hs_all_day_detection_save",
                            "hs_detectopn_time_table",
                            "hs_AddDetectingTime",
                            "sound_select",
                            "hs_sound_alarm_desc_android",
                            "hs_light_alarm_desc_android",
                            "hs_device_setting_sound_counts_android",
                            "hs_off_push_tips",
                            "hs_this_account",
                            "hs_always_on_dialog_desc",
                            "hs_power_saving_dialog_desc",
                            "hs_timed_sleep_dialog_desc",
                            "hs_constant_power_dialog_desc",
                            "hs_device_smarthub_connect_router",
                            "hs__already_activated",
                            "hs_not_activated",
                            "hs_all_day_detection",
                            "hs_scheduled_detection",
                            "hs_event_type_car_appeared",
                            "hs_AddAlarmTimeSlot",
                            "hs_sunday",
                            "hs_monday",
                            "hs_tuesday",
                            "hs_wednesday",
                            "hs_thursday",
                            "hs_friday",
                            "hs_saturday",
                            "device_setting_storage_no_find_sd",
                            "hs_choose_a_week",
                            "hs_common_time_limit",
                            "hs_adaptive_realtime_1_5fps",
                            "hs_standard_definition",
                            "hs_stream_low",
                            "hs_device_frame_rate",
                            "hs_screen_watermark",
                            "hs_smart_mode_switch_desc",
                            "hs_constant_power_switch_desc",
                            "hs_always_on_switch_desc",
                            "hs_power_saving_switch_desc",
                            "hs_timed_sleep_switch_desc",
                            "hs_PressToTalk",
                            "hs_Voice15Sec_android",
                            "hs_device_require_micro_title_android",
                            "hs_device_require_micro_desc_android",
                            "hs_app_needs_access_to_your_microphone",
                            "hs_TapToRerecord",
                            "hs_RemainingSec_android",
                            "hs_RecordTooShort",
                            "hs_week_select_limit",
                            "hs_current_version",
                            "hs_upgrade_failed",
                            "hs_avoid_power_off",
                            "hs_upgrade_error",
                            "hs_low_battery_tip_android",
                            "hs_account_email_invalid",
                            "hs_email_password_require_title",
                            "hs_email_password_require_content",
                            "hs_email_server_explain_title",
                            "hs_email_server_explain_content",
                            "hs_common_auto",
                            "hs_account_change_password_success",
                            "hs_account_confirm_password_error",
                            "hs_device_reset_setting_factory_confirm",
                            "hs_device_reset_setting_simple_confirm",
                            "hs_sdcar_format_failure",
                            "hs_sdcar_format_completed",
                            "hs_Format_Desc",
                            "hs_operate_success",
                            "hs_ptz_point_name_empty_alert",
                            "hs_common_loading",
                            "hs_PlayCount",
                            "hs_DeviceVolumeMicrophone",
                            "hs_DeviceVolumeSpeaker",
                            "hs_select_wifi_nearby",
                            "hs_High",
                            "hs_select_add_type",
                            "hs_power_on_and_press_reset",
                            "hs_device_mail_port",
                            "hs_network_config",
                            "hs_device_networking_failure",
                            "hs_device_check_sim",
                            "hs_device_check_sim_tips_1",
                            "hs_device_check_sim_tips_2",
                            "hs_device_check_sim_tips_3",
                            "hs_device_check_sim_tips_4",
                            "hs_device_networking_success",
                            "hs_device_added_by_another_account",
                            "hs_smarthub_pair_step1",
                            "hs_smarthub_pair_step2",
                            "hs_device_require_location_permission_title_android",
                            "hs_device_require_location_permission_content_android",
                            "hs_device_require_location_permission_state_android",
                            "hs_device_require_bluetooth_permission_title_android",
                            "hs_device_require_bluetooth_permission_content_android",
                            "hs_go_to_settings",
                            "hs_account_change_password",
                            "hs_cevice_internal_storage",
                            "hs_cevice_sd_card",
                            "hs_setting_sync_time",
                            "hs_off_push_tips2",
                            "hs_device_configure_wifi",
                            "hs_device_pair_by_qr_code",
                            "hs_device_networking_fail",
                            "hs_device_confirmation",
                            "hs_device_smart_network_prepare",
                            "hs_hub_nearby_devices",
                            "hs_common_none",
                            "hs_heard_device_beep",
                            "hs_pair_added_device",
                            "hs_device_added_by_another_hub",
                            "hs_device_added_by_another_account_android",
                            "hs_ask_device_owner_to_share_android",
                            "hs_home_device_share",
                            "hs_common_qrcode_is_invalid",
                            "hs_device_add_hub_ipc_success",
                            "hs_device_has_been_bound",
                            "hs_common_camera",
                            "hs_device_living_room",
                            "hs_device_bedroom",
                            "hs_device_nursery",
                            "hs_device_doorway",
                            "hs_device_courtyard",
                            "hs_device_carport",
                            "hs_device_checkstand",
                            "hs_device_warehouse",
                            "hs_device_pair_hub_ipc_failed_reason_helper_text",
                            "hs_device_pair_failed_reason_1",
                            "hs_device_pair_failed_reason_2",
                            "hs_device_reset_tips2_android",
                            "hs_device_change_binding",
                            "hs_device_verify_password",
                            "hs_device_password",
                            "hs_device_init_setting",
                            "hs_device_create_password",
                            "hs_device_launch_tips_2_connected",
                            "hs_apn_setting_android",
                            "hs_sharing_limit_exceeded",
                            "hs_device_require_camera_permission_title_android",
                            "hs_camera_permission_content_android",
                            "hs_device_not_exist",
                            "hs_device_require_album_title_android",
                            "hs_device_require_album_desc_android",
                            "hs_image_recognition_failed",
                            "hs_common_no_qrcode",
                            "hs_device_connect_failed_retry_tips",
                            "hs_search_nearby_ipc",
                            "hs_device_add_confirm",
                            "hs_smart_hub_bind_ipc_alarm",
                            "hs_device_add_time",
                            "hs_adding_device",
                            "hs_device_pair_turn_on_wifi_alert",
                            "hs_device_wifi_limit_android",
                            "hs_device_connect_device_ap_helper_text_android",
                            "hs_device_ap_not_found_helper_text",
                            "hs_device_no_voice_prompt_help_text1",
                            "hs_device_no_voice_prompt_help_text2",
                            "hs_device_wifi_recognize_success",
                            "hs_common_scan_failed_reason",
                            "hs_not_smart_hub_qr_code",
                            "hs_qr_code_invaid",
                            "hs_common_device_param_exception",
                            "hs_network_error"
                        )

                        sourceTranslationKeyList.let { sourceKeyList ->
                            println("目标key数量：${sourceKeyList.size}")
                            var enTranslationList: List<Translation>? = null
                            mTranslationDao.getLanguageList(targetProjectId).let { targetProjectLanguageList ->
                                mTranslationDao.getLanguageList(sourceProjectId).let { sourceProjectLanguageList ->
                                    var sourceEnLanguageId = 0
                                    sourceProjectLanguageList.forEach sourceProjectLanguageListForEach@{
                                        if (it.languageName == "en") {
                                            sourceEnLanguageId = it.languageId!!
                                            return@sourceProjectLanguageListForEach
                                        }
                                    }
                                    val unHandleLanguageList = mutableListOf<Language>()//源项目没有的翻译

                                    val sourceLangeIdToNameMap = sourceProjectLanguageList.associate { it.languageId to it.languageName }
                                    val targetLanguageNameToIdMap = targetProjectLanguageList.associate { it.languageName to it.languageId }

                                    targetProjectLanguageList.forEach forEach1@{ targetLanguage ->
                                        var hasLanguage = false
                                        sourceProjectLanguageList.forEach forEach2@{ sourceLanguage ->
                                            if (targetLanguage.languageName == sourceLanguage.languageName) {
                                                hasLanguage = true

                                            }
                                        }

                                        if (!hasLanguage) {
                                            unHandleLanguageList.add(targetLanguage)
                                        }
                                    }
                                    println("缺少的目标语言")
                                    unHandleLanguageList.forEach {
                                        println(",${it.languageName}")
                                    }
                                    sourceKeyList.forEach { sourceTranslationKey ->
                                        println("查询翻译：$sourceTranslationKey")
                                        mTranslationDao.queryTranslationByKey(sourceTranslationKey, sourceProjectId).toMutableList().let { sourceTranslationList ->
                                            val translateService = TranslateOptions.getDefaultInstance().service
                                            var enTranslationContent: String? = null
                                            sourceTranslationList.forEach { translation ->
                                                if (translation.languageId == sourceEnLanguageId) {
                                                    enTranslationContent = translation.translationContent
                                                }
                                                val languageName = sourceLangeIdToNameMap.get(translation.languageId)
                                                val languageId = targetLanguageNameToIdMap.get(languageName)
                                                translation.languageId = languageId
                                                translation.projectId = targetProjectId
                                                translation.moduleId = module.moduleId

                                            }
                                            enTranslationContent?.let {
                                                if (sourceTranslationList.isNotEmpty()) {
                                                    //添加缺少的目标语言

                                                    System.setProperty("http.proxyHost", proxyHost)
                                                    System.setProperty("http.proxyPort", proxyPort)
                                                    System.setProperty("https.proxyHost", proxyHost)
                                                    System.setProperty("https.proxyPort", proxyPort)

                                                    unHandleLanguageList.forEach { missingLanguage ->
                                                        val translateResult =
                                                            translateService.translate(
                                                                enTranslationContent,
                                                                Translate.TranslateOption.sourceLanguage("en"),
                                                                Translate.TranslateOption.targetLanguage(missingLanguage.languageName),
                                                                Translate.TranslateOption.format("text")
                                                            )
                                                        println("翻译结果 ：${translateResult.translatedText}")
                                                        val newTranslation = Translation().apply {
                                                            languageId = missingLanguage.languageId
                                                            projectId = targetProjectId
                                                            moduleId = module.moduleId
                                                            translationKey = sourceTranslationKey
                                                            translationContent = translateResult.translatedText
                                                        }
                                                        sourceTranslationList.add(newTranslation)
                                                    }
                                                }
                                            }
                                            if (sourceTranslationList.isNotEmpty()) {
                                                mTranslationDao.batchImportTranslation(sourceTranslationList, module.moduleId)
                                            }
                                        }
                                    }
                                }


//                                targetProjectLanguageList.forEach { targetLanguage ->
//
//
//                                    mTranslationDao.queryTranslationByLanguage(targetLanguage.languageId!!, sourceProjectId).let { sourceTranslationOfLanguageList ->
//                                        if (targetLanguage.languageName == "en") {
//                                            enTranslationList = sourceTranslationOfLanguageList
//                                        }
//                                        if (sourceTranslationOfLanguageList.isEmpty()) {
//                                            unHandleLanguageList.add(targetLanguage)
//                                        } else {
//                                            sourceTranslationOfLanguageList.forEach { translation ->
//                                                translation.languageId = targetLanguage.languageId
//                                                translation.moduleId = module.moduleId
//                                                translation.projectId = targetProjectId
//                                                translation.translationId = null
//                                            }
//                                            mTranslationDao.batchImportTranslation(sourceTranslationOfLanguageList, module.moduleId)
//                                        }
//                                    }
//
//                                }
//                                if (unHandleLanguageList.isNotEmpty()) {
//                                    enTranslationList?.let { enTranslationList ->
//                                        val translateService = TranslateOptions.getDefaultInstance().service
//                                        val translationContents = List(enTranslationList.size) { enTranslationList[it].translationContent }
//
//                                        unHandleLanguageList.forEach { targetLanguage ->
//                                            val translateResult =
//                                                translateService.translate(
//                                                    translationContents,
//                                                    Translate.TranslateOption.sourceLanguage("en"),
//                                                    Translate.TranslateOption.targetLanguage(targetLanguage.languageName),
//                                                    Translate.TranslateOption.format("text")
//                                                )
//                                            println("翻译结果：${translateResult.size}")
//                                            if (!translateResult.isEmpty() && translateResult.size == enTranslationList.size) {
//                                                val targetLanguageTranslationList = mutableListOf<Translation>()
//                                                for(index in enTranslationList.indices){
//
//                                                    val enTranslation =  enTranslationList[index]
//                                                    val translationResultItem = translateResult.get(index)
//
//                                                    val newTranslation = Translation().apply {
//                                                        this.translationContent = translationResultItem.translatedText
//                                                        this.languageId = targetLanguage.languageId
//                                                        this.projectId = targetProjectId
//                                                        this.moduleId = module.moduleId
//                                                        this.translationKey = enTranslation.translationKey
//                                                    }
//                                                    targetLanguageTranslationList.add(newTranslation)
//                                                }
//                                                if(targetLanguageTranslationList.isNotEmpty()){
//                                                    val success = mTranslationDao.batchImportTranslation(targetLanguageTranslationList,module.moduleId)
//                                                    if(success){
//                                                        println("增量翻译成功")
//                                                    }
//
//                                                }
//                                            }
//                                        }
//
//                                    }
//                                }
                            }
                        }
                    }


                    CommonResponse(200, "", null)
                } ?: CommonResponse(-1, "参数解析出错,缺少目标项目Id", null)
            } ?: CommonResponse(-1, "参数解析出错,缺少目标项目Id", null)
        } ?: CommonResponse(-1, "参数解析出错", null)
    }

    /**
     * 分页查询Translation数据，按translationKey分组返回TranslationRow列表
     * @param param 包含分页参数的CommonParam
     * @return 分页结果，包含TranslationRow列表和分页信息
     */
    fun getTranslationRowsPaginated(param: CommonParam<*>): CommonResponse<PaginatedResponse<TranslationRow>> {
        return parseRealParam(param, PaginatedTranslationParam::class.java)?.let { realParam ->
            try {
                val startTime = System.currentTimeMillis()
                
                // 计算偏移量
                val offset = realParam.page * realParam.size
                
                // 获取分页数据
                val translationRows = mTranslationDao.getTranslationRowsPaginated(
                    realParam.projectId,
                    offset,
                    realParam.size
                )
                
                // 获取总记录数
                val totalElements = mTranslationDao.getTotalTranslationKeysCount(realParam.projectId)
                
                // 计算总页数
                val totalPages = ((totalElements + realParam.size - 1) / realParam.size).toInt()
                
                // 构建分页响应
                val paginatedResponse = PaginatedResponse(
                    content = translationRows,
                    page = realParam.page,
                    size = realParam.size,
                    totalElements = totalElements,
                    totalPages = totalPages,
                    hasNext = realParam.page < totalPages - 1,
                    hasPrevious = realParam.page > 0
                )
                
                val endTime = System.currentTimeMillis()
                println("分页查询完成：projectId=${realParam.projectId}, page=${realParam.page}, size=${realParam.size}, totalElements=$totalElements, 查询耗时：${endTime - startTime}ms")
                
                CommonResponse(200, "查询成功", paginatedResponse)
            } catch (e: Exception) {
                e.printStackTrace()
                CommonResponse(-1, "查询失败：${e.message}", null)
            }
        } ?: CommonResponse(-1, "参数解析错误", null)
    }
}