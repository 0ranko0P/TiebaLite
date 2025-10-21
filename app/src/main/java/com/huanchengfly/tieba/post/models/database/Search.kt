package com.huanchengfly.tieba.post.models.database

/**
 * Abstract model used to represent a search history stored in the database.
 * */
abstract class Search {

    abstract val id: Int

    abstract val keyword: String

    abstract val timestamp: Long
}
