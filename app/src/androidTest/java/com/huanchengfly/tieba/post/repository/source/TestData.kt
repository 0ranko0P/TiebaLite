package com.huanchengfly.tieba.post.repository.source

import com.huanchengfly.tieba.post.api.models.GetForumListBean
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.models.database.TbLiteDatabase
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.GsonUtil
import kotlinx.coroutines.runBlocking

object TestData {

    val DummyAccount = Account(uid = 123, name = "Test user", bduss = "", tbs = "test tbs", portrait = "test portrait", sToken = "token", cookie = "Test cookie")

    // Simulate user login
    fun insertAccount(account: Account = DummyAccount, database: TbLiteDatabase, settingsRepository: SettingsRepository?) {
        runBlocking {
            database.accountDao().upsert(account)
            settingsRepository?.accountUid?.set(account.uid)
            AccountUtil.getInstance().saveNewAccount(null, account)
        }
    }

    // Simulate user logout
    suspend fun purgeAccount(database: TbLiteDatabase, settingsRepository: SettingsRepository?) {
        AccountUtil.getInstance().saveNewAccount(null, null)
        settingsRepository?.accountUid?.set(-1)
        database.accountDao().deleteAll()
    }

    // 黑莓吧, 蓝莓吧, 蔓越莓吧, 草莓吧, 树莓吧
    val DummyGetForumListBean: GetForumListBean by lazy {
        GsonUtil.getGson().fromJson(GET_FORUM_LIST_BEAN_JSON, GetForumListBean::class.java)
    }

    private const val GET_FORUM_LIST_BEAN_JSON: String = """
        {
            "button_content":"开通超级会员",
            "can_use":"0",
            "content":"贴吧超级会员一键签到6倍经验",
            "error":{ "errmsg":"success", "errno":"0", "usermsg":"成功" },
            "error_code":"0",
            "forum_info":[
                {
                    "avatar":"",
                    "cont_sign_num":"1",
                    "forum_id":"120310971",
                    "forum_name":"黑莓",
                    "is_sign_in":"0",
                    "need_exp":"2000",
                    "user_exp":"1900",
                    "user_level":"9"
                },
                {
                    "avatar":"",
                    "cont_sign_num":"0",
                    "forum_id":"324971000",
                    "forum_name":"蓝莓",
                    "is_sign_in":"0",
                    "need_exp":"3000",
                    "user_exp":"2260",
                    "user_level":"10"
                },
                {
                    "avatar":"",
                    "cont_sign_num":"1",
                    "forum_id":"100001",
                    "forum_name":"蔓越莓",
                    "is_sign_in":"0",
                    "need_exp":"1000",
                    "user_exp":"900",
                    "user_level":"8"
                },
                {
                    "avatar":"",
                    "cont_sign_num":"0",
                    "forum_id":"49781646131",
                    "forum_name":"草莓",
                    "is_sign_in":"0",
                    "need_exp":"3000",
                    "user_exp":"2600",
                    "user_level":"10"
                },
                {
                    "avatar":"",
                    "cont_sign_num":"0",
                    "forum_id":"1236137",
                    "forum_name":"树莓",
                    "is_sign_in":"0",
                    "need_exp":"100",
                    "user_exp":"28",
                    "user_level":"5"
                }
            ],
            "level":"7",
            "msign_step_num":"50",
            "num_notice":"现在仅支持对400个吧进行一键签到",
            "server_time":"114514",
            "show_dialog":"1",
            "sign_max_num":"200",
            "sign_new":"0",
            "sign_notice":"零点到一点为签到高峰期，一键签到失败机率较大，请错开高峰期再来签到！",
            "text_color":"",
            "text_mid":"",
            "text_pre":"让签到更容易些吧！加入超级会员可签200个吧",
            "text_suf":"每日最佳签到时间  9:00--16:00 、19:00--22:00",
            "time":1761494622,
            "title":"级以上的吧",
            "user":{
                "pay_member_info":{ "end_time":"0", "pic_url":"", "props_id":"0" },
                "unsign_info":[ { "level":"6", "num":"0" } ]
            },
            "valid":"1"
        }
        """
}