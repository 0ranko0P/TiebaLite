package com.huanchengfly.tieba.macrobenchmark.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import com.huanchengfly.tieba.macrobenchmark.TARGET_PACKAGE
import com.huanchengfly.tieba.macrobenchmark.startActivityAndSetup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a startup profile.
 * See
 * [the documentation](https://d.android.com//topic/performance/baselineprofiles/dex-layout-optimizations)
 * for details.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class StartupProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun profileGenerator() {
        rule.collect(
            packageName = TARGET_PACKAGE,
            includeInStartupProfile = true
        ) {
            uiAutomator {
                startActivityAndSetup(welcomeScreen = false) // skip welcome screen
                onElement { contentDescription == "动态" && isVisibleToUser }.click()
                Thread.sleep(600)
                onElement { contentDescription == "我" }.click()
                onElement { textAsString() == "设置" && isVisibleToUser }.click()
            }
        }
    }
}
