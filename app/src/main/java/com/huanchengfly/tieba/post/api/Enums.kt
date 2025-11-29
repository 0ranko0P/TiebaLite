package com.huanchengfly.tieba.post.api

enum class ClientVersion(val version: String) {
    TIEBA_V11("11.10.8.6"),
    TIEBA_V12("12.52.1.0"),
    TIEBA_V12_POST("12.35.1.0");

    override fun toString(): String {
        return version
    }
}