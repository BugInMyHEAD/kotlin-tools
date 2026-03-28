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
            if (this is IndexedTree<N, W>) {
                require(containsNode(node)) {
                    "The specified root node does not exist in the original tree."
                }
                return subtreeAt(node)
            }
            require(node in nodes) {
                "The specified root node does not exist in the original tree."
            }
            return IndexedTree(this).subtreeAt(node)
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
    private val rangeEnd: Int,
) : Tree<N, W> {

    constructor(acyclicGraph: AcyclicGraph<N, W>) : this(
        TreeIndex(acyclicGraph),
        0,
        acyclicGraph.nodes.size,
    )

    /** O(1) containment check using the index, without materializing [nodes]. */
    fun containsNode(node: N): Boolean {
        val idx = index.nodeToIndex[node] ?: return false
        return idx in rangeStart until rangeEnd
    }

    /** Creates a subtree rooted at [node] in O(1) by narrowing the index range. */
    fun subtreeAt(node: N): IndexedTree<N, W> {
        val idx = index.nodeToIndex.getValue(node)
        return IndexedTree(index, idx, index.subtreeEnd[idx])
    }

    override val nodes: Set<N> by lazy {
        LinkedHashSet<N>(rangeEnd - rangeStart).also { set ->
            for (i in rangeStart until rangeEnd) set.add(index.preOrder[i])
        }
    }

    override val edges: Map<Pair<N, N>, W> by lazy {
        buildMap {
            for (i in rangeStart until rangeEnd) {
                val from = index.preOrder[i]
                for (to in index.allOuts[from].orEmpty()) {
                    put(from to to, index.allEdges.getValue(from to to))
                }
            }
        }
    }

    override val outs: Map<N, Set<N>> by lazy {
        buildMap {
            for (i in rangeStart until rangeEnd) {
                val node = index.preOrder[i]
                put(node, index.allOuts[node].orEmpty())
            }
        }
    }

    override val ins: Map<N, Set<N>> by lazy {
        buildMap {
            for (i in rangeStart until rangeEnd) {
                val node = index.preOrder[i]
                put(node, if (i == rangeStart) emptySet() else index.allIns[node].orEmpty())
            }
        }
    }

    override val sinkNodes: Set<N> by lazy {
        buildSet {
            for (i in rangeStart until rangeEnd) {
                val node = index.preOrder[i]
                if (node in index.allSinkNodes) add(node)
            }
        }
    }

    override val sourceNodes: Set<N> by lazy {
        setOf(index.preOrder[rangeStart])
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

    val preOrder: List<N>
    val nodeToIndex: Map<N, Int>
    val subtreeEnd: IntArray

    val allEdges: Map<Pair<N, N>, W> = acyclicGraph.edges
    val allOuts: Map<N, Set<N>> = acyclicGraph.outs
    val allIns: Map<N, Set<N>> = acyclicGraph.ins
    val allSinkNodes: Set<N> = acyclicGraph.sinkNodes

    init {
        val root = acyclicGraph.sourceNodes.single()
        val size = acyclicGraph.nodes.size
        preOrder = ArrayList(size)

        // Iterative DFS pre-order traversal.
        val stack = ArrayDeque<N>(size)
        stack.addLast(root)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            preOrder.add(node)
            val children = acyclicGraph.outs[node].orEmpty().toList()
            for (i in children.indices.reversed()) {
                stack.addLast(children[i])
            }
        }

        nodeToIndex = preOrder.withIndex().associate { (i, v) -> v to i }

        // Compute exclusive end index for each node's subtree.
        // In reverse pre-order: for leaves, end = index + 1;
        // for internal nodes, end = max subtreeEnd among children.
        subtreeEnd = IntArray(size)
        for (i in size - 1 downTo 0) {
            val node = preOrder[i]
            val children = acyclicGraph.outs[node].orEmpty()
            subtreeEnd[i] =
                children.fold(i + 1) { maxEnd, child ->
                    maxOf(maxEnd, subtreeEnd[nodeToIndex.getValue(child)])
                }
        }
    }

}