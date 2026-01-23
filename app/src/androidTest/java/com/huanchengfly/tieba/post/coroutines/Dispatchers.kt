package com.huanchengfly.tieba.post.coroutines

import com.huanchengfly.tieba.post.di.TestCoroutinesModule
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun runTest(
    context: CoroutineContext = TestCoroutinesModule.Default,
    timeout: Duration = 60.seconds,
    testBody: suspend TestScope.() -> Unit
): TestResult = kotlinx.coroutines.test.runTest(context, timeout, testBody)
