package com.huanchengfly.tieba.post.api.models

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 官方一键签到Bean
 *
 * 服务器返回的垃圾数据需要特殊处理
 *
 * @see MSignBeanSerializer
 * */
@Serializable(with = MSignBeanSerializer::class)
abstract class MSignBean {

    abstract val ctime: Int

    abstract val error: Error

    abstract val errorCode: String

    abstract val isTimeout: String

    abstract val logid: Long

    abstract val serverTime: String

    abstract val showDialog: String

    abstract val signNotice: String

    abstract val time: Int

    abstract val timeoutNotice: String

    @Serializable
    data class Error(
        val errmsg: String,
        val errno: Int,
        val usermsg: String
    )

    @Serializable
    data class Info(
        @SerialName("cur_score")
        val curScore: String,
        val error: Error,
        @SerialName("forum_id")
        val forumId: Long,
        @SerialName("forum_name")
        val forumName: String,
        @SerialName("is_filter")
        val isFilter: String,
        @SerialName("is_on")
        val isOn: String,
        @SerialName("sign_day_count")
        val signDayCount: String,
        val signed: String
    ) {
        @Serializable
        data class Error(
            @SerialName("err_no")
            val errNo: String,
            val errmsg: String,
            val usermsg: String
        )
    }
}

@Serializable
data class MSignSuccess(
    override val ctime: Int,
    override val error: Error,
    @SerialName("error_code")
    override val errorCode: String,
    val info: List<Info>,
    @SerialName("is_timeout")
    override val isTimeout: String,
    override val logid: Long,
    @SerialName("server_time")
    override val serverTime: String,
    @SerialName("show_dialog")
    override val showDialog: String,
    @SerialName("sign_notice")
    override val signNotice: String,
    override val time: Int,
    @SerialName("timeout_notice")
    override val timeoutNotice: String
): MSignBean()

@Serializable
data class MSignFailed(
    override val ctime: Int,
    override val error: Error,
    @SerialName("error_code")
    override val errorCode: String,
    val info: String,
    @SerialName("is_timeout")
    override val isTimeout: String,
    override val logid: Long,
    @SerialName("server_time")
    override val serverTime: String,
    @SerialName("show_dialog")
    override val showDialog: String,
    @SerialName("sign_notice")
    override val signNotice: String,
    override val time: Int,
    @SerialName("timeout_notice")
    override val timeoutNotice: String
): MSignBean()

object MSignBeanSerializer : JsonContentPolymorphicSerializer<MSignBean>(MSignBean::class) {

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<MSignBean> {
        val infoElement = element.jsonObject["info"] ?: throw SerializationException("Element [info] not exists!")
        // Info type is string, MSign failed
        val infoIsString = try {
            infoElement.jsonPrimitive.isString
        } catch (_: IllegalArgumentException) {
            false
        }

        // Return based on info element's type
        return if (infoIsString) MSignFailed.serializer() else MSignSuccess.serializer()
    }
}