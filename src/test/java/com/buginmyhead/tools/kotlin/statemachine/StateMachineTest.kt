package com.buginmyhead.tools.kotlin.statemachine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

internal class StateMachineTest : FreeSpec({
    "pushEvent updates state" {
        val a = State()
        val b = State()
        val transitionFunction = TransitionFunction.WithScope<State, Unit> { states, root, event ->
            b
        }
        val machine = StateMachine(a, transitionFunction)

        machine.state shouldBe a
        machine.pushEvent(a, "event")
        machine.state shouldBe b
    }

    "pushEvent throws if sender is not in stateTree" {
        val b = State()
        val a = State() // a is root and does not contain b
        val transitionFunction = TransitionFunction.WithScope<State, Unit> { states, root, event ->
            root
        }
        val machine = StateMachine(a, transitionFunction)

        shouldThrow<IllegalArgumentException> {
            machine.pushEvent(b, "event")
        }
    }

    "default implementation browses all nested field states" {
        var statesCaptured: List<Any>? = null

        val c = State()
        val b = State(child = c)
        val a = State(child = b)
        val transitionFunction = TransitionFunction.WithScope<State, Unit> { states, root, event ->
            statesCaptured = states
            root
        }
        val machine = StateMachine(a, transitionFunction)

        machine.pushEvent(c, Any())

        statesCaptured shouldBe listOf(c, b, a)
    }

    "default implementation browses all nested states in collection" {
        var statesCaptured: List<Any>? = null

        val c = State()
        val b = State(children = setOf(c))
        val a = State(children = listOf(b))
        val transitionFunction = TransitionFunction.WithScope<State, Unit> { states, root, event ->
            statesCaptured = states
            root
        }
        val machine = StateMachine(a, transitionFunction)

        machine.pushEvent(c, Any())

        statesCaptured shouldBe listOf(c, b, a)
    }

    "TransitionFunction onEvent receives states from sender to root" {
        var statesCaptured: List<Any>? = null

        val c = State()
        val b = State(children = listOf(c))
        val a = State(child = b)
        val transitionFunction = TransitionFunction.WithScope<State, Unit> { states, root, event ->
            statesCaptured = states
            root
        }
        val machine = StateMachine(a, transitionFunction)

        machine.pushEvent(c, "event")
        statesCaptured shouldBe listOf(c, b, a)

        machine.pushEvent(b, "event")
        statesCaptured shouldBe listOf(b, a)

        machine.pushEvent(a, "event")
        statesCaptured shouldBe listOf(a)
    }

    "TransitionFunction onEvent stores effect retrievable by pollEffect and is removed after polling" {
        val b = State()
        val a = State(child = b)
        val transitionFunction = TransitionFunction.WithScope<State, Int> { states, root, event ->
            states.forEach { state ->
                stateToEffect[state as State] = event as Int
            }
            effect = event as Int
            root
        }
        val machine = StateMachine(a, transitionFunction)

        machine.pushEvent(b, 17)
        machine.pollEffect(transitionFunction) shouldBe 17
        machine.pollEffect(transitionFunction) shouldBe null
        machine.pollEffect(b) shouldBe 17
        machine.pollEffect(b) shouldBe null
        machine.pollEffect(a) shouldBe 17
        machine.pollEffect(a) shouldBe null

        machine.pushEvent(a, 19)
        machine.pollEffect(transitionFunction) shouldBe 19
        machine.pollEffect(transitionFunction) shouldBe null
        machine.pollEffect(b) shouldBe null
        machine.pollEffect(b) shouldBe null
        machine.pollEffect(a) shouldBe 19
        machine.pollEffect(a) shouldBe null
    }

    "obtainContext creates context with current state and delegates" {
        val b = State()
        val a = State(child = b)
        val transitionFunction = TransitionFunction.WithScope<State, Unit> { states, root, event ->
             stateToEffect[states.first() as State] = event as Int
             root
        }
        val machine = StateMachine(a, transitionFunction)
        val context = machine.obtainContext()

        context shouldBe machine.obtainContext()
        context.pushEvent(13)
        context.pollEffect() shouldBe 13
        context.pollEffect() shouldBe null
    }

    "Context pushEvent delegates to outer StateMachine pushEvent" {
        val b = State()
        val a = State(child = b)
        val transitionFunction = TransitionFunction.WithScope<State, Int> { _, _, _ -> b }
        val machine = StateMachine(a, transitionFunction)

        val ctx = machine.obtainContext()
        ctx.state shouldBe a

        ctx.pushEvent(13)
        machine.state shouldBe b
    }

    "Context pollEffect delegates to outer StateMachine pollEffect" {
        val state = State()
        val transitionFunction = TransitionFunction.WithScope<State, Int> { states, root, event ->
            stateToEffect[states.first() as State] = event as Int
            root
        }
        val machine = StateMachine(state, transitionFunction)

        machine.pushEvent(state, 13)
        val ctx = machine.obtainContext()
        ctx.pollEffect() shouldBe 13
        ctx.pollEffect() shouldBe null
    }

    "Context with creates new context with provided state and reuses delegates" {
        val b = State()
        val a = State(child = b)
        val transitionFunction = TransitionFunction.WithScope<State, Int> { states, root, event ->
            stateToEffect[states.first() as State] = event as Int
            root
        }
        val machine = StateMachine(a, transitionFunction)

        val ctxA = machine.obtainContext()
        val ctxB = ctxA.with(b)

        ctxB.state shouldBe b
        ctxB.pushEvent(17)
        ctxB.pollEffect() shouldBe 17

        ctxA shouldNotBe ctxB
    }

    "Context equals and hashCode consider state" {
        val state = State()
        val transitionFunction = TransitionFunction.WithScope<State, Int> { _, root, _ -> root }
        val machine = StateMachine(state, transitionFunction)

        val context = machine.obtainContext()

        context shouldBe context
        context.hashCode() shouldBe context.hashCode()
        context shouldNotBe null
        context shouldNotBe Any()

        val sameContext = machine.obtainContext()

        context shouldBe sameContext
        context.hashCode() shouldBe sameContext.hashCode()

        val otherState = State()
        val otherMachine = StateMachine(otherState, transitionFunction)
        val otherContext = otherMachine.obtainContext()
        context shouldNotBe otherContext
    }
}) {

    class State(
        val child: State? = null,
        val children: Collection<State> = emptyList()
    ) : TypeSafeBroker.Key<Int>

}