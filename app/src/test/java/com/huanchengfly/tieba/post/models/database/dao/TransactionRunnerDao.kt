package com.huanchengfly.tieba.post.models.database.dao

// Dummy TransactionRunner
object TransactionRunnerDao: TransactionRunner {
    override suspend fun <T> invoke(tx: suspend () -> T): T = tx()
}