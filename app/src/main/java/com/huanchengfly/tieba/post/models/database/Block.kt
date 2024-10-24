package com.huanchengfly.tieba.post.models.database

import org.litepal.crud.LitePalSupport

data class Block @JvmOverloads constructor(
    val category: Int = 0,
    val type: Int = 0,
    val keyword: String? = null,
    val username: String? = null,
    val uid: Long = 0L,
) : LitePalSupport() {
    var id: Long = 0L
        private set

    fun clone(category: Int = this.category, keyword: String? = this.keyword, username: String? = this.username) =
        this.copy(category = category, keyword = keyword, username = username, uid = uid).also {
            it.id = this.id
        }

    companion object {
        const val CATEGORY_BLACK_LIST = 10
        const val CATEGORY_WHITE_LIST = 11

        const val TYPE_KEYWORD = 0
        const val TYPE_USER = 1
    }
}