package com.huanchengfly.tieba.post.ui.page.thread

import androidx.annotation.IntDef

@IntDef(ThreadSortType.BY_ASC, ThreadSortType.BY_DESC, ThreadSortType.BY_HOT)
@Retention(AnnotationRetention.SOURCE)
annotation class ThreadSortType {
    companion object {
        const val BY_ASC = 0
        const val BY_DESC = 1
        const val BY_HOT = 2
        const val DEFAULT = BY_ASC
    }
}