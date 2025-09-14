package com.huanchengfly.tieba.post.ui.page.thread

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

// 收藏
const val FROM_STORE = "store_thread"
const val FROM_PERSONALIZED = "personalized"
const val FROM_HISTORY = "history"

@Serializable(with = ThreadFromSerializer::class)
sealed class ThreadFrom(val tag: String) {

    data class Store(val maxPid: Long, val maxFloor: Int): ThreadFrom(FROM_STORE)

    object Personalized: ThreadFrom(FROM_PERSONALIZED)

    object History: ThreadFrom(FROM_HISTORY)
}

object ThreadFromSerializer: KSerializer<ThreadFrom> {

    private val storeSerializer = PairSerializer(Long.serializer(), Int.serializer())

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ThreadFrom") {
        element<String>("T")
        element("store", storeSerializer.descriptor, isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: ThreadFrom) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.tag)
            if (value is ThreadFrom.Store) {
                val store: Pair<Long, Int> = Pair(value.maxPid, value.maxFloor)
                encodeSerializableElement(descriptor, 1, storeSerializer, store)
            }
        }
    }

    override fun deserialize(decoder: Decoder): ThreadFrom {
        return decoder.decodeStructure(descriptor) {
            var tag = ""
            var store: Pair<Long, Int>? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> tag = decodeStringElement(descriptor, 0)
                    1 -> store = decodeSerializableElement(descriptor, 1, storeSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            when(tag) {
                FROM_PERSONALIZED -> ThreadFrom.Personalized

                FROM_HISTORY -> ThreadFrom.History

                FROM_STORE -> store!!.run { ThreadFrom.Store(first, second) }

                else -> throw RuntimeException("Unimplemented ThreadFrom: $tag.")
            }
        }
    }
}