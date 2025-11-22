package com.huanchengfly.tieba.macrobenchmark.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import com.huanchengfly.tieba.macrobenchmark.DEFAULT_ITERATIONS
import com.huanchengfly.tieba.macrobenchmark.TARGET_PACKAGE
import com.huanchengfly.tieba.macrobenchmark.startActivityAndSetup
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {
    @get:Rule val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
        maxIterations = DEFAULT_ITERATIONS
    ) {
        uiAutomator {
            startActivityAndSetup(welcomeScreen = false)
            onElement { textAsString() == "发现更多" && isVisibleToUser }.click()
            Thread.sleep(600)
            onElement { contentDescription == "返回" }.click()
            onElement { contentDescription == "动态" && isVisibleToUser }.click()
            Thread.sleep(2000)
            onElement { contentDescription == "我" }.click()
            onElement { textAsString() == "主题选择" && isVisibleToUser }.click()
            Thread.sleep(1000)
            pressBack()
        }
    }
}
