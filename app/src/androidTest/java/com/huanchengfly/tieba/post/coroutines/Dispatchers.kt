package com.huanchengfly.tieba.post.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// TODO: Use dependency injection to provide dispatchers
object Dispatchers {
    val testScope = TestScope()

    val IO: CoroutineDispatcher = StandardTestDispatcher(testScope.testScheduler, "TestIO")

    val Main: CoroutineDispatcher = StandardTestDispatcher(testScope.testScheduler, "TestMain")

    val Default: CoroutineDispatcher = Main
}

fun runTest(
    context: CoroutineContext = Dispatchers.Default,
    timeout: Duration = 60.seconds,
    testBody: suspend TestScope.() -> Unit
): TestResult = kotlinx.coroutines.test.runTest(context, timeout, testBody)
