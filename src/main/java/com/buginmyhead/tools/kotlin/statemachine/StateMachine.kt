package com.buginmyhead.tools.kotlin.statemachine

import com.buginmyhead.tools.kotlin.graph.MutableGraph
import com.buginmyhead.tools.kotlin.graph.Tree
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.ancestorsFrom
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.root
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.toTree

/**
 * A framework-agnostic state transition engine.
 *
 * The caller is responsible for:
 * - **State observation**: Wrapping [state] with a reactive primitive
 *   (e.g., `StateFlow`, `LiveData`, Compose `State`) after each [pushEvent].
 * - **Thread safety**: Confining all [pushEvent] and [pollEffect] calls
 *   to a single thread (e.g., main thread), or using a `Mutex`.
 * - **State persistence**: Saving [state] to a persistence mechanism
 *   (e.g., `SavedStateHandle`) and restoring it via [initialState].
 *
 * @param S The type of the root state.
 * @param initialState The initial state of the state machine.
 *  Can be a restored state from a persistence mechanism.
 * @param transitionFunction A pure function to determine the next state and effects
 *  when an event is pushed.
 * @param nestedStatesAt How to browse first-depth nested states of the [state].
 *  This is repetitively executed to build the [stateTree].
 *  You can consider using such as [fieldPropertyValues] and/or [collectionPropertyValues].
 *  The default implementation uses Kotlin reflection (`memberProperties`).
 *  To avoid the `kotlin-reflect` dependency,
 *  provide an explicit lambda that returns the nested states manually.
 */
class StateMachine<S : TypeSafeBroker.Key<F>, F : Any>(
    initialState: S,
    private val transitionFunction: TransitionFunction<S, *>,
    private val nestedStatesAt: (state: TypeSafeBroker.Key<*>) -> Iterable<TypeSafeBroker.Key<*>> =
        { it.fieldPropertyValues() + it.collectionPropertyValues() },
) {

    /**
     * To find the path of states from the sender to the root state when an event is pushed.
     */
    private lateinit var stateTree: Tree<TypeSafeBroker.Key<*>, Unit>

    @Suppress("UNCHECKED_CAST")
    var state: S
        get() = stateTree.root as S
        private set(value) {
            stateTree =
                MutableGraph
                    .from(setOf(value), nestedStatesAt)
                    .toTree()
        }

    init {
        state = initialState
    }

    private var stateToEffect = TypeSafeBroker()

    /**
     * Pushes an [event] from the [sender] state to the state machine.
     *
     * Effects produced by this transition are accumulated into the internal broker
     *  via [TypeSafeBroker.plusAssign]. If the same [TypeSafeBroker.Key] produces an effect
     *  in two consecutive [pushEvent] calls before [pollEffect] is called,
     *  the earlier effect is overwritten by the later one.
     *
     * @param sender A state node that must exist in the current [stateTree].
     * @param event The event to push to the [transitionFunction].
     * @throws IllegalArgumentException if [sender] is not in the current [stateTree].
     */
    fun pushEvent(sender: TypeSafeBroker.Key<*>, event: Any) {
        require(sender in stateTree.nodes) {
            "The sender state does not exist in the current state tree."
        }

        val transition =
            transitionFunction.onEvent(stateTree.ancestorsFrom(sender).toList(), state, event)
        state = transition.state
        stateToEffect += transition.stateToEffect
    }

    /**
     * Removes and returns the effect associated with the [receiver] state,
     *  or `null` if no effect exists for it.
     */
    fun <T : TypeSafeBroker.Key<G>, G : Any> pollEffect(receiver: T): G? =
        stateToEffect.poll(receiver)

    /**
     * Creates a [Context] that bundles the current [state] with [pushEvent] and [pollEffect].
     * Intended for reducing boilerplate when passing state and callbacks down a UI tree.
     *
     * The returned [Context] captures [state] at call time.
     * It is designed to have the same life as [state].
     */
    fun obtainContext() = Context(this, state)

    /**
     * A parameter object that bundles a [state] with [pushEvent] and [pollEffect] delegates.
     *
     * Use [with] to navigate to a nested state's context
     *  while reusing the same delegates.
     */
    class Context<S : TypeSafeBroker.Key<F>, F : Any, T : TypeSafeBroker.Key<G>, G : Any>(
        private val stateMachine: StateMachine<S, F>,
        val state: T,
    ) {

        fun pushEvent(event: Any) = stateMachine.pushEvent(state, event)

        @Suppress("UNCHECKED_CAST")
        fun pollEffect(): G? = stateMachine.pollEffect(state)

        /**
         * Creates a new [Context] with the given nested [state],
         *  reusing the same [pushEvent] and [pollEffect] delegates.
         */
        fun <U : TypeSafeBroker.Key<H>, H : Any> with(state: U) =
            Context(stateMachine, state)

        override fun equals(other: Any?): Boolean =
            this === other || (
                    other is Context<*, *, *, *>
                            && stateMachine === other.stateMachine
                            && state == other.state
                    )

        override fun hashCode(): Int = state.hashCode()

    }

}