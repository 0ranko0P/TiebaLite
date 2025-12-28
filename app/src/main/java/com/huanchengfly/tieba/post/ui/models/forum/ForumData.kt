package com.huanchengfly.tieba.post.ui.models.forum

import androidx.compose.runtime.Immutable

// Classify name, Classify ID
typealias GoodClassify = Pair<String, Int>

@Immutable
/*data */class ForumData(
    val id: Long,
    val avatar: String,
    val name: String,
    val forumRuleTitle: String?,
    val slogan: String?,
    val tbs: String?,
    val liked: Boolean,
    val signed: Boolean,
    val signedDays: Int,
    val signedRank: Int,
    val level: Int,
    val levelName: String,
    val score: Int,
    val scoreLevelUp: Int,
    val members: Int,
    val threads: Int,
    val posts: Int,
    val goodClassifies: List<GoodClassify>?,
) {

    val levelProgress: Float
        get() = score.toFloat() / scoreLevelUp.coerceAtLeast(1)

    fun copy(
        avatar: String = this.avatar,
        forumRuleTitle: String? = this.forumRuleTitle,
        slogan: String? = this.slogan,
        tbs: String? = this.tbs,
        liked: Boolean = this.liked,
        signed: Boolean = this.signed,
        signedDays: Int = this.signedDays,
        signedRank: Int = this.signedRank,
        level: Int = this.level,
        levelName: String = this.levelName,
        score: Int = this.score,
        scoreLevelUp: Int = this.scoreLevelUp,
        members: Int = this.members,
        threads: Int = this.threads,
        posts: Int = this.posts,
        goodClassifies: List<GoodClassify>? = this.goodClassifies
    ) = ForumData(
        id = this.id,
        avatar = avatar,
        name = this.name,
        forumRuleTitle = forumRuleTitle,
        slogan = slogan,
        tbs = tbs,
        liked = liked,
        signed = signed,
        signedDays = signedDays,
        signedRank = signedRank,
        level = level,
        levelName = levelName,
        score = score,
        scoreLevelUp = scoreLevelUp,
        members = members,
        threads = threads,
        posts = posts,
        goodClassifies = goodClassifies
    )
}