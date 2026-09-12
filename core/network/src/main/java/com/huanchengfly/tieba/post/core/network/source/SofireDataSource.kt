package com.huanchengfly.tieba.post.core.network.source

import android.util.Base64
import com.huanchengfly.tieba.post.core.network.exception.SofireException
import com.huanchengfly.tieba.post.core.network.model.SofireResponseData
import com.huanchengfly.tieba.post.core.network.retrofit.RetrofitTiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.firstOrThrow
import com.huanchengfly.tieba.post.core.network.retrofit.interceptors.ConnectivityInterceptor
import com.huanchengfly.tieba.post.core.network.session.DeviceInfoProvider
import com.huanchengfly.tieba.post.core.network.util.NetworkMonitor
import com.huanchengfly.tieba.post.core.network.util.gzipCompress
import com.huanchengfly.tieba.post.core.network.util.rc442Crypt
import com.huanchengfly.tieba.post.utils.StringUtil.toMD5
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlin.random.Random

interface SofireDataSource {
    suspend fun fetchZid(): String
}

/**
 * [Retrofit] backed [SofireDataSource]
 *
 * From com.huanchengfly.tieba.post.utils.SofireUtils
 *
 * @since 4.0.0 beta.1
 * @author HuanChengFly
 * @author 0Ranko0P
 * */
internal class RetrofitSofireDataSource @Inject constructor(
    private val deviceInfoProvider: DeviceInfoProvider,
    private val retrofitTiebaApi: RetrofitTiebaApi,
    private val networkMonitor: NetworkMonitor,
): SofireDataSource {

    companion object {
        const val DEFAULT_APP_KEY = "200033"
        const val DEFAULT_SECRET_KEY = "ea737e4f435b53786043369d2e5ace4f"

        @Serializable
        data class SofireRequestBody(
            @SerialName("module_section")
            val moduleSection: List<Map<String, String>>
        )

        fun generateRandomString(length: Int): String {
            val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            return (1..length)
                .map { Random.nextInt(0, charPool.size) }
                .map(charPool::get)
                .joinToString("")
        }
    }

    override suspend fun fetchZid(): String {
        val appKey = DEFAULT_APP_KEY
        val secKey = DEFAULT_SECRET_KEY
        val cuid = "${deviceInfoProvider.uuid.toMD5().uppercase()}|0"
        val cuidMd5 = cuid.toMD5().lowercase()
        val currTime = "${System.currentTimeMillis() / 1000}"
        val reqBody =
            Json.encodeToString(SofireRequestBody(listOf(mapOf("zid" to cuid)))).gzipCompress()
        val randomKey = generateRandomString(16)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = IvParameterSpec(0.toChar().toString().repeat(16).encodeToByteArray())
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(randomKey.toByteArray(), "AES"), iv)
        val encBody = cipher.doFinal(reqBody)
        val reqBodyMd5Digest = MessageDigest.getInstance("MD5").apply { update(reqBody) }.digest()
        val finalBody =
            (encBody + reqBodyMd5Digest).toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val headers = mapOf(
            "Pragma" to "no-cache",
            "Accept" to "*/*",
            "Accept-Language" to Locale.getDefault().language,
            "x-device-id" to cuidMd5,
            "x-client-src" to "src",
            "User-Agent" to "x6/$appKey/12.35.1.0/4.4.1.3",
            "x-sdk-ver" to "sofire/3.5.9.6",
            "x-plu-ver" to "x6/4.4.1.3",
            "x-app-ver" to "com.baidu.tieba/12.35.1.0",
            "x-api-ver" to "33"
        )
        val pathMd5 = listOf(appKey, currTime, secKey).joinToString("").toMD5().lowercase()
        val skey = Base64.encodeToString(
            rc442Crypt(randomKey.encodeToByteArray(), cuidMd5.encodeToByteArray(), 16, 32),
            Base64.DEFAULT
        )
        val url = "https://sofire.baidu.com/c/11/z/100/$appKey/$currTime/$pathMd5"
        return retrofitTiebaApi.SOFIRE_API
            .post(url, skey, finalBody, headers)
            .map {
                val resSkey = rc442Crypt(
                    Base64.decode(it.skey, Base64.DEFAULT),
                    cuidMd5.encodeToByteArray(),
                    16,
                    32
                )
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(resSkey, "AES"), iv)
                val decode = Base64.decode(it.data, Base64.DEFAULT).dropLast(16).toByteArray()
                val json = Json { ignoreUnknownKeys = true }
                val decryptData = json.decodeFromString<SofireResponseData>(
                    cipher.doFinal(decode).decodeToString()
                )
                decryptData.token
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                if (networkMonitor.isCurrentlyValidated()) {
                    throw SofireException(e)
                } else {
                    throw ConnectivityInterceptor.wrapException(networkMonitor, e)
                }
            }
            .firstOrThrow()
    }
}