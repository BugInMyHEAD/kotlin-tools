package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.graph.Graph.Companion.isEmpty
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

// CyclicGraphException should not be thrown
//  because it should be thrown on AcyclicGraph creation.
suspend fun <N> AcyclicGraph<N, *>.parallelTopologicalSort(
    direction: Graph.Direction,
    onVisit: suspend (N) -> Unit
): Unit = coroutineScope {
    if (isEmpty()) return@coroutineScope

    // Since the graph is acyclic, there should be at least one source and one sink.
    // If not, this [AcyclicGraph] is invalid.
    if (sourceNodes.isEmpty() || sinkNodes.isEmpty()) throw CyclicGraphException()

    val commandChannel = Channel<Command<N>>(capacity = Channel.UNLIMITED)
    direction.getStartingNodes(this@parallelTopologicalSort)
        .forEach { node -> commandChannel.send(Visit(node)) }

    val nodeToInDegree = nodes.associateWith { node ->
        direction.getPreviousNodes(node, this@parallelTopologicalSort).size
    }.toMutableMap()

    var remainingNodeCount = nodes.size

    while (remainingNodeCount > 0) {
        when (val command = commandChannel.receive()) {
            is Visit<N> -> launch {
                onVisit(command.node)
                commandChannel.send(EnqueueChildren(command.node))
            }
            is EnqueueChildren<N> -> {
                for (next in direction.getNextNodes(command.node, this@parallelTopologicalSort)) {
                    val inDegree = nodeToInDegree.getValue(next) - 1
                    if (inDegree < 0) throw CyclicGraphException()
                    nodeToInDegree[next] = inDegree
                    if (inDegree == 0) commandChannel.send(Visit(next))
                }
                --remainingNodeCount
            }
        }
    }
}

fun <N> AcyclicGraph<N, *>.parallelTopologicalFlow(
    direction: Graph.Direction,
    onVisit: suspend (N) -> Unit
): Flow<N> = channelFlow {
    parallelTopologicalSort(direction) { node ->
        onVisit(node)
        send(node)
    }
}

private sealed interface Command<N>
private data class Visit<N>(val node: N) : Command<N>
private data class EnqueueChildren<N>(val node: N) : Command<N>