package com.buginmyhead.tools.kotlin.statemachine

import java.util.Objects

class StateMachineContext<T : TypeSafeBroker.Key<F>, F : Any>(
    val state: T,
    private val pushEvent: (event: Any, state: Any) -> Unit,
    private val pollEffect: (state: Any) -> Any?,
) {

    fun pushEvent(event: Any): Unit = pushEvent(event, state)

    @Suppress("UNCHECKED_CAST")
    fun pollEffect(): F? = pollEffect(state) as F?

    fun <U : TypeSafeBroker.Key<G>, G : Any> with(state: U) =
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