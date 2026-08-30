package com.buginmyhead.tools.kotlin.statemachine

import com.buginmyhead.tools.kotlin.statemachine.TypeSafeBroker.Companion.plusAssign
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

internal class TypeSafeBrokerTest : FreeSpec({
    fun gc() {
        Thread.sleep(200)
        System.gc()
    }

    "TypeSafeBrokerOnWeakIdentityHashMap is created by default" {
        val broker = TypeSafeBroker()

        broker as TypeSafeBrokerOnWeakIdentityHashMap
    }

    "SynchronizedTypeSafeBroker proxies underlying TypeSafeBrokerOnWeakIdentityHashMap if synchronization is true" {
        val broker = TypeSafeBroker(synchronization = true)

        broker as SynchronizedTypeSafeBroker
        broker.delegate as TypeSafeBrokerOnWeakIdentityHashMap
    }

    "SynchronizedTypeSafeBroker delegates" {
        var setKeyCaptured: Any? = null
        var setValueCaptured: Any? = null
        var pollKeyCaptured: Any? = null
        var mergeOtherCaptured: Any? = null
        var mergeConflictResolverCaptured: Any? = null

        val pollGenericResult = Any()
        val broker = SynchronizedTypeSafeBroker(object : TypeSafeBroker {
            override fun <V : Any> set(key: TypeSafeBroker.Key<V>, value: V) {
                setKeyCaptured = key
                setValueCaptured = value
            }

            override fun <V : Any> poll(key: TypeSafeBroker.Key<V>): V {
                pollKeyCaptured = key
                @Suppress("UNCHECKED_CAST")
                return pollGenericResult as V
            }

            override fun merge(
                other: TypeSafeBroker,
                conflictResolver: (key: TypeSafeBroker.Key<*>, oldValue: Any, newValue: Any) -> Any
            ) {
                mergeOtherCaptured = other
                mergeConflictResolverCaptured = conflictResolver
            }
        })
        val setKey = object : TypeSafeBroker.Key<String> {}
        val setValue = "13"
        val pollKey = object : TypeSafeBroker.Key<Any> {}
        val other = TypeSafeBroker()
        val conflictResolver = { key: TypeSafeBroker.Key<*>, oldValue: Any, newValue: Any -> newValue }

        broker[setKey] = setValue
        broker.poll(pollKey)
        broker.merge(other, conflictResolver)

        setKeyCaptured shouldBeSameInstanceAs setKey
        setValueCaptured shouldBeSameInstanceAs setValue
        pollKeyCaptured shouldBeSameInstanceAs pollKey
        mergeOtherCaptured shouldBeSameInstanceAs other
        mergeConflictResolverCaptured shouldBeSameInstanceAs conflictResolver
    }

    "TypeSafeBrokerOnWeakIdentityHashMap poll removes an effect for the identical key" {
        val broker = TypeSafeBrokerOnWeakIdentityHashMap()
        val stateA = State("A")
        broker[stateA] = 13
        gc()

        broker.poll(stateA) shouldBe 13
        broker.poll(stateA) shouldBe null
    }

    "TypeSafeBrokerOnWeakIdentityHashMap poll does not remove an effect if the key is equal but not identical" {
        val broker = TypeSafeBrokerOnWeakIdentityHashMap()
        val stateA = State("A")
        broker[stateA] = 13
        gc()

        stateA shouldBe State("A")
        broker.poll(State("A")) shouldBe null
        broker.poll(stateA) shouldBe 13
    }

    "TypeSafeBrokerOnWeakIdentityHashMap removes zombie effects" {
        val broker = TypeSafeBrokerOnWeakIdentityHashMap()
        broker[State("A")] = 13
        gc()

        broker.store shouldBe emptyMap()
    }

    "TypeSafeBrokerOnWeakIdentityHashMap merge resolves conflicting effects" {
        val broker1 = TypeSafeBrokerOnWeakIdentityHashMap()
        val broker2 = TypeSafeBrokerOnWeakIdentityHashMap()
        val state = State("A")
        broker1[state] = 13
        broker2[state] = 17

        broker1.merge(broker2) { key, oldValue, newValue ->
            key shouldBeSameInstanceAs state
            oldValue shouldBe 13
            newValue shouldBe 17
            19
        }

        broker1.poll(state) shouldBe 19
    }

    "TypeSafeBrokerOnWeakIdentityHashMap plusAssign replaces with right-side effects" {
        val broker1 = TypeSafeBrokerOnWeakIdentityHashMap()
        val broker2 = TypeSafeBrokerOnWeakIdentityHashMap()
        val stateA = State("A")
        val stateB = State("B")
        broker1[stateA] = 13
        broker2[stateB] = 17

        broker1 += broker2

        broker1.poll(stateA) shouldBe 13
        broker1.poll(stateB) shouldBe 17
    }

    "TypeSafeBrokerOnWeakIdentityHashMap merge throws if other is not TypeSafeBrokerOnWeakIdentityHashMap" {
        val broker1 = TypeSafeBrokerOnWeakIdentityHashMap()
        val broker2 = DummyTypeSafeBroker

        shouldThrow<IllegalArgumentException> {
            broker1 += broker2
        }
    }

    "TypeSafeBrokerOnWeakIdentityHashMap plusAssign throws if other is not TypeSafeBrokerOnWeakIdentityHashMap" {
        val broker1 = TypeSafeBrokerOnWeakIdentityHashMap()
        val broker2 = DummyTypeSafeBroker

        shouldThrow<IllegalArgumentException> {
            broker1 += broker2
        }
    }
}) {

    private data class State(val value: String) : TypeSafeBroker.Key<Int>

    private object DummyTypeSafeBroker : TypeSafeBroker {

        override fun <V : Any> set(key: TypeSafeBroker.Key<V>, value: V) = Unit

        override fun <V : Any> poll(key: TypeSafeBroker.Key<V>): V? = null

        override fun merge(
            other: TypeSafeBroker,
            conflictResolver: (key: TypeSafeBroker.Key<*>, oldValue: Any, newValue: Any) -> Any
        ) = Unit

    }

}