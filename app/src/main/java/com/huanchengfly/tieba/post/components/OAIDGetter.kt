package com.huanchengfly.tieba.post.components

import com.github.gzuliyujiang.oaid.IGetter
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.utils.helios.Base32

object OAIDGetter : IGetter {

    override fun onOAIDGetComplete(result: String) {
        App.Config.inited = true
        App.Config.oaid = result
        App.Config.encodedOAID = Base32.encode(result.encodeToByteArray())
        App.Config.statusCode = 0
        App.Config.isTrackLimited = false
    }

    override fun onOAIDGetError(error: Exception?) {
        App.Config.inited = true
        App.Config.statusCode = -100
        App.Config.isTrackLimited = true
    }
}