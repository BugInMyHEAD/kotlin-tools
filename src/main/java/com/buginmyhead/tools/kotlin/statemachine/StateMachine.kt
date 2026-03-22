package com.buginmyhead.tools.kotlin.statemachine

import com.buginmyhead.tools.kotlin.graph.MutableGraph
import com.buginmyhead.tools.kotlin.graph.Tree
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.ancestorsFrom
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.root
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.toTree
import java.util.Objects

/**
 * @param S The type of the root state.
 * @param T The type for the root and all nested states.
 * @param transitionFunction A pure function to determine the next state and effects
 *  when an event is pushed.
 * @param nestedStatesAt How to browse first-depth nested states of the [state].
 *  This is repetitively executed to build the [stateTree].
 *  You can consider using such as [fieldPropertyValues] and/or [collectionPropertyValues].
 *
 * @see Companion.invoke
 */
class StateMachine<S : T, T : TypeSafeBroker.Key<*>>(
    initialState: S,
    private val transitionFunction: TransitionFunction<S, T>,
    private val nestedStatesAt: (state: T) -> Iterable<T>,
) {

    /**
     * To find the path of states from the sender to the root state when an event is pushed.
     */
    private lateinit var stateTree: Tree<T, Unit>

    var state: S
        @Suppress("UNCHECKED_CAST")
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

    fun pushEvent(sender: T, event: Any) {
        val transition =
            transitionFunction.onEvent(stateTree.ancestorsFrom(sender).toList(), state, event)
        state = transition.state
        stateToEffect = transition.stateToEffect
    }

    fun <T : TypeSafeBroker.Key<G>, G : Any> pollEffect(receiver: T): G? =
        stateToEffect.poll(receiver)

    @Suppress("UNCHECKED_CAST")
    fun <U : TypeSafeBroker.Key<H>, H : Any> obtainContext(state: U) = Context(
        state,
        pushEvent = { state, event -> pushEvent(state as T, event) },
        pollEffect = { state -> pollEffect(state as TypeSafeBroker.Key<*>) },
    )

    class Context<T : TypeSafeBroker.Key<G>, G : Any>(
        val state: T,
        private val pushEvent: (state: Any, event: Any) -> Unit,
        private val pollEffect: (state: Any) -> Any?,
    ) {

        fun pushEvent(event: Any): Unit = pushEvent(state, event)

        @Suppress("UNCHECKED_CAST")
        fun pollEffect(): G? = pollEffect(state) as G?

        fun <U : TypeSafeBroker.Key<H>, H : Any> with(state: U) =
            Context(state, pushEvent, pollEffect)

        override fun equals(other: Any?): Boolean =
            this === other
                    || (
                    other is Context<*, *>
                            && state == other.state
                            && pushEvent == other.pushEvent
                            && pollEffect == other.pollEffect
                    )

        override fun hashCode(): Int = Objects.hash(state, pushEvent, pollEffect)

    }

    companion object {

        /**
         * @param nestedStatesAt The default argument uses
         *  [fieldPropertyValues] and [collectionPropertyValues] to find nested states recursively.
         *
         * @see [StateMachine.nestedStatesAt]
         */
        inline operator fun <S : T, reified T : TypeSafeBroker.Key<*>> invoke(
            initialState: S,
            transitionFunction: TransitionFunction<S, T>,
            noinline nestedStatesAt: (state: T) -> Iterable<T> =
                { it.fieldPropertyValues() + it.collectionPropertyValues() },
        ) = StateMachine(
            initialState,
            transitionFunction,
            nestedStatesAt,
        )

    }

}