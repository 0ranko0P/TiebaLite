package com.huanchengfly.tieba.post.models

data class EmoticonCache(
    var ids: Set<String> = emptySet(),
    var mapping: Map<String, String> = emptyMap()
)
