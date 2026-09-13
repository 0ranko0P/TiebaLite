package com.huanchengfly.tieba.post.models.database.dao

import com.huanchengfly.tieba.post.core.database.dao.TransactionRunner

// Dummy TransactionRunner
object TransactionRunnerDao: TransactionRunner {
    override suspend fun <T> invoke(tx: suspend () -> T): T = tx()
}