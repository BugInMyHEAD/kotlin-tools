package com.buginmyhead.tools.kotlin.statemachine

fun interface TransitionFunction<S : T, T : TypeSafeBroker.Key<*>> {

    /**
     * Implementation is highly recommended to be pure.
     *
     * @param states The list of states from the sender to the root state. It guarantees that
     *  the first element is the sender state and the last element is the [root] state.
     */
    fun onEvent(states: List<T>, root: S, event: Any): Transition<S>

    /**
     * The default implementation of [TransitionFunction]
     *  that creates a [Scope] and passes it to [Scope.onEvent].
     */
    fun interface WithScope<S : T, T : TypeSafeBroker.Key<*>> : TransitionFunction<S, T> {

        /**
         * @see TransitionFunction.onEvent
         */
        fun Scope.onEvent(states: List<T>, root: S, event: Any): S

        override fun onEvent(states: List<T>, root: S, event: Any): Transition<S> {
            val scope = object : Scope {
                override val stateToEffect = TypeSafeBroker()
            }
            return Transition(scope.onEvent(states, root, event), scope.stateToEffect)
        }

    }

    interface Scope {

        val stateToEffect: TypeSafeBroker

    }

}