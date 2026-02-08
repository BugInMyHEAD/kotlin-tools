package com.buginmyhead.tools.kotlin.graph

/**
 * It is a simple wrapper over a [Graph] to mark it as immutable.
 * Actual immutability can be achieved by encapsulating access to the mutable delegate.
 */
interface ImmutableGraph<N, W> : Graph<N, W> {

    companion object {

        operator fun <N, W> invoke(original: Graph<N, W>): Graph<N, W> =
            object : ImmutableGraph<N, W>, Graph<N, W> by original {}

    }

}