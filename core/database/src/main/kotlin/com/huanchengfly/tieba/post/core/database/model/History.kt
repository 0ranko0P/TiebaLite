package com.huanchengfly.tieba.post.core.database.model

/**
 * Abstract model used to represent a history stored in the database.
 *
 * @param timestamp epoch timestamp, should be the last access time
 * */
abstract class History {

    abstract val id: Long

    abstract val avatar: String

    abstract val name: String

    abstract val timestamp: Long
}