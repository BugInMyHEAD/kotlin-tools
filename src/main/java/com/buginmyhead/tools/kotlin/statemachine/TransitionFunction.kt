package com.buginmyhead.tools.kotlin.statemachine

fun interface TransitionFunction<S : T, T : TypeSafeBroker.Key<*>> {

    /**
     * Implementation is highly recommended to be pure.
     *
     * @param states The list of states from the sender to the root state. It guarantees that
     *  the first element is the sender state and the last element is the [root] state.
     */
    fun onEvent(states: List<T>, root: S, event: Any): Transition<S>

}