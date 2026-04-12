package com.buginmyhead.tools.kotlin.statemachine

fun interface TransitionFunction<S : TypeSafeBroker.Key<*>, F : Any> : TypeSafeBroker.Key<F> {

    /**
     * Implementation is highly recommended to be pure.
     *
     * @param states The list of states from the sender to the root state. It guarantees that
     *  the first element is the sender state and the last element is the [root] state.
     */
    fun onEvent(states: List<TypeSafeBroker.Key<*>>, root: S, event: Any): Transition<S>

    /**
     * The default implementation of [TransitionFunction]
     *  that creates a [Scope] and passes it to [Scope.onEvent].
     */
    fun interface WithScope<S : TypeSafeBroker.Key<*>, F : Any> : TransitionFunction<S, F> {

        /**
         * @see TransitionFunction.onEvent
         */
        fun Scope<F>.onEvent(states: List<TypeSafeBroker.Key<*>>, root: S, event: Any): S

        override fun onEvent(states: List<TypeSafeBroker.Key<*>>, root: S, event: Any): Transition<S> {
            val scope = object : Scope<F> {

                override val stateToEffect = TypeSafeBroker()

                override var effect: F? = null

            }
            val nextState = scope.onEvent(states, root, event)
            scope.effect?.also { scope.stateToEffect[this] = it }
            return Transition(nextState, scope.stateToEffect)
        }

    }

    interface Scope<F : Any> {

        val stateToEffect: TypeSafeBroker
        var effect: F?

    }

}