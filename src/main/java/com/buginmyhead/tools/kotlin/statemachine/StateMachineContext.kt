package com.buginmyhead.tools.kotlin.statemachine

import java.util.Objects

class StateMachineContext<S : TypeSafeBroker.Key<F>, F : Any>(
    val state: S,
    private val pushEvent: (event: Any, state: Any) -> Unit,
    private val pollEffect: (state: Any) -> Any?,
) {

    fun pushEvent(event: Any): Unit = pushEvent(event, state)

    @Suppress("UNCHECKED_CAST")
    fun pollEffect(): F? = pollEffect(state) as F?

    fun <T : TypeSafeBroker.Key<G>, G : Any> with(state: T) =
        StateMachineContext(state, pushEvent, pollEffect)

    override fun equals(other: Any?): Boolean =
        this === other
                || (
                other is StateMachineContext<*, *>
                        && state == other.state
                        && pushEvent == other.pushEvent
                        && pollEffect == other.pollEffect
                )

    override fun hashCode(): Int = Objects.hash(state, pushEvent, pollEffect)

}