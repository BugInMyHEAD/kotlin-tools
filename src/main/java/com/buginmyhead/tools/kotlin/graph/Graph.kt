package com.buginmyhead.tools.kotlin.graph

import java.io.Serializable

/**
 * Represents a directed graph structure.
 */
interface Graph<N, W> : Serializable {

    /**
     * A [Map] that has a pair of nodes as a key and the weight of the edge between them as a value.
     * - ```edges[Pair(from, to)]``` returns `null` if there is no edge from `from` to `to`.
     */
    val edges: Map<Pair<N, N>, W>

    /**
     * A [Map] that has the node as a key and the [Set] of its outgoing neighbors as a value.
     * - ```outs[node]``` returns `null` if the node is not present.
     * - ```outs[node]``` returns an empty [Set] if the node is present,
     *  but it does not have any outgoing neighbors.
     */
    val outs: Map<N, Set<N>>

    /**
     * A [Map] that has the node as a key and the [Set] of its incoming neighbors as a value.
     * - ```ins[node]``` returns `null` if the `node` is not present.
     * - ```ins[node]``` returns an empty [Set] if the node is present,
     *  but it does not have any incoming neighbors.
     */
    val ins: Map<N, Set<N>>

    /** A [Set] of all nodes that do not have incoming neighbors. */
    val sourceNodes: Set<N>

    /** A [Set] of all nodes that do not have outgoing neighbors. */
    val sinkNodes: Set<N>

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

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

        /** A [Set] of all nodes in the graph. */
        val <N> Graph<N, *>.nodes: Set<N> get() = outs.keys

        fun Graph<*, *>.isEmpty(): Boolean = nodes.isEmpty()

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