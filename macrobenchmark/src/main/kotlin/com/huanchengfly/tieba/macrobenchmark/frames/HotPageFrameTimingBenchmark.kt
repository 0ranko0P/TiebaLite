package com.huanchengfly.tieba.macrobenchmark.frames

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import com.huanchengfly.tieba.macrobenchmark.TARGET_PACKAGE
import com.huanchengfly.tieba.macrobenchmark.TRACE_FEED_CARD
import com.huanchengfly.tieba.macrobenchmark.startActivityAndSetup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class HotPageFrameTimingBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun scrollHotPageList() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(
                FrameTimingMetric(),
                // Measure custom trace sections by name [TRACE_FEED_CARD] (which is added to the HotPage composable).
                // Mode.Sum measure combined duration and also how many times it occurred in the trace.
                // This way, you can estimate whether a composable recomposes more than it should.
                TraceSectionMetric(TRACE_FEED_CARD, TraceSectionMetric.Mode.Sum),
                // This trace section takes into account the SQL wildcard character %,
                // which can find trace sections without knowing the full name.
                // This way, you can measure composables produced by the composition tracing
                // and measure how long they took and how many times they recomposed.
                // WARNING: This metric only shows results when running with composition tracing, otherwise it won't be visible in the outputs.
                TraceSectionMetric("%FeedCard (%", TraceSectionMetric.Mode.Sum),
            ),
            // Try switching to different compilation modes to see the effect
            // it has on frame timing metrics.
            compilationMode = CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Disable,
                warmupIterations = 1
            ),
            startupMode = StartupMode.WARM, // restarts activity each iteration
            iterations = 10,
            setupBlock = {
                pressHome(1000)
                uiAutomator {
                    startActivityAndSetup(welcomeScreen = false)
                    // Waits for an element that corresponds to fully drawn state
                    onElement { contentDescription == "动态" && isVisibleToUser }.click()

                    // Jump to HotPage
                    onElement { textAsString() == "热榜" && isVisibleToUser }.click()

                    // Wait data to load
                    onElement { textAsString() == "总榜" && isVisibleToUser }
                }
            }
        ) {
            uiAutomator {
                onElement(timeoutMs = 300) { isScrollable }.fling(Direction.DOWN)
            }
        }
    }
}