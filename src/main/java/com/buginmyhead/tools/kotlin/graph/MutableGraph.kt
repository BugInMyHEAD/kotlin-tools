package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.graph.Graph.Companion.bfs
import com.buginmyhead.tools.kotlin.graph.Graph.Companion.nodes

class MutableGraph<N, W> : Graph<N, W> {

    override val edges: Map<Pair<N, N>, W>
        field = mutableMapOf<Pair<N, N>, W>()

    override val outs: Map<N, Set<N>>
        field = mutableMapOf<N, MutableSet<N>>()

    override val ins: Map<N, Set<N>>
        field = mutableMapOf<N, MutableSet<N>>()

    override val sourceNodes: Set<N>
        field = mutableSetOf<N>()

    override val sinkNodes: Set<N>
        field = mutableSetOf<N>()

    /**
     * Adds an edge to the graph.
     * If the [edge] already exists, it will be ignored.
     * If the nodes of the [edge] do not exist in the graph, they will be added.
     */
    fun addEdge(edge: Pair<N, N>, weight: W) {
        val (from, to) = edge
        edges[edge] = weight
        addNode(from)
        addNode(to)
        outs[from]?.add(to)
        ins[to]?.add(from)
        sinkNodes -= from
        sourceNodes -= to
    }

    /**
     * Removes an edge from the graph.
     * If the [edge] does not exist in the graph, it will be ignored.
     */
    fun removeEdge(edge: Pair<N, N>) {
        val (from, to) = edge
        edges -= edge
        outs[from]?.remove(to)
        ins[to]?.remove(from)
        if (outs[from].isNullOrEmpty()) {
            sinkNodes += from
        }
        if (ins[to].isNullOrEmpty()) {
            sourceNodes += to
        }
    }

    /**
     * Adds a node to the graph.
     * If the [node] already exists in the graph, it will be ignored.
     */
    fun addNode(node: N) {
        outs.getOrPut(node) {
            sinkNodes += node
            mutableSetOf()
        }
        ins.getOrPut(node) {
            sourceNodes += node
            mutableSetOf()
        }
    }

    /**
     * Removes a node and all edges connected to the node.
     * If the [node] does not exist in the graph, it will be ignored.
     */
    fun removeNode(node: N) {
        outs[node].orEmpty().map { node to it }.forEach(::removeEdge)
        ins[node].orEmpty().map { it to node }.forEach(::removeEdge)
        outs.remove(node)
        ins.remove(node)
        sinkNodes -= node
        sourceNodes -= node
    }

    override fun toString(): String = "MutableGraph(ins=$ins, outs=$outs)"

    override fun equals(other: Any?): Boolean = Graph.areEqual(this, other)

    override fun hashCode(): Int = Graph.hash(this)

    companion object {

        fun <N> MutableGraph<N, Unit>.addEdge(edge: Pair<N, N>) = addEdge(edge, Unit)

        fun <N> from(
            sourceNodes: Iterable<N>,
            nextFunction: (N) -> Iterable<N>
        ): MutableGraph<N, Unit> =
            from(sourceNodes, { _, _ -> }, nextFunction)

        fun <N, W> from(
            sourceNodes: Iterable<N>,
            weightFunction: (N, N) -> W,
            nextFunction: (N) -> Iterable<N>
        ): MutableGraph<N, W> {
            val graph = MutableGraph<N, W>()

            fun build(from: N) {
                graph.addNode(from)
                for (to in nextFunction(from)) {
                    val edge = from to to
                    if (edge in graph.edges) continue
                    graph.addEdge(from to to, weightFunction(from, to))
                    build(to)
                }
            }

            sourceNodes.forEach(::build)
            return graph
        }

        fun <N, W> Graph<N, W>.toMutableGraph(): MutableGraph<N, W> =
            toMutableGraph({ node -> node }) { _, _, weight, _, _ -> weight }

        fun <N, W, M, V> Graph<N, W>.toMutableGraph(
            nodeTransform: (N) -> M,
            weightTransform: (from: N, to: N, weight: W, tFrom: M, tTo: M) -> V
        ): MutableGraph<M, V> {
            val mutableGraph = MutableGraph<M, V>()
            val transformedNodes: Map<N, M> = nodes.associateWith(nodeTransform)

            for ((edge, weight) in edges) {
                val (from, to) = edge
                val transformedFrom = transformedNodes.getValue(from)
                val transformedTo = transformedNodes.getValue(to)
                mutableGraph.edges[transformedFrom to transformedTo] =
                    weightTransform(from, to, weight, transformedFrom, transformedTo)
            }
            for ((n, m) in transformedNodes) {
                mutableGraph.outs[m] =
                    outs[n].orEmpty().mapNotNull { transformedNodes[it] }.toMutableSet()
                mutableGraph.ins[m] =
                    ins[n].orEmpty().mapNotNull { transformedNodes[it] }.toMutableSet()
            }
            mutableGraph.sinkNodes += sinkNodes.mapNotNull { transformedNodes[it] }
            mutableGraph.sourceNodes += sourceNodes.mapNotNull { transformedNodes[it] }
            return mutableGraph
        }

        fun <N, W> Graph<N, W>.filterNodes(
            predicate: (N) -> Boolean
        ): Graph<N, W> =
            toMutableGraph().apply {
                nodes
                    .filterNot(predicate)
                    .forEach(::removeNode)
            }

        fun <N, W> Graph<N, W>.filterEdges(
            predicate: (edge: Pair<N, N>, weight: W) -> Boolean
        ): Graph<N, W> =
            toMutableGraph().apply {
                edges
                    .filterNot { (edge, weight) -> predicate(edge, weight) }
                    .keys
                    .forEach(::removeEdge)
            }

        fun <N, W> Graph<N, W>.selectReachable(
            direction: Graph.Direction,
            startNodes: Iterable<N>
        ): Graph<N, W> {
            val reachableNodes = bfs(direction, startNodes).toSet()
            return filterNodes { it in reachableNodes }
        }

    }

}