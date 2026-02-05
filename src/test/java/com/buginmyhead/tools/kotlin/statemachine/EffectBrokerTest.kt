package com.buginmyhead.tools.kotlin.statemachine

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

internal class EffectBrokerTest : FreeSpec({
    fun gc() {
        Thread.sleep(200)
        System.gc()
    }

    "EffectBrokerOnWeakIdentityHashMap is created by default" {
        val broker = EffectBroker()
        broker::class shouldBe EffectBrokerOnWeakIdentityHashMap::class
    }

    "SynchronizedEffectBroker is created if synchronization is true" {
        val broker = EffectBroker(synchronization = true)
        broker::class shouldBe SynchronizedEffectBroker::class
    }

    "EffectBrokerOnWeakIdentityHashMap poll removes an effect for the identical key" {
        val broker = EffectBrokerOnWeakIdentityHashMap()
        val stateA = State("A")
        broker[stateA] = 5
        gc()

        broker.poll(stateA) shouldBe 5
        broker.poll(stateA) shouldBe null
    }

    "EffectBrokerOnLinkedList poll removes an effect for the identical key" {
        val broker = EffectBrokerOnLinkedList()
        val stateA = State("A")
        broker[stateA] = 5
        gc()

        broker.poll(stateA) shouldBe 5
        broker.poll(stateA) shouldBe null
    }

    "EffectBrokerOnWeakIdentityHashMap poll does not remove an effect if the key is equal but not identical" {
        val broker = EffectBrokerOnWeakIdentityHashMap()
        val stateA = State("A")
        broker[stateA] = 5
        gc()

        stateA shouldBe State("A")
        broker.poll(State("A")) shouldBe null
        broker.poll(stateA) shouldBe 5
    }

    "EffectBrokerOnLinkedList poll does not remove an effect if the key is equal but not identical" {
        val broker = SynchronizedEffectBroker(EffectBrokerOnLinkedList())
        val stateA = State("A")
        broker[stateA] = 5
        gc()

        stateA shouldBe State("A")
        broker.poll(State("A")) shouldBe null
        broker.poll(stateA) shouldBe 5
    }

    "EffectBrokerOnWeakIdentityHashMap removes zombie effects" {
        val broker = EffectBrokerOnWeakIdentityHashMap()
        broker[State("A")] = 5
        gc()

        // broker as EffectBrokerOnLinkedList
        // println(broker.keyRefsAndEffects.first().first.get())
        broker.keyToEffect shouldBe emptyMap()
    }

    "EffectBrokerOnLinkedList poll removes zombie effects" {
        val broker = EffectBrokerOnLinkedList()
        broker[State("A")] = 5
        gc()

        broker.poll(Any())

        broker.keyRefsAndEffects shouldBe emptyList()
    }

    "SynchronizedEffectBroker delegates" {
        var setKeyCaptured: Any? = null
        var setEffectCaptured: Any? = null
        var pollKeyCaptured: Any? = null
        var pollGenericKeyCaptured: Any? = null

        val pollResult = Any()
        val pollGenericResult = Any()
        val broker = SynchronizedEffectBroker(object : EffectBroker {
            override fun set(key: Any, effect: Any) {
                setKeyCaptured = key
                setEffectCaptured = effect
            }

            override fun poll(key: Any): Any? {
                pollKeyCaptured = key
                return pollResult
            }

            override fun <F> poll(key: EffectBroker.Key<F>): F? {
                pollGenericKeyCaptured = key
                @Suppress("UNCHECKED_CAST")
                return pollGenericResult as F
            }
        })
        val setKey = Any()
        val setEffect = Any()
        val pollKey = Any()
        val pollKeyGeneric = object : EffectBroker.Key<Any> {}

        broker[setKey] = setEffect
        broker.poll(pollKey)
        broker.poll(pollKeyGeneric)

        setKeyCaptured shouldBe setKey
        setEffectCaptured shouldBe setEffect
        pollKeyCaptured shouldBe pollKey
        pollGenericKeyCaptured shouldBe pollKeyGeneric
    }
}) {

    private data class State(val value: String) : EffectBroker.Key<Int>

}