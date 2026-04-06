package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.DfsPostContext
import com.buginmyhead.tools.kotlin.IgnoreTestCoverage
import com.buginmyhead.tools.kotlin.dfsPost
import com.buginmyhead.tools.kotlin.graph.AcyclicGraph.Companion.toAcyclicGraph
import com.buginmyhead.tools.kotlin.graph.Graph.Companion.bfs
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.leaves
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.root
import java.util.NavigableMap

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

        @get:IgnoreTestCoverage
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
            if (node == root) return this

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
    rangeEndInclusive: Int,
    rootNode: N,
    lastNode: N,
) : Tree<N, W> {

    constructor(original: AcyclicGraph<N, W>) : this(
        TreeIndex(original),
    )

    private constructor(
        index: TreeIndex<N, W>,
    ) : this(
        index,
        0,
        index.original.nodes.size - 1,
        index.original.sourceNodes.single(),
        index.nodes.last(),
    )

    /** Creates a subtree rooted at [node] in O(1) by narrowing the index range. */
    fun subtreeAt(node: N): IndexedTree<N, W> {
        val idx = index.nodeToIndex.getValue(node)
        val endIdxInclusive = idx + index.nodeToSubtreeSize.getValue(node) - 1
        return IndexedTree(
            index,
            idx,
            endIdxInclusive,
            node,
            index.nodes[endIdxInclusive],
        )
    }

    /** Sub-view that serves as both [nodes] (via keys) and [outs] (as map). */
    private val _outs: NavigableMap<N, Set<N>> =
        index.outs.subMap(rootNode, true, lastNode, true)

    override val outs: Map<N, Set<N>> get() = _outs

    /** View backed by the pre-order index range. No copy. */
    override val nodes: Set<N> = _outs.keys

    /** View backed by the cumulative edge count range. No copy. */
    override val edges: Map<Pair<N, N>, W> =
        index.edges.subView(rangeStart .. rangeEndInclusive - 1)

    /** Maps each node to its in-neighbors, with the subtree root overridden to emptySet(). */
    override val ins: Map<N, Set<N>> = _outs.keys.associateWith { node ->
        if (node == rootNode) emptySet() else index.ins.getValue(node)
    }

    /** View backed by binary-searched sink range. No copy. */
    override val sinkNodes: Set<N> = run {
        val fromSinkIdx =
            index.sinkGlobalIndices
                .binarySearch(rangeStart)
                .let { if (it < 0) it.inv() else it }
        val toSinkIdx =
            index.sinkGlobalIndices
                .binarySearch(rangeEndInclusive)
                .let { if (it < 0) it.inv() - 1 else it }
        index.sinkMap.subView(fromSinkIdx .. toSinkIdx).keys
    }

    /** View as a singleton sub-map of the pre-order map. No copy. */
    override val sourceNodes: Set<N> =
        index.outs.subMap(rootNode, true, rootNode, true).keys

    override fun equals(other: Any?): Boolean = index.original == other

    override fun hashCode(): Int = index.original.hashCode()

}

/**
 * Pre-computed index structure for efficient subtree operations.
 *
 * All [Map] and [Set] properties in [IndexedTree] are O(1) views over this index,
 *  except [IndexedTree.sinkNodes] which uses O(log s) binary search.
 */
private class TreeIndex<N, W>(
    val original: AcyclicGraph<N, W>,
) {

    val nodes: List<N>
    val nodeToIndex: Map<N, Int>
    val nodeToSubtreeSize: Map<N, Int>

    val outs: NavigableListMap<N, Set<N>>
    val ins: NavigableListMap<N, Set<N>>

    private val preOrderedInEdges: List<Pair<N, N>>
    private val edgeToIndex: Map<Pair<N, N>, Int>
    val edges: NavigableListMap<Pair<N, N>, W>

    // For sinkNodes sub-views: sinks in pre-order with their global indices
    val sinkMap: NavigableListMap<N, Unit>
    val sinkGlobalIndices: IntArray

    init {
        val root = original.sourceNodes.single()
        val size = original.nodes.size

        val stack = ArrayDeque<N>(size)
        stack.addLast(root)

        val preOrderedDfsPostContext =
            dfsPost(
                cycleSafe = false,
                roots = sequenceOf(root),
                initial = { 1 },
                aggregate = { parent, child -> parent + child },
                flatten = { node -> yieldAll(original.outs.getValue(node)) },
            ).toList().reversed()

        nodes = preOrderedDfsPostContext.map(DfsPostContext<N, Int>::node)
        nodeToIndex = nodes.withIndex().associate { it.value to it.index }
        nodeToSubtreeSize = preOrderedDfsPostContext.associate { it.node to it.result }

        outs = NavigableListMap(nodes.map { it to original.outs.getValue(it) })
        ins = NavigableListMap(nodes.map { it to original.ins.getValue(it) })

        preOrderedInEdges =
            preOrderedDfsPostContext
                .map { it.pathToRoot.take(2).toList() }
                .filter { it.size == 2 }
                .map { (child, parent) -> parent to child }
                .toList()
        edges =
            NavigableListMap(preOrderedInEdges.map { it to original.edges.getValue(it) })
        edgeToIndex = preOrderedInEdges.withIndex().associate { it.value to it.index }

        // Sink nodes in pre-order with their global indices for binary search
        val sinkList = ArrayList<N>()
        val sinkIndices = ArrayList<Int>()
        var sinkTraverseNode: N? = outs.firstKey()
        for (i in 0 ..< size) {
            val node = sinkTraverseNode!!
            if (node in original.sinkNodes) {
                sinkList.add(node)
                sinkIndices.add(i)
            }
            sinkTraverseNode = outs.higherKey(node)
        }
        sinkMap = NavigableListMap(sinkList.map { it to Unit })
        sinkGlobalIndices = sinkIndices.toIntArray()
    }

}