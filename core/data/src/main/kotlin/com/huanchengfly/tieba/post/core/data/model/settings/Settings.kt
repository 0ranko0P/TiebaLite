package com.huanchengfly.tieba.post.core.data.model.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

abstract class Settings<T>(protected val flow: Flow<T>): Flow<T> by flow {

    suspend fun snapshot(): T = this.first()

    abstract fun set(new: T)

    abstract fun save(transform: (old: T) -> T)
}