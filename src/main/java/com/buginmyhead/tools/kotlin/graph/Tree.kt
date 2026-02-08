package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.graph.AcyclicGraph.Companion.toAcyclicGraph
import com.buginmyhead.tools.kotlin.graph.Graph.Companion.bfs
import com.buginmyhead.tools.kotlin.graph.Graph.Direction
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.leaves
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.root

/**
 * [Direction.Forward] means from [root] to [leaves].
 * [Direction.Backward] means from [leaves] to [root].
 */
interface Tree<N, W> : AcyclicGraph<N, W> {

    companion object {

        @Throws(NotATreeException::class)
        fun <N, W> Graph<N, W>.toTree(): Tree<N, W> = try {
            this as? Tree<N, W>
                // Copies the graph as a Tree.
                ?: object : Tree<N, W>, AcyclicGraph<N, W> by toAcyclicGraph() {
                    init {
                        if (sourceNodes.size != 1)
                            throw NotATreeException("Multiple root node candidates found.")
                        if (ins.values.any { it.size > 1 })
                            throw NotATreeException("A node with multiple parents found.")
                    }
                }
        } catch (cause: CyclicGraphException) {
            throw NotATreeException(cause)
        }

        inline val <N> Tree<N, *>.root: N get() = sourceNodes.single()

        inline val <N> Tree<N, *>.leaves: Set<N> get() = sinkNodes

        /**
         * Guarantees that there are [node] at the first and [root] at the last.
         */
        fun <N> Tree<N, *>.ancestorsFrom(node: N): Sequence<N> =
            bfs(Graph.Direction.Backward, setOf(node))

        /**
         * @return A subtree rooted at [node], copying contents of the receiver lazily.
         * @throws IllegalArgumentException if [node] does not exist in the receiver.
         */
        @Throws(IllegalArgumentException::class)
        fun <N, W> Tree<N, W>.subtreeOf(node: N): Tree<N, W> {
            require(node in nodes) {
                "The specified root node does not exist in the original tree."
            }
            return LazySubtree(this, node)
        }

    }

}

private class LazySubtree<N, W>(
    original: Tree<N, W>,
    root: N
) : Tree<N, W> {

    private val subtree: Tree<N, W> by lazy { Subtree(original, root) }

    override val nodes: Set<N> get() = subtree.nodes
    override val edges: Map<Pair<N, N>, W> get() = subtree.edges
    override val outs: Map<N, Set<N>> get() = subtree.outs
    override val ins: Map<N, Set<N>> get() = subtree.ins
    override val sinkNodes: Set<N> get() = subtree.sinkNodes
    override val sourceNodes: Set<N> get() = subtree.sourceNodes

}

private class Subtree<N, W>(
    original: Tree<N, W>,
    root: N
) : Tree<N, W> {

    override val nodes: Set<N> =
        original.bfs(Graph.Direction.Forward, setOf(root)).toSet()

    override val edges: Map<Pair<N, N>, W> =
        original.edges.filter { (arrow, weight) -> arrow.first in nodes }

    override val outs: Map<N, Set<N>> =
        original.outs.filter { (node, neighbors) -> node in nodes }

    override val ins: Map<N, Set<N>> =
        original.ins.filter { (node, neighbors) -> node in nodes } + (root to emptySet())

    override val sinkNodes: Set<N> =
        original.sinkNodes.filter { it in nodes }.toSet()

    override val sourceNodes: Set<N> =
        setOf(root)

}