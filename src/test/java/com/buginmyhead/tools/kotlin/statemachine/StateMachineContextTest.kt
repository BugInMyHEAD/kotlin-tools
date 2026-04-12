package com.buginmyhead.tools.kotlin.statemachine

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

internal class StateMachineContextTest : FreeSpec(
    {
        "pushEvent delegates to provided pushEvent" {
            var stateCaptured: Any? = null
            var eventCaptured: Any? = null
            val pushEvent = { state: Any, event: Any ->
                stateCaptured = state
                eventCaptured = event
            }

            val state = State("A")
            val ctx = StateMachine.Context(state, pushEvent, { null })

            ctx.pushEvent("ev")
            eventCaptured shouldBe "ev"
            stateCaptured shouldBe state
        }

        "pollEffect delegates and casts result" {
            var stateCaptured: Any? = null
            val pollEffect = { state: Any ->
                stateCaptured = state
                13
            }

            val state = State("A")
            val ctx = StateMachine.Context(state, { _, _ -> }, pollEffect)

            ctx.pollEffect() shouldBe 13
            stateCaptured shouldBe state
        }

        "with creates new context with provided state and reuses delegates" {
            var stateCaptured: Any? = null
            var eventCaptured: Any? = null
            val pushEvent = { state: Any, event: Any ->
                stateCaptured = state
                eventCaptured = event
            }
            var polledState: Any? = null
            val pollEffect = { state: Any ->
                polledState = state
                17
            }

            val stateA = State("A")
            val stateB = State("B")
            val ctxA = StateMachine.Context(stateA, pushEvent, pollEffect)
            val ctxB = ctxA.with(stateB)

            ctxB.pushEvent("evt")
            stateCaptured shouldBe stateB
            eventCaptured shouldBe "evt"

            ctxB.pollEffect() shouldBe 17
            polledState shouldBe stateB

            ctxA shouldNotBe ctxB
        }

        "equals and hashCode consider state and delegates" {
            val pushEvent = { _: Any, _: Any -> }
            val pollEffect = { _: Any -> 13 }
            val state = State("A")

            val a = StateMachine.Context(state, pushEvent, pollEffect)

            a shouldBe a
            a.hashCode() shouldBe a.hashCode()
            a shouldNotBe null
            a shouldNotBe Any()

            val b = StateMachine.Context(state, pushEvent, pollEffect)

            a shouldBe b
            a.hashCode() shouldBe b.hashCode()

            val c = StateMachine.Context(state, { _, _ -> }, pollEffect)
            a shouldNotBe c
        }
    }
) {

    private data class State(val value: String) : TypeSafeBroker.Key<Int>

}