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
        val idx = index.preOrderedMap.globalIndexOf(node)
        return IndexedTree(index, idx, index.subtreeEnd[idx])
    }

    /** Sub-view that serves as both [nodes] (via keys) and [outs] (as map). */
    private val _outs: NavigableListMap<N, Set<N>> =
        index.preOrderedMap.subView(rangeStart ..< rangeEnd)

    override val outs: Map<N, Set<N>> get() = _outs

    /** View backed by the pre-order index range. No copy. */
    override val nodes: Set<N> = _outs.keys

    /** View backed by the cumulative edge count range. No copy. */
    override val edges: Map<Pair<N, N>, W> =
        index.edgesMap.subView(index.edgeCumCount[rangeStart] ..< index.edgeCumCount[rangeEnd])

    /** View with the subtree root's ins overridden to emptySet(). No copy. */
    override val ins: Map<N, Set<N>> = run {
        val root = index.preOrderedMap.keyAt(rangeStart)
        _outs.withValues { if (it == root) emptySet() else index.allIns[it].orEmpty() }
    }

    /** View backed by binary-searched sink range. No copy. */
    override val sinkNodes: Set<N> = run {
        val fromSinkIdx =
            index.sinkGlobalIndices
                .binarySearch(rangeStart)
                .let { if (it < 0) it.inv() else it }
        val toSinkIdx =
            index.sinkGlobalIndices
                .binarySearch(rangeEnd)
                .let { if (it < 0) it.inv() else it }
        index.sinkMap.subView(fromSinkIdx ..< toSinkIdx).keys
    }

    /** View as a singleton sub-view of the pre-order map. No copy. */
    override val sourceNodes: Set<N> = index.preOrderedMap.subView(rangeStart..rangeStart).keys

}

/**
 * Pre-computed index structure for efficient subtree operations.
 *
 * Stores a DFS pre-order traversal where all descendants of any node form
 *  a contiguous range &#91;nodeToIndex&#91;node&#93;, subtreeEnd&#91;nodeToIndex&#91;node&#93;).
 *
 * All [Map] and [Set] properties in [IndexedTree] are O(1) views over this index,
 *  except [IndexedTree.sinkNodes] which uses O(log s) binary search.
 */
private class TreeIndex<N, W>(
    acyclicGraph: AcyclicGraph<N, W>,
) {

    /** Keys = nodes in pre-order, values = out-neighbors. Serves as both node set and outs map. */
    val preOrderedMap: NavigableListMap<N, Set<N>>
    val subtreeEnd: IntArray

    // For ins value function (root override happens per-subtree in IndexedTree)
    val allIns: Map<N, Set<N>> = acyclicGraph.ins

    // For edges sub-views: edge keys ordered by from-node pre-order
    val edgesMap: NavigableListMap<Pair<N, N>, W>
    val edgeCumCount: IntArray

    // For sinkNodes sub-views: sinks in pre-order with their global indices
    val sinkMap: NavigableListMap<N, Unit>
    val sinkGlobalIndices: IntArray

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

        // Pre-ordered map: keys = nodes in pre-order, values = outs
        preOrderedMap = NavigableListMap(order.map { it to acyclicGraph.outs[it].orEmpty() })

        // Computes exclusive end index for each node's subtree using reverse pre-order traversal,
        //  a kind of dynamic programming to visit all children before their parent.
        subtreeEnd = IntArray(size)
        for (i in size - 1 downTo 0) {
            val node = preOrderedMap.keyAt(i)
            val children = acyclicGraph.outs[node].orEmpty()
            subtreeEnd[i] =
                children.fold(i + 1) { maxEnd, child ->
                    maxOf(maxEnd, subtreeEnd[preOrderedMap.globalIndexOf(child)])
                }
        }

        // Edges ordered by from-node pre-order with cumulative count for O(1) range lookup
        val edgeKeyList = ArrayList<Pair<N, N>>()
        val cumCount = IntArray(size + 1)
        for (i in 0 ..< size) {
            cumCount[i] = edgeKeyList.size
            val node = preOrderedMap.keyAt(i)
            for (child in acyclicGraph.outs[node].orEmpty()) {
                edgeKeyList.add(node to child)
            }
        }
        cumCount[size] = edgeKeyList.size
        edgesMap = NavigableListMap(edgeKeyList.map { it to acyclicGraph.edges.getValue(it) })
        edgeCumCount = cumCount

        // Sink nodes in pre-order with their global indices for binary search
        val sinkList = ArrayList<N>()
        val sinkIndices = ArrayList<Int>()
        for (i in 0 ..< size) {
            val node = preOrderedMap.keyAt(i)
            if (node in acyclicGraph.sinkNodes) {
                sinkList.add(node)
                sinkIndices.add(i)
            }
        }
        sinkMap = NavigableListMap.ofKeys(sinkList)
        sinkGlobalIndices = sinkIndices.toIntArray()
    }

}