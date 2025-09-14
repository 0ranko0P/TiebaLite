package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import org.litepal.crud.LitePalSupport

@Immutable
data class SearchHistory(
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
) : LitePalSupport(), KeywordProvider {
    val id: Long = 0

    override fun getKeyword(): String = content
}

/**
 * Display search keyword on UI for [SearchHistory] and [SearchPostHistory]
 *
 * @see [com.huanchengfly.tieba.post.ui.page.search.SearchHistoryList]
 * */
interface KeywordProvider {
    fun getKeyword(): String
}