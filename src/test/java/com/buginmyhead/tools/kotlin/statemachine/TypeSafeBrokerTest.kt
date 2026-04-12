package com.buginmyhead.tools.kotlin.statemachine

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
        var plusAssignOtherCaptured: Any? = null

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

            override fun plusAssign(other: TypeSafeBroker) {
                plusAssignOtherCaptured = other
            }
        })
        val setKey = object : TypeSafeBroker.Key<Int> {}
        val setValue = 13
        val pollKey = object : TypeSafeBroker.Key<Any> {}
        val other = TypeSafeBroker()

        broker[setKey] = setValue
        broker.poll(pollKey)
        broker += other

        setKeyCaptured shouldBeSameInstanceAs setKey
        setValueCaptured shouldBe setValue
        pollKeyCaptured shouldBeSameInstanceAs pollKey
        plusAssignOtherCaptured shouldBeSameInstanceAs other
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

    "TypeSafeBrokerOnWeakIdentityHashMap plusAssign merges effects" {
        val broker1 = TypeSafeBrokerOnWeakIdentityHashMap()
        val broker2 = TypeSafeBrokerOnWeakIdentityHashMap()
        val stateA = State("A")
        val stateB = State("B")
        broker1[stateA] = 13
        broker2[stateB] = 17

        broker1 += broker2

        broker1.poll(stateA) shouldBe 13
        broker1.poll(stateB) shouldBe 17
        broker2.poll(stateA) shouldBe null
        broker2.poll(stateB) shouldBe null
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

        override fun plusAssign(other: TypeSafeBroker) = Unit

    }

}