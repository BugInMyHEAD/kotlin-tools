package com.buginmyhead.tools.kotlin.statemachine

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

internal class StateMachineContextTest : FreeSpec({
    "pushEvent delegates to provided pushEvent" {
        var capturedEvent: Any? = null
        var capturedState: Any? = null
        val pushEvent: (Any, Any) -> Unit = { v, s -> capturedEvent = v; capturedState = s }
        val pollEffect: (Any) -> Any? = { null }

        val state = State("A")
        val ctx = StateMachineContext(state, pushEvent, pollEffect)

        ctx.pushEvent("ev")
        capturedEvent shouldBe "ev"
        capturedState shouldBe state
    }

    "pollEffect delegates and casts result" {
        var capturedState: Any? = null
        val pushEvent: (Any, Any) -> Unit = { _, _ -> }
        val pollEffect: (Any) -> Any? = { s -> capturedState = s; 7 }

        val state = State("A")
        val ctx = StateMachineContext(state, pushEvent, pollEffect)

        ctx.pollEffect() shouldBe 7
        capturedState shouldBe state
    }

    "with creates new context with provided state and reuses delegates" {
        var pushed: Pair<Any, Any>? = null
        val pushEvent: (Any, Any) -> Unit = { v, s -> pushed = Pair(v, s) }
        var polledState: Any? = null
        val pollEffect: (Any) -> Any? = { s -> polledState = s; 11 }

        val stateA = State("A")
        val stateB = State("B")
        val ctxA = StateMachineContext(stateA, pushEvent, pollEffect)
        val ctxB = ctxA.with(stateB)

        ctxB.pushEvent("evt")
        pushed shouldBe Pair("evt", stateB)

        ctxB.pollEffect() shouldBe 11
        polledState shouldBe stateB

        ctxA shouldNotBe ctxB
    }

    "equals and hashCode consider state and delegates" {
        val push = { v: Any, s: Any -> }
        val poll = { s: Any -> 5 }
        val state = State("A")

        val a = StateMachineContext(state, push, poll)
        val b = StateMachineContext(state, push, poll)

        a shouldBe b
        a.hashCode() shouldBe b.hashCode()

        val c = StateMachineContext(state, { _, _ -> }, poll)
        a shouldNotBe c
    }
}) {

    private data class State(val value: String) : TypeSafeBroker.Key<Int>

}