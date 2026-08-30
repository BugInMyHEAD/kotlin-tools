package com.buginmyhead.tools.kotlin.statemachine

import com.buginmyhead.tools.kotlin.WeakIdentityHashMap
import com.buginmyhead.tools.kotlin.statemachine.TypeSafeBroker.Companion.invoke

/**
 * A type-safe broker for passing values associated with specific keys.
 *
 * The implementation may use a [WeakIdentityHashMap] to store the associations,
 *  ensuring that keys are compared by identity and do not prevent garbage collection.
 *
 * @see invoke
 */
interface TypeSafeBroker {

    /**
     * Associates the given [value] with the specified [key].
     *
     * @param V the type of the value to be associated with the key
     */
    operator fun <V : Any> set(key: Key<V>, value: V)

    /**
     * Removes and returns the event associated with the given [key],
     *  or `null` if no such event exists.
     *
     * @param V the type of the value to be associated with the key
     */
    fun <V : Any> poll(key: Key<V>): V?

    /**
     * Adds all associations from [other] into this broker.
     *
     * If this broker already contains an association for a key present in [other],
     *  the existing value is overwritten by [conflictResolver] result.
     */
    fun merge(
        other: TypeSafeBroker,
        conflictResolver: (key: Key<*>, oldValue: Any, newValue: Any) -> Any = { _, _, newValue -> newValue }
    )

    /**
     * A key used to associate values in the [TypeSafeBroker].
     *
     * @param V the type of the value to be associated with the key
     */
    interface Key<V : Any>

    companion object {

        /**
         * Creates a new instance of [TypeSafeBroker]
         *  that uses [WeakIdentityHashMap] to store the associations.
         * By default, the created broker is not thread-safe.
         *
         * @param synchronization if `true`, the created broker will be thread-safe
         */
        @JvmName("create")
        @JvmOverloads
        @JvmStatic
        operator fun invoke(
            synchronization: Boolean = false,
        ): TypeSafeBroker =
            if (synchronization) SynchronizedTypeSafeBroker(TypeSafeBrokerOnWeakIdentityHashMap())
            else TypeSafeBrokerOnWeakIdentityHashMap()

        /**
         * Adds all associations from [other] into this broker.
         *
         * If this broker already contains an association for a key present in [other],
         *  the existing value is overwritten by the one from [other].
         */
        operator fun TypeSafeBroker.plusAssign(other: TypeSafeBroker) = merge(other)

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

    @Synchronized
    override fun merge(
        other: TypeSafeBroker,
        conflictResolver: (key: TypeSafeBroker.Key<*>, oldValue: Any, newValue: Any) -> Any
    ) =
        delegate.merge(other, conflictResolver)

}

internal class TypeSafeBrokerOnWeakIdentityHashMap : TypeSafeBroker {

    internal val store: MutableMap<Any, Any> = WeakIdentityHashMap()

    override operator fun <V : Any> set(key: TypeSafeBroker.Key<V>, value: V) {
        store[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> poll(key: TypeSafeBroker.Key<V>): V? =
        store.remove(key) as V?

    override fun merge(
        other: TypeSafeBroker,
        conflictResolver: (key: TypeSafeBroker.Key<*>, oldValue: Any, newValue: Any) -> Any
    ) {
        require(other is TypeSafeBrokerOnWeakIdentityHashMap) {
            "Unsupported TypeSafeBroker implementation: ${other::class}"
        }

        for ((key, value) in other.store) {
            store.merge(key, value) { oldValue, newValue ->
                conflictResolver(key as TypeSafeBroker.Key<*>, oldValue, newValue)
            }
        }
    }

}