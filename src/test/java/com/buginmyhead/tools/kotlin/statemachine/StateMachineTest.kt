package com.buginmyhead.tools.kotlin.statemachine

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

internal class StateMachineTest : FreeSpec({
    "pushEvent updates state" {
        val a = State()
        val b = State()
        val machine = StateMachine(a) { _, _ ->
            b
        }

        machine.state shouldBe a
        machine.pushEvent(a, "event")
        machine.state shouldBe b
    }

    "default implementation browses all nested field states" {
        var statesCaptured: List<State>? = null

        val c = State()
        val b = State(child = c)
        val a = State(child = b)
        val machine = StateMachine(a) { states, event ->
            statesCaptured = states
            states.last()
        }

        machine.pushEvent(c, Any())

        statesCaptured shouldBe listOf(c, b, a)
    }

    "default implementation browses all nested states in collection" {
        var statesCaptured: List<State>? = null

        val c = State()
        val b = State(children = setOf(c))
        val a = State(children = listOf(b))
        val machine = StateMachine(a) { states, event ->
            statesCaptured = states
            states.last()
        }

        machine.pushEvent(c, Any())

        statesCaptured shouldBe listOf(c, b, a)
    }

    "onEvent receives ancestors from sender to root (sender first, root last)" {
        var statesCaptured: List<State>? = null

        val c = State()
        val b = State(children = listOf(c))
        val a = State(child = b)
        val machine = StateMachine(a) { states, _ ->
            statesCaptured = states
            states.last()
        }

        machine.pushEvent(c, "event")
        statesCaptured shouldBe listOf(c, b, a)

        machine.pushEvent(b, "event")
        statesCaptured shouldBe listOf(b, a)

        machine.pushEvent(a, "event")
        statesCaptured shouldBe listOf(a)
    }

    "pushEffect stores effect retrievable by pollEffect and is removed after polling" {
        val b = State()
        val a = State(child = b)
        val machine = StateMachine(a) { states, event ->
            states.forEach { state ->
                pushEffect(state, event as Int)
            }
            states.last()
        }

        machine.pushEvent(b, 5)
        machine.pollEffect(b) shouldBe 5
        machine.pollEffect(b) shouldBe null
        machine.pollEffect(a) shouldBe 5
        machine.pollEffect(a) shouldBe null

        machine.pushEvent(a, 7)
        machine.pollEffect(b) shouldBe null
        machine.pollEffect(b) shouldBe null
        machine.pollEffect(a) shouldBe 7
        machine.pollEffect(a) shouldBe null
    }

}) {

    class State(
        val child: State? = null,
        val children: Collection<State> = emptyList()
    ) : TypeSafeBroker.Key<Int>

}