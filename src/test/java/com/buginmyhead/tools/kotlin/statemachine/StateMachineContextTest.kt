package com.buginmyhead.tools.kotlin.statemachine

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

internal class StateMachineContextTest : FreeSpec({
    "pushEvent delegates to provided pushEvent" {
        var stateCaptured: Any? = null
        var eventCaptured: Any? = null
        val pushEvent: (Any, Any) -> Unit = { state, event ->
            stateCaptured = state
            eventCaptured = event
        }
        val pollEffect: (Any) -> Any? = { null }

        val state = State("A")
        val ctx = StateMachine.Context(state, pushEvent, pollEffect)

        ctx.pushEvent("ev")
        eventCaptured shouldBe "ev"
        stateCaptured shouldBe state
    }

    "pollEffect delegates and casts result" {
        var stateCaptured: Any? = null
        val pushEvent: (Any, Any) -> Unit = { _, _ -> }
        val pollEffect: (Any) -> Any? = { state ->
            stateCaptured = state
            7
        }

        val state = State("A")
        val ctx = StateMachine.Context(state, pushEvent, pollEffect)

        ctx.pollEffect() shouldBe 7
        stateCaptured shouldBe state
    }

    "with creates new context with provided state and reuses delegates" {
        var stateCaptured: Any? = null
        var eventCaptured: Any? = null
        val pushEvent: (Any, Any) -> Unit = { state, event ->
            stateCaptured = state
            eventCaptured = event
        }
        var polledState: Any? = null
        val pollEffect: (Any) -> Any? = { s ->
            polledState = s
            11
        }

        val stateA = State("A")
        val stateB = State("B")
        val ctxA = StateMachine.Context(stateA, pushEvent, pollEffect)
        val ctxB = ctxA.with(stateB)

        ctxB.pushEvent("evt")
        stateCaptured shouldBe stateB
        eventCaptured shouldBe "evt"

        ctxB.pollEffect() shouldBe 11
        polledState shouldBe stateB

        ctxA shouldNotBe ctxB
    }

    "equals and hashCode consider state and delegates" {
        val push = { v: Any, s: Any -> }
        val poll = { s: Any -> 5 }
        val state = State("A")

        val a = StateMachine.Context(state, push, poll)

        a shouldBe a
        a.hashCode() shouldBe a.hashCode()
        a shouldNotBe null
        a shouldNotBe Any()

        val b = StateMachine.Context(state, push, poll)

        a shouldBe b
        a.hashCode() shouldBe b.hashCode()

        val c = StateMachine.Context(state, { _, _ -> }, poll)
        a shouldNotBe c
    }
}) {

    private data class State(val value: String) : TypeSafeBroker.Key<Int>

}