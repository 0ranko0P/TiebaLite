package com.huanchengfly.tieba.post.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias SubNavBackStack<T> = Map<T, NavBackStack<T>>

/**
 * Provides a [SubNavBackStack] that is automatically remembered in the Compose hierarchy across
 * process death and configuration changes.
 *
 * This overload **does not take a [SavedStateConfiguration]**. It relies on the platform default:
 * on **Android**, state is saved/restored using a **reflection-based serializer**; on **other
 * platforms this will fail at runtime**. If you target non-Android platforms, use the overload that
 * accepts a [SavedStateConfiguration] and register your serializers explicitly.
 *
 * ### When to use this overload
 * - You are on **Android only** and want a simple API that uses reflection under the hood.
 * - Your back stack elements use **closed polymorphism** (sealed hierarchies) or otherwise work
 *   with Android’s reflective serializer.
 *
 * ### Serialization requirements
 * - Each element placed in the [NavBackStack] must be `@Serializable`.
 * - For **closed polymorphism** (sealed hierarchies), the compiler knows all subtypes and generates
 *   serializers; Android’s reflection will also work.
 * - For **open polymorphism** (interfaces or non-sealed hierarchies):
 *     - On Android, the reflection path can handle subtypes without manual registration.
 *     - On non-Android, this overload is **unsupported**; use the configuration overload and
 *       register all subtypes of [NavKey] in a [kotlinx.serialization.modules.SerializersModule].
 *
 * @sample androidx.navigation3.runtime.samples.rememberNavBackStack_withReflection
 * @param topLevelKeys list of the top level key.
 * @return A [SubNavBackStack] that survives process death and configuration changes on Android.
 */
@Composable
internal fun rememberSubNavBackStack(topLevelKeys: List<NavKey>): SubNavBackStack<NavKey> {
    return rememberSerializable(
        topLevelKeys,
        serializer = SubNavBackStackSerializer(elementSerializer = NavKeySerializer())
    ) {
        topLevelKeys.associateWithTo(mutableStateMapOf()) { key -> NavBackStack(key) }
    }
}

/**
 * A [KSerializer] for Sub [NavBackStack].
 *
 * This serializer wraps a [KSerializer] for the element type [T], enabling serialization and
 * deserialization of [NavBackStack] instances. The serialization of individual elements is
 * delegated to the provided [elementSerializer].
 *
 * If your stack elements [T] are open polymorphic (e.g., a interface for different screens), the
 * provided [elementSerializer] must be correctly configured to handle this.
 *
 * @sample androidx.navigation3.runtime.samples.NavBackStackSerializer_withReflection
 * @param T The type of elements stored in the [NavBackStack].
 * @param elementSerializer The [KSerializer] used to serialize and deserialize individual elements.
 */
internal class SubNavBackStackSerializer<T : NavKey>(
    private val elementSerializer: KSerializer<T>
) : KSerializer<Map<T, NavBackStack<T>>> {

    private val delegate = MapSerializer(
        keySerializer = elementSerializer,
        valueSerializer = NavBackStackSerializer(elementSerializer)
    )

    override val descriptor: SerialDescriptor =
        SerialDescriptor("com.huanchengfly.tieba.post.core.navigation.SubNavBackStack", delegate.descriptor)

    override fun serialize(encoder: Encoder, value: Map<T, NavBackStack<T>>) {
        encoder.encodeSerializableValue(serializer = delegate, value = value)
    }

    override fun deserialize(decoder: Decoder): Map<T, NavBackStack<T>> {
        return decoder.decodeSerializableValue(deserializer = delegate)
    }
}
