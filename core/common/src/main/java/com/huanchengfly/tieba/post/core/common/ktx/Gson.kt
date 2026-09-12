package com.huanchengfly.tieba.post.core.common.ktx

import com.huanchengfly.tieba.post.utils.GsonUtil

fun Any.toJson(): String = GsonUtil.getGson().toJson(this)