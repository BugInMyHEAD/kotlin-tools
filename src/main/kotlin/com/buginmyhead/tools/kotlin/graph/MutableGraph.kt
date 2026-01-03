package com.buginmyhead.tools.kotlin.graph

import java.util.Objects

class MutableGraph<N, W> : Graph<N, W> {

    private val _edges = mutableMapOf<Pair<N, N>, W>()
    override val edges: Map<Pair<N, N>, W> get() = _edges

    private val _outs = mutableMapOf<N, MutableSet<N>>()
    override val outs: Map<N, Set<N>> get() = _outs

    private val _ins = mutableMapOf<N, MutableSet<N>>()
    override val ins: Map<N, Set<N>> get() = _ins

    override val nodes: Set<N> get() = _outs.keys

    private val _sinkNodes = mutableSetOf<N>()
    override val sinkNodes: Set<N> get() = _sinkNodes

    private val _sourceNodes = mutableSetOf<N>()
    override val sourceNodes: Set<N> get() = _sourceNodes

    /**
     * Adds an edge to the graph.
     * If the [edge] already exists, it will be ignored.
     * If the nodes of the [edge] do not exist in the graph, they will be added.
     */
    fun addEdge(edge: Pair<N, N>, weight: W) {
        val (from, to) = edge
        _edges[edge] = weight
        addNode(from)
        addNode(to)
        _outs[from]?.add(to)
        _ins[to]?.add(from)
        _sinkNodes -= from
        _sourceNodes -= to
    }

    /**
     * Removes an edge from the graph.
     * If the [edge] does not exist in the graph, it will be ignored.
     */
    fun removeEdge(edge: Pair<N, N>) {
        val (from, to) = edge
        _edges -= edge
        _outs[from]?.remove(to)
        _ins[to]?.remove(from)
        if (_outs[from].isNullOrEmpty()) {
            _sinkNodes += from
        }
        if (_ins[to].isNullOrEmpty()) {
            _sourceNodes += to
        }
    }

    /**
     * Adds a node to the graph.
     * If the [node] already exists in the graph, it will be ignored.
     */
    fun addNode(node: N) {
        _outs.getOrPut(node) {
            _sinkNodes += node
            mutableSetOf()
        }
        _ins.getOrPut(node) {
            _sourceNodes += node
            mutableSetOf()
        }
    }

    /**
     * Removes a node and all edges connected to the node.
     * If the [node] does not exist in the graph, it will be ignored.
     */
    fun removeNode(node: N) {
        _outs[node].orEmpty().map { node to it }.forEach(::removeEdge)
        _ins[node].orEmpty().map { it to node }.forEach(::removeEdge)
        _outs.remove(node)
        _ins.remove(node)
        _sinkNodes -= node
        _sourceNodes -= node
    }

    override fun toString(): String = "MutableGraph(ins=$ins, outs=$outs)"

    override fun equals(other: Any?): Boolean =
        this === other
                ||
                (other is Graph<*, *>
                        && this.edges == other.edges
                        && this.sourceNodes == other.sourceNodes
                        && this.sinkNodes == other.sinkNodes)

    override fun hashCode(): Int = Objects.hash(
        edges,
        sourceNodes,
        sinkNodes,
    )

    companion object {

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
                mutableGraph._edges[transformedFrom to transformedTo] =
                    weightTransform(from, to, weight, transformedFrom, transformedTo)
            }
            for ((n, m) in transformedNodes) {
                mutableGraph._outs[m] =
                    outs[n].orEmpty().mapNotNull { transformedNodes[it] }.toMutableSet()
                mutableGraph._ins[m] =
                    ins[n].orEmpty().mapNotNull { transformedNodes[it] }.toMutableSet()
            }
            mutableGraph._sinkNodes += sinkNodes.mapNotNull { transformedNodes[it] }
            mutableGraph._sourceNodes += sourceNodes.mapNotNull { transformedNodes[it] }
            return mutableGraph
        }

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

    }

}