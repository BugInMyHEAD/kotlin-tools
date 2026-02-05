package com.buginmyhead.tools.kotlin.statemachine

import com.buginmyhead.tools.kotlin.WeakIdentityHashMap

interface TypeSafeBroker {

    operator fun <V : Any> set(key: Key<V>, value: V)

    /**
     * Removes and returns the event associated with the given [key],
     *  or `null` if no such event exists.
     */
    fun <V : Any> poll(key: Key<V>): V?

    interface Key<V : Any>

    companion object {

        @JvmName("create")
        @JvmOverloads
        @JvmStatic
        operator fun invoke(
            synchronization: Boolean = false,
        ): TypeSafeBroker =
            if (synchronization) SynchronizedTypeSafeBroker(TypeSafeBrokerOnWeakIdentityHashMap())
            else TypeSafeBrokerOnWeakIdentityHashMap()

    }

}

internal class SynchronizedTypeSafeBroker(
    internal val delegate: TypeSafeBroker
) : TypeSafeBroker {

    @Synchronized
    override operator fun <V : Any> set(key: TypeSafeBroker.Key<V>, value: V) =
        delegate.set(key, value)

    @Synchronized
    override fun <V : Any> poll(key: TypeSafeBroker.Key<V>): V? =
        delegate.poll(key)

}

internal class TypeSafeBrokerOnWeakIdentityHashMap : TypeSafeBroker {

    internal val store: MutableMap<Any, Any> = WeakIdentityHashMap()

    override operator fun <V : Any> set(key: TypeSafeBroker.Key<V>, value: V) {
        store[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> poll(key: TypeSafeBroker.Key<V>): V? =
        store.remove(key) as V?

}