package com.huanchengfly.tieba.post.core.network.retrofit

import com.huanchengfly.tieba.post.core.network.Error
import com.huanchengfly.tieba.post.core.network.model.CommonResponse
import com.huanchengfly.tieba.post.core.network.exception.NoConnectivityException
import com.huanchengfly.tieba.post.core.network.model.web.ErrorBean
import com.huanchengfly.tieba.post.core.network.retrofit.body.MyMultipartBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.FormBody
import okio.Buffer

fun FormBody.containsEncodedName(name: String): Boolean {
    repeat(size) {
        if (encodedName(it) == name) return true
    }
    return false
}

inline fun FormBody.forEach(block: (String, String) -> Unit) {
    repeat(size) {
        block(encodedName(it), encodedValue(it))
    }
}

fun FormBody.raw() =
    StringBuilder().apply {
        repeat(size) {
            if (it != 0) append('&')
            append(encodedName(it))
            append('=')
            append(encodedValue(it))
        }
    }.toString()

fun FormBody.sortedEncodedRaw(separator: Boolean = true): String {
    val nameAndValue = mutableListOf<String>()
    repeat(size) {
        nameAndValue.add("${encodedName(it)}=${encodedValue(it)}")
    }
    return if (separator) nameAndValue.sorted().joinToString(separator = "&")
    else nameAndValue.sorted().joinToString(separator = "")
}

fun FormBody.sortedRaw(separator: Boolean = true): String {
    val nameAndValue = mutableListOf<String>()
    repeat(size) {
        nameAndValue.add("${name(it)}=${value(it)}")
    }
    return if (separator) nameAndValue.sorted().joinToString(separator = "&")
    else nameAndValue.sorted().joinToString(separator = "")
}

fun FormBody.Builder.addAllEncoded(formBody: FormBody): FormBody.Builder {
    with(formBody) {
        repeat(size) {
            addEncoded(encodedName(it), encodedValue(it))
        }
    }
    return this
}

internal fun MyMultipartBody.Builder.addAllParts(myMultipartBody: MyMultipartBody): MyMultipartBody.Builder {
    repeat(myMultipartBody.size) {
        val part = myMultipartBody.part(it)
        addPart(part)
    }
    return this
}

internal fun MyMultipartBody.contains(name: String): Boolean {
    repeat(size) {
        if (part(it).name() == name) return true
    }
    return false
}

internal fun MyMultipartBody.Part.contentDisposition(): Map<String, String> {
    val headersMap = mutableMapOf<String, String>()
    headers?.toString()?.split(";")?.forEach {
        val header = it.trim().split("=").toMutableList()
        if (header.size >= 2) {
            val name = header.removeAt(0).trim()
            val value = header.joinToString("=").trim().trim('"')
            headersMap[name] = value
        }
    }
    return headersMap
}

internal fun MyMultipartBody.Part.name(): String? {
    return contentDisposition()["name"]
}

internal fun MyMultipartBody.Part.fileName(): String? {
    return contentDisposition()["filename"]
}

internal fun MyMultipartBody.newBuilder(): MyMultipartBody.Builder =
    MyMultipartBody.Builder(boundary).setType(type)

internal fun MyMultipartBody.sort(): MyMultipartBody {
    val builder = newBuilder()
    val fileParts = mutableListOf<MyMultipartBody.Part>()
    parts.forEach {
        if (it.fileName() != null) {
            fileParts.add(it)
        }
    }
    parts.filterNot { it in fileParts }.sortedBy { it.name() }.forEach { builder.addPart(it) }
    if (fileParts.isNotEmpty()) fileParts.sortedBy { it.fileName() }.forEach { builder.addPart(it) }
    return builder.build()
}

internal fun MyMultipartBody.Part.readString(): String {
    val buffer = Buffer()
    body.writeTo(buffer)
    return buffer.readUtf8()
}

internal typealias ParamExpression = Pair<String, () -> String?>

internal inline fun Array<out ParamExpression>.forEachNonNull(action: (String, String) -> Unit) {
    forEach { (name, valueExpression) ->
        val value = valueExpression()
        if (value != null) {
            action(name, value)
        }
    }
}

internal inline fun List<ParamExpression>.forEachNonNull(action: (String, String) -> Unit) {
    forEach { (name, valueExpression) ->
        val value = valueExpression()
        if (value != null) {
            action(name, value)
        }
    }
}

fun ErrorBean.getError(): CommonResponse {
    return CommonResponse(
        errorCode = errorCode.toIntOrNull() ?: Error.ERROR_UNKNOWN,
        errorMsg = errorMsg
    )
}

val com.huanchengfly.tieba.post.core.network.model.protos.Error?.commonResponse
    get() = CommonResponse(
        errorCode = this?.error_code ?: Error.ERROR_UNKNOWN,
        errorMsg = this?.error_msg.orEmpty()
    )

@Throws(NoConnectivityException::class)
suspend inline fun <T> Flow<T>.firstOrThrow(): T = firstOrNull() ?: throw NoConnectivityException()
