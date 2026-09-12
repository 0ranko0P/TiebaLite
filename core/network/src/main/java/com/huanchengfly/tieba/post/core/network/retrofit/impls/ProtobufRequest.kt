package com.huanchengfly.tieba.post.core.network.retrofit.impls

import android.os.Build
import com.huanchengfly.tieba.post.core.common.ktx.toJson
import com.huanchengfly.tieba.post.core.network.ClientVersion
import com.huanchengfly.tieba.post.core.network.Param
import com.huanchengfly.tieba.post.core.network.model.OAID
import com.huanchengfly.tieba.post.core.network.model.protos.AppPosInfo
import com.huanchengfly.tieba.post.core.network.model.protos.CommonRequest
import com.huanchengfly.tieba.post.core.network.model.protos.frsPage.AdParam
import com.huanchengfly.tieba.post.core.network.retrofit.body.MyMultipartBody
import com.huanchengfly.tieba.post.core.network.util.CacheUtil.base64Encode
import com.squareup.wire.Message
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val BOUNDARY = "--------7da3d81520810*"

internal fun MixedTiebaApiImpl.buildProtobufRequestBody(
    data: Message<*, *>,
    clientVersion: ClientVersion = ClientVersion.TIEBA_V11,
    needSToken: Boolean = true,
): MyMultipartBody {
    return MyMultipartBody.Builder(BOUNDARY)
        .apply {
            setType(MyMultipartBody.FORM)
            if (clientVersion != ClientVersion.TIEBA_V12 && clientVersion != ClientVersion.TIEBA_V12_POST) {
                addFormDataPart(Param.CLIENT_VERSION, clientVersion.version)
            }
            if (needSToken) {
                val sToken = credentialProvider.getSToken()
                if (sToken != null) addFormDataPart(Param.STOKEN, sToken)
            }
            addFormDataPart("data", "file", data.encode().toRequestBody())
        }
        .build()
}

internal fun buildAdParam(
    load_count: Int = 0,
    refresh_count: Int = 4,
    yoga_lib_version: String? = "1.0"
): AdParam {
    return AdParam(
        load_count = load_count,
        refresh_count = refresh_count,
        yoga_lib_version = yoga_lib_version
    )
}

internal fun buildAppPosInfo(): AppPosInfo {
    return AppPosInfo(
        addr_timestamp = 0L,
        ap_connected = true,
        ap_mac = "02:00:00:00:00:00",
        asp_shown_info = "",
        coordinate_type = "BD09LL"
    )
}

