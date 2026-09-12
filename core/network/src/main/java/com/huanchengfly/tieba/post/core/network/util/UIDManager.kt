package com.huanchengfly.tieba.post.core.network.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.huanchengfly.tieba.post.core.network.session.DeviceInfoProvider
import com.huanchengfly.tieba.post.core.network.session.OAIDProvider
import com.huanchengfly.tieba.post.core.network.util.helios.Base32
import com.huanchengfly.tieba.post.core.network.util.helios.Hasher
import com.huanchengfly.tieba.post.utils.StringUtil.toMD5
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UID Manager
 *
 * From com.huanchengfly.tieba.post.utils.UIDUtil
 * */
@Singleton
class UIDManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val oaidProvider: OAIDProvider
) {

    @SuppressLint("HardwareIds")
    fun getAndroidId(defaultValue: String): String {
        val androidId =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return androidId ?: defaultValue
    }

    fun getOAID(): String {
        if (oaidProvider.getEncodedOAID().isBlank()) return ""
        val raw = "A10-${oaidProvider.getEncodedOAID()}-"
        val sign = Base32.encode(Hasher.hash(raw.toByteArray()))
        return "$raw$sign"
    }

    fun getAid(): String {
        val raw = "com.helios" + getAndroidId("000000000") + deviceInfoProvider.uuid
        val bytes = getSHA1(raw)
        val encoded = Base32.encode(bytes)
        val rawAid = "A00-$encoded-"
        val sign = Base32.encode(Hasher.hash(rawAid.toByteArray()))
        return "$rawAid$sign"
    }

    private fun getSHA1(str: String): ByteArray {
        var sha1: ByteArray = "".toByteArray()
        try {
            val digest = MessageDigest.getInstance("SHA1")
            sha1 = digest.digest(str.toByteArray(StandardCharsets.UTF_8))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sha1
    }

    val cUID: String by lazy { "com.baidu${getAndroidId("")}".toMD5().uppercase() }

    /**
     * From com.huanchengfly.tieba.post.utils.CuidUtils
     *
     * @since 4.0.0 dev 10
     * */
    val newCUID: String by lazy {
        val encode = Base32.encode(Hasher.hash(cUID.toByteArray()))
        "$cUID|V$encode"
    }

    val finalCUID: String
        get() {
            var imei: CharSequence = deviceInfoProvider.imei
            if (TextUtils.isEmpty(imei)) {
                imei = "0"
            } else {
                imei = StringBuffer(imei).reverse()
            }
            return "$cUID|$imei"
        }
}