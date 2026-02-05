package com.buginmyhead.tools.kotlin.statemachine

import com.buginmyhead.tools.kotlin.WeakIdentityHashMap
import java.lang.ref.WeakReference
import java.util.LinkedList

interface EffectBroker {

    operator fun set(key: Any, effect: Any)

    /**
     * Removes and returns the effect associated with the given [key],
     *  or `null` if no such effect exists.
     */
    fun poll(key: Any): Any?

    /**
     * Removes and returns the effect associated with the given [key],
     *  or `null` if no such effect exists.
     */
    @Suppress("UNCHECKED_CAST")
    fun <F> poll(key: Key<F>): F? = poll(key as Any) as F?

    interface Key<F>

    companion object {

        @JvmName("create")
        @JvmOverloads
        @JvmStatic
        operator fun invoke(
            synchronization: Boolean = false,
        ): EffectBroker =
            if (synchronization) SynchronizedEffectBroker(EffectBrokerOnWeakIdentityHashMap())
            else EffectBrokerOnWeakIdentityHashMap()

    }

}

internal class SynchronizedEffectBroker(
    internal val delegate: EffectBroker
) : EffectBroker {

    @Synchronized
    override operator fun set(key: Any, effect: Any) = delegate.set(key, effect)

    @Synchronized
    override fun poll(key: Any): Any? = delegate.poll(key)

    @Synchronized
    override fun <F> poll(key: EffectBroker.Key<F>): F? = delegate.poll(key)

}

internal class EffectBrokerOnLinkedList : EffectBroker {

    internal val keyRefsAndEffects: MutableList<Pair<WeakReference<Any>, Any>> = LinkedList()

    override operator fun set(key: Any, effect: Any) {
        keyRefsAndEffects += WeakReference(key) to effect
    }

    override fun poll(key: Any): Any? {
        var result: Any? = null

        val itt = keyRefsAndEffects.iterator()
        while (itt.hasNext()) {
            val (keyRefInTheList, effect) = itt.next()
            val keyInTheList = keyRefInTheList.get()
            when {
                keyInTheList === null -> {
                    itt.remove()
                }
                keyInTheList === key -> {
                    itt.remove()
                    result = effect
                }
            }
        }

        return result
    }

}

internal class EffectBrokerOnWeakIdentityHashMap : EffectBroker {

    internal val keyToEffect: MutableMap<Any, Any> = WeakIdentityHashMap()

    override operator fun set(key: Any, effect: Any) {
        keyToEffect[key] = effect
    }

    override fun poll(key: Any): Any? = keyToEffect.remove(key)

}