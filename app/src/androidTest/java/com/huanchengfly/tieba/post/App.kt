@file:Suppress("unused")

package com.huanchengfly.tieba.post

import android.app.Application
import com.huanchengfly.tieba.post.di.TestCoroutinesModule
import kotlinx.coroutines.CoroutineScope

open class App : Application() {

    override fun onCreate() {
        INSTANCE = this
    }

    companion object {

        @JvmStatic
        lateinit var INSTANCE: App

        val AppBackgroundScope: CoroutineScope = TestCoroutinesModule.testScope
    }
}
