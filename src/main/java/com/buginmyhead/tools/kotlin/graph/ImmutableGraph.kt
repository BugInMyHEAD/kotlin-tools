package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.graph.MutableGraph.Companion.toMutableGraph

/**
 * It is a simple wrapper over a [Graph] to mark it as immutable.
 * Actual immutability can be achieved by encapsulating access to the mutable delegate.
 *
 * @see [AcyclicGraph]
 * @see [Tree]
 */
interface ImmutableGraph<N, W> : Graph<N, W> {

    companion object {

        operator fun <N, W> invoke(original: Graph<N, W>): ImmutableGraph<N, W> =
            object : ImmutableGraph<N, W>, Graph<N, W> by original {}

        /**
         * @return itself if it is immutable,
         *  otherwise a new [ImmutableGraph] copying contents of the receiver.
         */
        fun <N, W> Graph<N, W>.toGraph(): ImmutableGraph<N, W> =
            this as? ImmutableGraph<N, W>
                ?: toGraph({ node -> node }, { _, _, weight, _, _ -> weight })

        /**
         * @return A new immutable [ImmutableGraph] transforming contents of the receiver.
         */
        fun <N, W, M, V> Graph<N, W>.toGraph(
            nodeTransform: (N) -> M,
            weightTransform: (from: N, to: N, weight: W, tFrom: M, tTo: M) -> V
        ) = ImmutableGraph(toMutableGraph(nodeTransform, weightTransform))

    }

}