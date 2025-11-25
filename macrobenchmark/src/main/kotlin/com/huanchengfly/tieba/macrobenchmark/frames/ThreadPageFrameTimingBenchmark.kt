package com.huanchengfly.tieba.macrobenchmark.frames

import android.net.Uri
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
import com.huanchengfly.tieba.macrobenchmark.DEFAULT_ITERATIONS
import com.huanchengfly.tieba.macrobenchmark.TAG_COLUMN
import com.huanchengfly.tieba.macrobenchmark.TARGET_PACKAGE
import com.huanchengfly.tieba.macrobenchmark.TRACE_THREAD
import com.huanchengfly.tieba.macrobenchmark.startActivityAndSetup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Multi image thread
private const val URL_THREAD_A = "https://tieba.baidu.com/p/10083795678"

@LargeTest
@RunWith(AndroidJUnit4::class)
class ThreadPageFrameTimingBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun scrollTreadPage() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(
                FrameTimingMetric(),
                TraceSectionMetric(TRACE_THREAD, TraceSectionMetric.Mode.Sum),
            ),
            compilationMode = CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Disable,
                warmupIterations = 1
            ),
            startupMode = StartupMode.WARM,
            iterations = DEFAULT_ITERATIONS,
            setupBlock = {
                pressHome(1000)
                uiAutomator {
                    startActivityAndSetup(welcomeScreen = false) {
                        data = Uri.parse(URL_THREAD_A)
                    }
                }
            }
        ) {
            uiAutomator {
                onElement { textAsString() == "楼主" && isVisibleToUser }
                onElement { viewIdResourceName == TAG_COLUMN && isVisibleToUser }.run {
                    repeat(1) {
                        fling(Direction.DOWN)
                    }
                }
            }
        }
    }
}
