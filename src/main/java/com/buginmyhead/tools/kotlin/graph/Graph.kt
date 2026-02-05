package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.graph.MutableGraph.Companion.toMutableGraph
import java.io.Serializable

/**
 * Represents a directed graph structure.
 */
interface Graph<N, W> : Serializable {

    val nodes: Set<N>

    val edges: Map<Pair<N, N>, W>

    val outs: Map<N, Set<N>>

    val ins: Map<N, Set<N>>

    val sinkNodes: Set<N>

    val sourceNodes: Set<N>

    enum class Direction {

        Forward {

            override fun <N> getStartingNodes(graph: Graph<N, *>): Set<N> =
                graph.sourceNodes

            override fun <N> getNextNodes(node: N, graph: Graph<N, *>): Set<N> =
                graph.outs[node].orEmpty()

            override fun <N> getPreviousNodes(node: N, graph: Graph<N, *>): Set<N> =
                graph.ins[node].orEmpty()

        },
        Backward {

            override fun <N> getStartingNodes(graph: Graph<N, *>): Set<N> =
                graph.sinkNodes

            override fun <N> getNextNodes(node: N, graph: Graph<N, *>): Set<N> =
                graph.ins[node].orEmpty()

            override fun <N> getPreviousNodes(node: N, graph: Graph<N, *>): Set<N> =
                graph.outs[node].orEmpty()

        };

        abstract fun <N> getStartingNodes(graph: Graph<N, *>): Set<N>

        abstract fun <N> getNextNodes(node: N, graph: Graph<N, *>): Set<N>

        abstract fun <N> getPreviousNodes(node: N, graph: Graph<N, *>): Set<N>

    }

    companion object {

        fun Graph<*, *>.isEmpty(): Boolean = nodes.isEmpty()

        fun <N, W> Graph<N, W>.toGraph(): Graph<N, W> =
            toGraph({ node -> node }, { _, _, weight, _, _ -> weight })

        fun <N, W, M, V> Graph<N, W>.toGraph(
            nodeTransform: (N) -> M,
            weightTransform: (from: N, to: N, weight: W, tFrom: M, tTo: M) -> V
        ): Graph<M, V> =
            toMutableGraph(nodeTransform, weightTransform)

        fun <N> Graph<N, *>.bfs(
            direction: Direction,
            startNodes: Iterable<N>
        ): Sequence<N> = sequence {
            // `toSet()` preserves the element iteration order of the original collection.
            val filteredStartNodes =
                startNodes.filter { it in nodes }.ifEmpty { return@sequence }.toSet()

            val q = ArrayDeque<N>(nodes.size)
            q.addAll(filteredStartNodes)

            val remainingNodes = nodes.toMutableSet()
            remainingNodes.removeAll(filteredStartNodes)

            while (q.isNotEmpty()) {
                val node = q.removeFirst()
                yield(node)
                for (next in direction.getNextNodes(node, this@bfs)) {
                    if (remainingNodes.remove(next)) q.addLast(next)
                }
            }
        }

    }

}