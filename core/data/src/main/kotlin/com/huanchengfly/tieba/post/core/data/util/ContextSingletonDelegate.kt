package com.huanchengfly.tieba.post.core.data.util

import android.app.Application
import android.content.Context
import androidx.annotation.GuardedBy
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ContextSingletonDelegate<out T>(
    private val initializer: Application.() -> T
) : ReadOnlyProperty<Context, T> {

    private val lock = Any()

    @GuardedBy("lock")
    @Volatile
    private var INSTANCE: T? = null

    /**
     * Gets the instance of the [T] object.
     *
     * @param thisRef must be an instance of [Context]
     * @param property not used
     */
    override fun getValue(thisRef: Context, property: KProperty<*>): T {
        return INSTANCE ?: synchronized(lock) {
            INSTANCE ?: initializer(thisRef.applicationContext as Application).also {
                INSTANCE = it
            }
        }
    }
}