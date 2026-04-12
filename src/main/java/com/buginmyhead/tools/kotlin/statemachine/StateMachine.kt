package com.buginmyhead.tools.kotlin.statemachine

import com.buginmyhead.tools.kotlin.graph.MutableGraph
import com.buginmyhead.tools.kotlin.graph.Tree
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.ancestorsFrom
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.root
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.toTree
import java.util.Objects

/**
 * @param S The type of the root state.
 * @param transitionFunction A pure function to determine the next state and effects
 *  when an event is pushed.
 * @param nestedStatesAt How to browse first-depth nested states of the [state].
 *  This is repetitively executed to build the [stateTree].
 *  You can consider using such as [fieldPropertyValues] and/or [collectionPropertyValues].
 */
class StateMachine<S : TypeSafeBroker.Key<*>>(
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

    fun pushEvent(sender: TypeSafeBroker.Key<*>, event: Any) {
        val transition =
            transitionFunction.onEvent(stateTree.ancestorsFrom(sender).toList(), state, event)
        state = transition.state
        stateToEffect += transition.stateToEffect
    }

    fun <T : TypeSafeBroker.Key<G>, G : Any> pollEffect(receiver: T): G? =
        stateToEffect.poll(receiver)

    fun obtainContext() = Context(
        state,
        pushEvent = ::pushEvent,
        pollEffect = ::pollEffect,
    )

    class Context<T : TypeSafeBroker.Key<G>, G : Any>(
        val state: T,
        private val pushEvent: (state: TypeSafeBroker.Key<*>, event: Any) -> Unit,
        private val pollEffect: (state: TypeSafeBroker.Key<*>) -> Any?,
    ) {

        fun pushEvent(event: Any) = pushEvent(state, event)

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

}