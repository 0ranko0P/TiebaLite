package com.huanchengfly.tieba.post.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil.startSign
import com.huanchengfly.tieba.post.workers.OKSignWorker
import kotlinx.coroutines.flow.FlowCollector

class OKSignTileService: TileService(), LifecycleOwner {

    companion object {

        /**
         * Is OKSign tile service in listening state
         *
         * @see OKSignTileService.onStartListening
         * @see OKSignTileService.requestListening
         */
        @Volatile
        private var isListening: Boolean = false

        fun requestListening(appContext: Context) {
            if (!isListening) { // Avoid costly binder call to StatusBarManager service
                isListening = true
                requestListeningState(appContext, ComponentName(appContext, OKSignTileService::class.java))
            }
        }
    }

    private val dispatcher = ServiceLifecycleDispatcher(this)

    private var initialized = false

    private var userName: String? = null

    private val workInfoCollector = FlowCollector<List<WorkInfo>> { workInfos ->
        val info = workInfos.lastOrNull()
        if (info != null && info.state == WorkInfo.State.RUNNING) {
            val progressData = info.progress
            val progress = progressData.getInt(OKSignWorker.KEY_PROGRESS, 0)
            val total = progressData.getInt(OKSignWorker.KEY_TOTAL, 0)
            updateState(Tile.STATE_ACTIVE, progress, total)
        } else {
            updateState(Tile.STATE_INACTIVE, 0, 0)
        }
    }

    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()

        // Observe OKSign workers
        val workManager = WorkManager.getInstance(application)
        workManager.getWorkInfosByTagFlow(OKSignWorker.TAG_EXPEDITED).collectIn(this, collector = workInfoCollector)
        workManager.getWorkInfosByTagFlow(OKSignWorker.TAG).collectIn(this, collector = workInfoCollector)

        // Observe current account
        AccountUtil.getInstance().currentAccount.collectIn(lifecycleOwner = this) { account ->
            requestListening(applicationContext)
            userName = account?.name
            if (qsTile?.state != Tile.STATE_ACTIVE) {
                updateState(Tile.STATE_INACTIVE, 0, 0)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        return super.onBind(intent)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onStart(intent: Intent?, startId: Int) {
        dispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    override fun onStartListening() {
        isListening = true

        if (!initialized) {
            initialized = true
            updateState(Tile.STATE_INACTIVE, 0, 0)
        }
    }

    override fun onStopListening() {
        isListening = false
    }

    private fun updateState(state: Int, progress: Int, total: Int) {
        var state = state
        val qsTile = qsTile ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (state) {
                Tile.STATE_UNAVAILABLE -> qsTile.subtitle = getString(R.string.text_loading)

                Tile.STATE_INACTIVE -> {
                    qsTile.subtitle = userName ?: getString(R.string.tip_login)
                    // No user logged-in, make tile unclickable
                    if (userName == null) {
                        state = Tile.STATE_UNAVAILABLE
                    }
                }

                Tile.STATE_ACTIVE -> qsTile.subtitle = "$progress / $total" // simple version of R.string.title_signing_progress
            }
        }
        qsTile.state = state
        qsTile.updateTile()
    }

    override fun onClick() {
        if (qsTile?.state == Tile.STATE_INACTIVE) {
            startSign(application)
        }
    }

    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        isListening = false
        super.onDestroy()
    }

    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle
}