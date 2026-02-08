package com.buginmyhead.tools.kotlin.statemachine

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

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

        val pollGenericResult = Any()
        val broker = SynchronizedTypeSafeBroker(object : TypeSafeBroker {
            override fun <V : Any> set(key: TypeSafeBroker.Key<V>, value: V) {
                setKeyCaptured = key
                setValueCaptured = value
            }

            override fun <V : Any> poll(key: TypeSafeBroker.Key<V>): V? {
                pollKeyCaptured = key
                @Suppress("UNCHECKED_CAST")
                return pollGenericResult as V
            }
        })
        val setKey = object : TypeSafeBroker.Key<Int> {}
        val setValue = 5
        val pollKey = object : TypeSafeBroker.Key<Any> {}

        broker[setKey] = setValue
        broker.poll(pollKey)

        setKeyCaptured shouldBe setKey
        setValueCaptured shouldBe setValue
        pollKeyCaptured shouldBe pollKey
    }

    "TypeSafeBrokerOnWeakIdentityHashMap poll removes an effect for the identical key" {
        val broker = TypeSafeBrokerOnWeakIdentityHashMap()
        val stateA = State("A")
        broker[stateA] = 5
        gc()

        broker.poll(stateA) shouldBe 5
        broker.poll(stateA) shouldBe null
    }

    "TypeSafeBrokerOnWeakIdentityHashMap poll does not remove an effect if the key is equal but not identical" {
        val broker = TypeSafeBrokerOnWeakIdentityHashMap()
        val stateA = State("A")
        broker[stateA] = 5
        gc()

        stateA shouldBe State("A")
        broker.poll(State("A")) shouldBe null
        broker.poll(stateA) shouldBe 5
    }

    "TypeSafeBrokerOnWeakIdentityHashMap removes zombie effects" {
        val broker = TypeSafeBrokerOnWeakIdentityHashMap()
        broker[State("A")] = 5
        gc()

        broker.store shouldBe emptyMap()
    }
}) {

    private data class State(val value: String) : TypeSafeBroker.Key<Int>

}