internal fun MixedTiebaApiImpl.buildCommonRequest(
    clientVersion: ClientVersion = ClientVersion.TIEBA_V11,
    bduss: String? = null,
    stoken: String? = null,
    tbs: String? = null,
): CommonRequest = when (clientVersion) {
    ClientVersion.TIEBA_V11 -> {
        CommonRequest(
            BDUSS = bduss ?: credentialProvider.getBduss(),
            _client_id = clientConfigProvider.getClientId() ?: retrofitTiebaApi.randomClientId,
            _client_type = 2,
            _client_version = clientVersion.version,
            _os_version = "${Build.VERSION.SDK_INT}",
            _phone_imei = deviceInfoProvider.imei,
            _timestamp = System.currentTimeMillis(),
            brand = Build.BRAND,
            c3_aid = uidManager.getAid(),
            cuid = uidManager.newCUID,
            cuid_galaxy2 = uidManager.newCUID,
            cuid_gid = "",
            from = "1024324o",
            is_teenager = 0,
            lego_lib_version = "3.0.0",
            model = Build.MODEL,
            net_type = 1,
            oaid = OAID(oaidProvider).toJson(),
            pversion = "1.0.3",
            sample_id = clientConfigProvider.getSampleId(),
            stoken = stoken ?: credentialProvider.getSToken(),
        )
    }

    ClientVersion.TIEBA_V12 -> {
        CommonRequest(
            BDUSS = credentialProvider.getBduss(),
            _client_id = clientConfigProvider.getClientId() ?: retrofitTiebaApi.randomClientId,
            _client_type = 2,
            _client_version = clientVersion.version,
            _os_version = "${Build.VERSION.SDK_INT}",
            _phone_imei = deviceInfoProvider.imei,
            _timestamp = System.currentTimeMillis(),
            active_timestamp = clientConfigProvider.activeTimestamp,
            android_id = base64Encode(uidManager.getAndroidId("000")),
            brand = Build.BRAND,
            c3_aid = uidManager.getAid(),
            cmode = 1,
            cuid = uidManager.newCUID,
            cuid_galaxy2 = uidManager.newCUID,
            cuid_gid = "",
            event_day = SimpleDateFormat("yyyyMdd", Locale.getDefault()).format(
                Date(
                    System.currentTimeMillis()
                )
            ),
            extra = "",
            first_install_time = oaidProvider.appFirstInstallTime,
            framework_ver = "3340042",
            from = "1020031h",
            is_teenager = 0,
            last_update_time = oaidProvider.appLastUpdateTime,
            lego_lib_version = "3.0.0",
            model = Build.MODEL,
            net_type = 1,
            oaid = "",
            personalized_rec_switch = 1,
            pversion = "1.0.3",
            q_type = 0,
            sample_id = clientConfigProvider.getSampleId(),
            scr_dip = deviceInfoProvider.density.toDouble(),
            scr_h = deviceInfoProvider.screenHeight,
            scr_w = deviceInfoProvider.screenWidth,
            sdk_ver = "2.34.0",
            start_scheme = "",
            start_type = 1,
            stoken = credentialProvider.getSToken(),
            swan_game_ver = "1038000",
            user_agent = retrofitTiebaApi.getUserAgent("tieba/${clientVersion.version}"),
            z_id = credentialProvider.getZid()
        )
    }

    ClientVersion.TIEBA_V12_POST -> {
        CommonRequest(
            BDUSS = credentialProvider.getBduss(),
            _client_id = clientConfigProvider.getClientId() ?: retrofitTiebaApi.randomClientId,
            _client_type = 2,
            _client_version = clientVersion.version,
            _os_version = "${Build.VERSION.SDK_INT}", // TODO
            _phone_imei = deviceInfoProvider.imei,
            _timestamp = System.currentTimeMillis(),
            active_timestamp = clientConfigProvider.activeTimestamp,
            android_id = uidManager.getAndroidId("000"),
            applist = "",
            brand = Build.BRAND,
            c3_aid = uidManager.getAid(),
            cmode = 1,
            cuid = uidManager.newCUID,
            cuid_galaxy2 = uidManager.newCUID,
            cuid_gid = "",
            device_score = deviceInfoProvider.deviceScore,
            event_day = SimpleDateFormat("yyyyMdd", Locale.getDefault()).format(
                Date(
                    System.currentTimeMillis()
                )
            ),
            extra = "",
            first_install_time = oaidProvider.appFirstInstallTime,
            framework_ver = "3340042",
            from = "1020031h",
            is_teenager = 0,
            last_update_time = oaidProvider.appLastUpdateTime,
            lego_lib_version = "3.0.0",
            model = Build.MODEL,
            net_type = 1,
            oaid = oaidProvider.getEncodedOAID(),
            personalized_rec_switch = 1,
            pversion = "1.0.3",
            q_type = 0,
            sample_id = clientConfigProvider.getSampleId(),
            scr_dip = deviceInfoProvider.density.toDouble(),
            scr_h = deviceInfoProvider.screenHeight,
            scr_w = deviceInfoProvider.screenWidth,
            sdk_ver = "2.34.0",
            start_scheme = "",
            start_type = 1,
            stoken = credentialProvider.getSToken(),
            swan_game_ver = "1038000",
            tbs = tbs,
            user_agent = retrofitTiebaApi.getUserAgent("tieba/${clientVersion.version}"),
            z_id = credentialProvider.getZid(),
        )
    }
}