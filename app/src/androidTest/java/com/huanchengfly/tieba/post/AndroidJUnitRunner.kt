package com.huanchengfly.tieba.post

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.CustomTestApplication

@CustomTestApplication(TestApp::class)
open class TestApp() : App()

@Suppress("unused")
class TbLiteTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, TestApp_Application::class.java.name, context)
    }
}
