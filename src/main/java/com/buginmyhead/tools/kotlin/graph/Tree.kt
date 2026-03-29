package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.graph.AcyclicGraph.Companion.toAcyclicGraph
import com.buginmyhead.tools.kotlin.graph.Graph.Companion.bfs
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.leaves
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.root

/**
 * [Graph.Direction.Forward] means from [root] to [leaves].
 * [Graph.Direction.Backward] means from [leaves] to [root].
 */
interface Tree<N, W> : AcyclicGraph<N, W> {

    companion object {

        @Throws(NotATreeException::class)
        fun <N, W> Graph<N, W>.toTree(): Tree<N, W> = try {
            this as? Tree<N, W>
                ?: run {
                    val acyclic = toAcyclicGraph()
                    if (acyclic.sourceNodes.size != 1)
                        throw NotATreeException("Multiple root node candidates found.")
                    if (acyclic.ins.values.any { it.size > 1 })
                        throw NotATreeException("A node with multiple parents found.")
                    IndexedTree(acyclic)
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
         * @return A subtree rooted at [node] in O(1) by narrowing the pre-order index range.
         * @throws IllegalArgumentException if [node] does not exist in the receiver.
         */
        @Throws(IllegalArgumentException::class)
        fun <N, W> Tree<N, W>.subtreeOf(node: N): Tree<N, W> {
            require(node in nodes) {
                "The specified root node does not exist in the original tree."
            }
            val indexed = this as? IndexedTree<N, W> ?: IndexedTree(this)
            return indexed.subtreeAt(node)
        }

    }

}

/**
 * An indexed tree backed by a DFS pre-order traversal where all descendants of any node
 * occupy a contiguous index range. A subtree is represented as a narrowed range over this
 * index, making [subtreeAt] an O(1) operation regardless of nesting depth.
 */
private class IndexedTree<N, W> private constructor(
    private val index: TreeIndex<N, W>,
    private val rangeStart: Int,
    rangeEnd: Int,
) : Tree<N, W> {

    constructor(acyclicGraph: AcyclicGraph<N, W>) : this(
        TreeIndex(acyclicGraph),
        0,
        acyclicGraph.nodes.size,
    )

    /** Creates a subtree rooted at [node] in O(1) by narrowing the index range. */
    fun subtreeAt(node: N): IndexedTree<N, W> {
        val idx = index.preOrderedSet.globalIndexOf(node)
        return IndexedTree(index, idx, index.subtreeEnd[idx])
    }

    /** View backed by the pre-order index range. No copy. */
    override val nodes: Set<N> = index.preOrderedSet.subView(rangeStart, rangeEnd)

    override val edges: Map<Pair<N, N>, W> by lazy {
        buildMap {
            for (from in nodes) {
                for (to in index.allOuts[from].orEmpty()) {
                    put(from to to, index.allEdges.getValue(from to to))
                }
            }
        }
    }

    override val outs: Map<N, Set<N>> by lazy {
        nodes.associateWith { index.allOuts[it].orEmpty() }
    }

    override val ins: Map<N, Set<N>> by lazy {
        val root = index.preOrderedSet[rangeStart]
        nodes.associateWith { if (it == root) emptySet() else index.allIns[it].orEmpty() }
    }

    override val sinkNodes: Set<N> by lazy {
        nodes.filterTo(linkedSetOf()) { it in index.allSinkNodes }
    }

    override val sourceNodes: Set<N> by lazy {
        setOf(index.preOrderedSet[rangeStart])
    }

}

/**
 * Pre-computed index structure for efficient subtree operations.
 *
 * Stores a DFS pre-order traversal where all descendants of any node form
 *  a contiguous range &#91;nodeToIndex&#91;node&#93;, subtreeEnd&#91;nodeToIndex&#91;node&#93;).
 */
private class TreeIndex<N, W>(
    acyclicGraph: AcyclicGraph<N, W>,
) {

    val preOrderedSet: ListOrderedSet<N>
    val subtreeEnd: IntArray

    val allEdges: Map<Pair<N, N>, W> = acyclicGraph.edges
    val allOuts: Map<N, Set<N>> = acyclicGraph.outs
    val allIns: Map<N, Set<N>> = acyclicGraph.ins
    val allSinkNodes: Set<N> = acyclicGraph.sinkNodes

    init {
        val root = acyclicGraph.sourceNodes.single()
        val size = acyclicGraph.nodes.size

        val stack = ArrayDeque<N>(size)
        stack.addLast(root)

        // Iterative DFS pre-order traversal
        val order = ArrayList<N>(size)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            order.add(node)
            stack.addAll(acyclicGraph.outs[node].orEmpty())
        }

        preOrderedSet = ListOrderedSet(order)

        // Computes exclusive end index for each node's subtree using reverse pre-order traversal,
        //  a kind of dynamic programming to visit all children before their parent.
        subtreeEnd = IntArray(size)
        for (i in size - 1 downTo 0) {
            val node = preOrderedSet[i]
            val children = acyclicGraph.outs[node].orEmpty()
            subtreeEnd[i] =
                children.fold(i + 1) { maxEnd, child ->
                    maxOf(maxEnd, subtreeEnd[preOrderedSet.globalIndexOf(child)])
                }
        }
    }

}