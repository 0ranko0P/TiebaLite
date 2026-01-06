package com.huanchengfly.tieba.post.components

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.startup.Initializer
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.di.CoroutinesEntryPoint
import com.huanchengfly.tieba.post.di.RepositoryEntryPoint
import com.huanchengfly.tieba.post.ui.page.TB_LITE_DOMAIN
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Objects

/**
 * 启动时根据用户登录状态初始化动态快捷方式.
 *
 * @see ShortcutInitializer.initialize
 * */
class ShortcutInitializer : Initializer<Unit>{

    override fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        val coroutinesEntryPoint = EntryPointAccessors.fromApplication<CoroutinesEntryPoint>(context)
        coroutinesEntryPoint.coroutineScope().launch {
            delay(2000)
            val repoEntryPoint = EntryPointAccessors.fromApplication<RepositoryEntryPoint>(context)
            val settingsRepo = repoEntryPoint.settingsRepository()
            val loggedIn = settingsRepo.accountUid.snapshot() > 0
            initialize(loggedIn, context)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> = emptyList()

    companion object {
        private const val TAG = "ShortcutInitializer"

        enum class TbShortcut(val id: String, val label: Int, val icon: Int, val requireLogin: Boolean) {
            OK_SIGN(ID_OKSIGN, R.string.shortcut_oksign, R.mipmap.ic_shortcut_oksign, requireLogin = true),

            COLLECTION(ID_COLLECTION, R.string.shortcut_my_collect, R.mipmap.ic_shortcut_my_collect, requireLogin = true),

            SEARCH(ID_SEARCH, R.string.shortcut_search, R.mipmap.ic_shortcut_search, requireLogin = false),

            NOTIFICATIONS(ID_NOTIFICATIONS, R.string.shortcut_my_message, R.mipmap.ic_shortcut_my_message, requireLogin = true);

            open fun shouldEnable(loggedIn: Boolean): Boolean = !requireLogin || loggedIn
        }

        /**
         * Activity Action: Start OK Sign.
         *
         * @since 3.8.1 α
         * */
        private const val ACTION_OKSIGN = "com.huanchengfly.tieba.post.action.OKSIGN"

        /**
         * Intent Extra: Shortcut ID.
         *
         * @since 4.0.0
         * */
        private const val EXTRA_SHORTCUT_ID = "com.huanchengfly.tieba.post.SHORTCUT_ID"

        /**
         * Shortcut ID: OK Sign.
         *
         * @since 3.8.1 α
         * */
        private const val ID_OKSIGN = "oksign"

        /**
         * Shortcut ID: Thread Store page.
         *
         * @since 3.8.1 α
         * */
        private const val ID_COLLECTION = "my_collect"

        /**
         * Shortcut ID: Search page.
         *
         * @since 4.0.0-beta.1
         * */
        private const val ID_SEARCH = "search"

        /**
         * Shortcut ID: Notification page.
         *
         * @since 4.0.0-beta.1
         * */
        private const val ID_NOTIFICATIONS = "notifications"

        private val Intent.filterHashCode: Int
            get() = Objects.hash(filterHashCode(), flags)

        fun getTbShortcut(intent: Intent): TbShortcut? {
            val id = intent.getStringExtra(EXTRA_SHORTCUT_ID) ?: return null
            return TbShortcut.entries.firstOrNull { it.id == id }
        }

        /**
         * Note: Keep sync with [com.huanchengfly.tieba.post.ui.page.RootNavGraph]
         * */
        private fun createIntent(context: Context, shortcut: TbShortcut): Intent {
            val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val intent = when(shortcut) {
                TbShortcut.OK_SIGN -> Intent(ACTION_OKSIGN)

                TbShortcut.COLLECTION -> Intent(Intent.ACTION_VIEW, "$TB_LITE_DOMAIN://favorite".toUri())

                TbShortcut.SEARCH -> Intent(Intent.ACTION_VIEW, "$TB_LITE_DOMAIN://search".toUri())

                TbShortcut.NOTIFICATIONS -> Intent(Intent.ACTION_VIEW, "$TB_LITE_DOMAIN://notifications?type=0".toUri())
            }
            return intent.apply {
                addFlags(flags)
                setPackage(context.packageName)
                putExtra(EXTRA_SHORTCUT_ID, shortcut.id)
            }
        }

        private fun buildShortcut(context: Context, shortcut: TbShortcut): ShortcutInfoCompat.Builder {
            val shortLabel = context.getString(shortcut.label)
            return ShortcutInfoCompat.Builder(context, shortcut.id)
                .setShortLabel(shortLabel)
                .setLongLabel(shortLabel)
                .setIntent(createIntent(context, shortcut))
                .setIcon(IconCompat.createWithResource(context, shortcut.icon))
        }

        @RequiresApi(Build.VERSION_CODES.N_MR1)
        private fun isShortcutsOutdated(context: Context, dynamicShortcuts: List<ShortcutInfo>, loggedIn: Boolean): Boolean {
            if (dynamicShortcuts.isEmpty()) return true

            // Map to <ID, Intent filterHash>
            val available = TbShortcut.entries
                .filter { it.shouldEnable(loggedIn) }
                .sortedBy { it.id }
                .map { it.id to createIntent(context, it).filterHashCode }

            // Fast path: login state changed
            if (dynamicShortcuts.size != available.size) return true
            // Map to <ID, Intent filterHash>
            val dynamic = dynamicShortcuts
                .sortedBy { it.id }
                .map { it.id to (it.intent?.filterHashCode ?: 0) }

            // available shortcuts != dynamic shortcuts
            return !available.toTypedArray().contentDeepEquals(dynamic.toTypedArray())
        }

        @RequiresApi(Build.VERSION_CODES.N_MR1)
        fun initialize(loggedIn: Boolean, context: Context) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            val dynamicShortcuts = shortcutManager.dynamicShortcuts

            if (isShortcutsOutdated(context, dynamicShortcuts, loggedIn)) {
                val tbShortcuts = TbShortcut.entries
                    .filter { it.shouldEnable(loggedIn) }
                    .map {
                        buildShortcut(context, shortcut = it).build()
                    }
                val size = tbShortcuts.size
                Log.w(TAG, "onInitialize: Enabling $size dynamic shortcuts, logged-in: $loggedIn")
                ShortcutManagerCompat.setDynamicShortcuts(context, tbShortcuts)
            } else if (BuildConfig.DEBUG) {
                Log.i(TAG, "onInitialize: Skip update dynamic shortcuts, logged-in: $loggedIn")
            }
        }
    }
}