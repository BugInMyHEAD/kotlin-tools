package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.graph.Graph.Companion.isEmpty
import com.buginmyhead.tools.kotlin.graph.Graph.Companion.toGraph

/**
 * Represents a directed acyclic graph structure.
 */
interface AcyclicGraph<N, W> : Graph<N, W> {

    companion object {

        fun <N, W> Graph<N, W>.toAcyclicGraph(): AcyclicGraph<N, W> {
            // Copies the graph as an AcyclicGraph.
            // All nodes have been visited without detecting a cycle.
            return object : AcyclicGraph<N, W>, Graph<N, W> by toGraph() {
                init {
                    // May throw CyclicGraphException
                    topologicalSort(Graph.Direction.Forward).forEach { }
                }
            }
        }

        // CyclicGraphException should not be thrown
        //  because it should be thrown on AcyclicGraph creation.
        fun <N> AcyclicGraph<N, *>.topologicalSort(direction: Graph.Direction): Sequence<N> = sequence {
            if (isEmpty()) return@sequence

            // Since the graph is acyclic, there should be at least one source and one sink.
            // If not, this [AcyclicGraph] is invalid.
            if (sourceNodes.isEmpty() || sinkNodes.isEmpty()) throw CyclicGraphException()

            val q = ArrayDeque<N>(nodes.size)
            q.addAll(direction.getStartingNodes(this@topologicalSort))

            val nodeToInDegree = nodes.associateWith { node ->
                direction.getPreviousNodes(node, this@topologicalSort).size
            }.toMutableMap()

            while (q.isNotEmpty()) {
                val node = q.removeFirst()
                yield(node)
                for (next in direction.getNextNodes(node, this@topologicalSort)) {
                    val inDegree = nodeToInDegree.getValue(next) - 1
                    if (inDegree < 0) throw CyclicGraphException()
                    nodeToInDegree[next] = inDegree
                    if (inDegree == 0) q.addLast(next)
                }
            }
        }

    }

}