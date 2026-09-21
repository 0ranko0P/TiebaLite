package com.huanchengfly.tieba.post.core.data.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager

/**
 * 获取IMEI
 *
 * 原位置: com.huanchengfly.tieba.post.utils.MobileInfoUtil
 *
 * @author HuanChengFly
 *
 * @since 3.8.1 α
 * */
@Suppress("DEPRECATION")
internal object MobileInfoUtil {

    @SuppressLint("HardwareIds", "MissingPermission")
    fun getIMEI(context: Context): String {
        val DEFAULT_IMEI = "000000000000000"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return DEFAULT_IMEI
        }
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return telephonyManager.deviceId
        } catch (e: Throwable) {
            e.printStackTrace()
            return DEFAULT_IMEI
        }
    }
}