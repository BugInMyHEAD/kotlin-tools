package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.DfsPostContext
import com.buginmyhead.tools.kotlin.IgnoreTestCoverage
import com.buginmyhead.tools.kotlin.UnsafeMergeMap
import com.buginmyhead.tools.kotlin.dfsPost
import com.buginmyhead.tools.kotlin.graph.AcyclicGraph.Companion.toAcyclicGraph
import com.buginmyhead.tools.kotlin.graph.Graph.Companion.bfs
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.leaves
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.root
import java.util.NavigableMap
import java.util.NavigableSet
import java.util.Objects

/**
 * [Graph.Direction.Forward] means from [root] to [leaves].
 * [Graph.Direction.Backward] means from [leaves] to [root].
 */
interface Tree<N, W> : AcyclicGraph<N, W> {

    companion object {

        @Throws(NotATreeException::class)
        fun <N, W> Graph<N, W>.toTree(): Tree<N, W> =
            this as? Tree<N, W>
                ?: try {
                    val original = toAcyclicGraph()
                    if (original.sourceNodes.size != 1)
                        throw NotATreeException("Multiple root node candidates found.")
                    if (original.ins.values.any { it.size > 1 })
                        throw NotATreeException("A node with multiple parents found.")
                    IndexedTree(original)
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
        fun <N, W> Tree<N, W>.subtreeAt(node: N): Tree<N, W> {
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
    root: N,
) : Tree<N, W> {

    constructor(original: AcyclicGraph<N, W>) : this(
        TreeIndex(original),
        original.sourceNodes.single(),
    )

    private val last: N = run {
        val rootIdx = index.nodeToIndex.getValue(root)
        index.nodes[rootIdx + index.nodeToSubtreeSize.getValue(root) - 1]
    }

    override val edges: Map<Pair<N, N>, W> = run {
        val rootIdx = index.nodeToIndex.getValue(root)
        val firstChildEdge = index.preOrderedInEdges.getOrElse(rootIdx) {
            // When [root] is the only node, there are no child edges.
            return@run emptyMap()
        }
        // -1 for exclusion of the in-edge of [root],
        //  and -1 for [lastInEdge] being inclusive in `subMap` operation.
        val lastIdx = rootIdx + index.nodeToSubtreeSize.getValue(root) - 2
        val lastInEdge = index.preOrderedInEdges[lastIdx]
        index.edges.subMap(firstChildEdge, true, lastInEdge, true)
    }

    override val outs: Map<N, Set<N>> = index.outs.subMap(root, true, last, true)

    override val ins: Map<N, Set<N>> =
        UnsafeMergeMap(
            mapOf(root to emptySet()),
            index.ins.subMap(root, false, last, true),
        )

    override val nodes: Set<N> = outs.keys

    override val sourceNodes: Set<N> = setOf(root)

    override val sinkNodes: Set<N> = run {
        val firstSink = index.nodeToFirstSink.getValue(root)
        index.sinkNodes.subSet(firstSink, true, last, true)
    }

    override fun equals(other: Any?): Boolean =
        this === other
                || (
                other is Graph<*, *>
                        && this.edges == other.edges
                        && this.sourceNodes == other.sourceNodes
                        && this.sinkNodes == other.sinkNodes
                )

    override fun hashCode(): Int = Objects.hash(edges, sourceNodes, sinkNodes)

    /** Creates a subtree rooted at [node] in O(1) by narrowing the index range. */
    fun subtreeAt(node: N) = IndexedTree(
        index,
        node,
    )

}

/**
 * Pre-computed index structure for efficient subtree operations.
 *
 * All [Map] and [Set] properties in [IndexedTree] are O(1) views over this index.
 */
private class TreeIndex<N, W>(
    val original: AcyclicGraph<N, W>,
) {

    val preOrderedInEdges: List<Pair<N, N>>
    val edges: NavigableMap<Pair<N, N>, W>

    val nodes: List<N>
    val nodeToIndex: Map<N, Int>
    val nodeToSubtreeSize: Map<N, Int>

    val outs: NavigableMap<N, Set<N>>
    val ins: NavigableMap<N, Set<N>>

    val sinkNodes: NavigableSet<N>
    val nodeToFirstSink: Map<N, N>

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
            ).toList().asReversed()

        preOrderedInEdges =
            preOrderedDfsPostContext
                .map { it.pathToRoot.take(2).toList() }
                .filter { it.size == 2 }
                .map { (child, parent) -> parent to child }
                .toList()
        edges =
            NavigableListMap(preOrderedInEdges.map { it to original.edges.getValue(it) })

        nodes = preOrderedDfsPostContext.map(DfsPostContext<N, Int>::node)
        nodeToIndex = nodes.withIndex().associate { it.value to it.index }
        nodeToSubtreeSize = preOrderedDfsPostContext.associate { it.node to it.result }

        outs = NavigableListMap(nodes.map { it to original.outs.getValue(it) })
        ins = NavigableListMap(nodes.map { it to original.ins.getValue(it) })

        sinkNodes = navigableListSetFrom(nodes.filter { it in original.sinkNodes })

        val firstSink =
            nodes.asReversed()
                .runningReduce { lastSink, node ->
                    if (node in original.sinkNodes) node else lastSink
                }
                .asReversed()
        nodeToFirstSink = (nodes zip firstSink).toMap()
    }

}