package com.buginmyhead.tools.kotlin.statemachine

import com.buginmyhead.tools.kotlin.collectionPropertyValues
import com.buginmyhead.tools.kotlin.fieldPropertyValues
import com.buginmyhead.tools.kotlin.graph.MutableGraph
import com.buginmyhead.tools.kotlin.graph.Tree
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.ancestorsFrom
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.root
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.toTree
import com.buginmyhead.tools.kotlin.statemachine.StateMachine.Companion.invoke

/**
 * @param S The type of the root state, extending [T].
 * @param T The base type for all states, extending [TypeSafeBroker.Key].
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
                    .from(setOf(value), ::browseStates)
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
     * Browses first-depth nested states of the [root] state.
     *
     * @param root The root state to browse nested states from.
     * @return An iterable of first-depth nested states found within the [root] state.
     * @see state
     * @see stateTree
     * @see fieldPropertyValues
     * @see collectionPropertyValues
     * @see invoke
     */
    abstract fun browseStates(root: T): Iterable<T>

    fun pushEvent(sender: T, event: Any) {
        state = onEvent(stateTree.ancestorsFrom(sender).toList(), event)
    }

    protected fun <T : TypeSafeBroker.Key<F>, F : Any> pushEffect(receiver: T, effect: F) {
        stateToEffect[receiver] = effect
    }

    fun <T : TypeSafeBroker.Key<F>, F : Any> pollEffect(state: T): F? = stateToEffect.poll(state)

    interface EffectSender {

        fun <T : TypeSafeBroker.Key<F>, F : Any> pushEffect(receiver: T, effect: F)

    }

    companion object {

        /**
         * The default implementation uses reflection to find
         *  all first-depth properties of type [S].
         */
        inline operator fun <S : T, reified T : TypeSafeBroker.Key<*>> invoke(
            initialState: S,
            crossinline browseStates: (root: T) -> Iterable<T> =
                { it.fieldPropertyValues() + it.collectionPropertyValues() },
            crossinline onEvent: EffectSender.(states: List<T>, event: Any) -> S,
        ) = object : StateMachine<S, T>(initialState) {
            /**
             * Kotlin does not have a syntactic way to reference this `object` in a nested class.
             */
            private val thisStateMachine = this

            val effectSender: EffectSender = object : EffectSender {
                override fun <T : TypeSafeBroker.Key<F>, F : Any> pushEffect(receiver: T, effect: F) =
                    thisStateMachine.pushEffect(receiver, effect)
            }

            override fun browseStates(root: T): Iterable<T> =
                browseStates(root)

            override fun onEvent(states: List<T>, event: Any): S =
                effectSender.onEvent(states, event)
        }

    }

}