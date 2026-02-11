package com.buginmyhead.tools.kotlin.statemachine

import com.buginmyhead.tools.kotlin.graph.MutableGraph
import com.buginmyhead.tools.kotlin.graph.Tree
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.ancestorsFrom
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.root
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.toTree
import com.buginmyhead.tools.kotlin.statemachine.StateMachine.Companion.invoke
import java.util.Objects

/**
 * @param S The type of the root state.
 * @param T The type for the root and all nested states.
 */
abstract class StateMachine<S : T, T : TypeSafeBroker.Key<*>>(
    initialState: S
) {

    private lateinit var stateTree: Tree<T, Unit>

    var state: S
        @Suppress("UNCHECKED_CAST")
        get() = stateTree.root as S
        set(value) {
            stateTree =
                MutableGraph
                    .from(setOf(value), ::nestedStatesAt)
                    .toTree()
        }

    init {
        state = initialState
    }

    private val stateToEffect = TypeSafeBroker()

    /**
     * @param states The list of states from the sender to the root state. It guarantees that
     *  the first element is the sender state and the last element is the root state.
     */
    protected abstract fun onEvent(states: List<T>, event: Any): S

    /**
     * Browses first-depth nested states of the [state] state.
     *
     * @param state The root state to browse nested states from.
     * @return An iterable of first-depth nested states found within the [state] state.
     * @see state
     * @see stateTree
     * @see fieldPropertyValues
     * @see collectionPropertyValues
     * @see invoke
     */
    abstract fun nestedStatesAt(state: T): Iterable<T>

    fun pushEvent(sender: T, event: Any) {
        state = onEvent(stateTree.ancestorsFrom(sender).toList(), event)
    }

    protected fun <T : TypeSafeBroker.Key<G>, G : Any> pushEffect(receiver: T, effect: G) {
        stateToEffect[receiver] = effect
    }

    fun <T : TypeSafeBroker.Key<G>, G : Any> pollEffect(receiver: T): G? = stateToEffect.poll(receiver)

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

    interface EffectSender {

        fun <T : TypeSafeBroker.Key<G>, G : Any> pushEffect(receiver: T, effect: G)

    }

    companion object {

        /**
         * The default implementation uses reflection to find
         *  all first-depth properties of type [T].
         */
        inline operator fun <S : T, reified T : TypeSafeBroker.Key<*>> invoke(
            initialState: S,
            crossinline nestedStatesAt: (state: T) -> Iterable<T> =
                { it.fieldPropertyValues() + it.collectionPropertyValues() },
            crossinline onEvent: EffectSender.(states: List<T>, root: S, event: Any) -> S,
        ) = object : StateMachine<S, T>(initialState) {
            /**
             * Kotlin does not have a syntactic way to reference this `object` in a nested class.
             */
            private val thisStateMachine = this

            private val effectSender: EffectSender = object : EffectSender {
                override fun <T : TypeSafeBroker.Key<G>, G : Any> pushEffect(receiver: T, effect: G) =
                    thisStateMachine.pushEffect(receiver, effect)
            }

            override fun nestedStatesAt(state: T): Iterable<T> =
                nestedStatesAt(state)

            override fun onEvent(states: List<T>, event: Any): S =
                effectSender.onEvent(states, state, event)
        }

    }

}