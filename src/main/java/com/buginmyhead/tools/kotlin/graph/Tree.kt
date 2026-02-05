package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.graph.AcyclicGraph.Companion.toAcyclicGraph
import com.buginmyhead.tools.kotlin.graph.Graph.Direction

/**
 * [Direction.Forward] means from root to leaves.
 * [Direction.Backward] means from leaves to root.
 */
interface Tree<N, W> : AcyclicGraph<N, W> {

    companion object {

        @Throws(
            CyclicGraphException::class,
            NotATreeException::class,
        )
        fun <N, W> Graph<N, W>.toTree(): Tree<N, W> {
            // Copies the graph as a Tree.
            // Assures each node has at most one parent.
            // Assures there is only one root node.
            return object : Tree<N, W>, AcyclicGraph<N, W> by toAcyclicGraph() {
                init {
                    if (sourceNodes.size != 1 || ins.values.any { it.size > 1 })
                        throw NotATreeException()
                }
            }
        }

    }

}