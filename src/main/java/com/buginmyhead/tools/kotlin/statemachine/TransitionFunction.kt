package com.buginmyhead.tools.kotlin.statemachine

/**
 * A function that defines how the state machine transitions from one state to another
 *  in response to an event.
 *
 * @param R The type of the root state.
 * @param G The type of the global side effect.
 */
fun interface TransitionFunction<R : TypeSafeBroker.Key<*>, G : Any> {

    /**
     * Implementation is highly recommended to be pure.
     *
     * @param states The list of states from the sender to the root state. It guarantees that
     *  the first element is the sender state and the last element is the [root] state.
     */
    fun onEvent(states: List<TypeSafeBroker.Key<*>>, root: R, event: Any): Transition<R, G>

    /**
     * The default implementation of [TransitionFunction]
     *  that creates a [Scope] and passes it to [Scope.onEvent].
     *
     * @param R The type of the root state.
     * @param G The type of the global side effect.
     */
    fun interface WithScope<R : TypeSafeBroker.Key<*>, G : Any> : TransitionFunction<R, G> {

        /**
         * @param G The type of the global side effect.
         *
         * @see TransitionFunction.onEvent
         */
        fun Scope<R, G>.onEvent(
            states: List<TypeSafeBroker.Key<*>>,
            root: R,
            event: Any
        ): Transition<R, G>

        override fun onEvent(
            states: List<TypeSafeBroker.Key<*>>,
            root: R,
            event: Any
        ): Transition<R, G> {
            val scope = object : Scope<R, G> {
                override val stateToEffect = TypeSafeBroker()
            }
            return scope.onEvent(states, root, event)
        }

        /**
         * @param G The type of the 'global' effect, which is not mapped by a certain state.
         */
        interface Scope<R : TypeSafeBroker.Key<*>, G : Any> {

            val stateToEffect: TypeSafeBroker

            fun transit(
                state: R,
                globalEffect: G
            ) = Transition(
                state,
                globalEffect,
                stateToEffect
            )

        }

    }

}