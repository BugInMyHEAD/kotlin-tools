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

    val channel = Channel<Command<N>>(capacity = Channel.UNLIMITED)
    direction.getStartingNodes(this@parallelTopologicalSort)
        .forEach { node -> channel.send(Start(node)) }

    val nodeToInDegree = nodes.associateWith { node ->
        direction.getPreviousNodes(node, this@parallelTopologicalSort).size
    }.toMutableMap()

    var remainingNodeCount = nodes.size

    while (remainingNodeCount > 0) {
        when (val command = channel.receive()) {
            is Start<N> -> launch {
                onVisit(command.node)
                channel.send(Finish(command.node))
            }
            is Finish<N> -> {
                for (next in direction.getNextNodes(command.node, this@parallelTopologicalSort)) {
                    val inDegree = nodeToInDegree.getValue(next) - 1
                    if (inDegree < 0) throw CyclicGraphException()
                    nodeToInDegree[next] = inDegree
                    if (inDegree == 0) channel.send(Start(next))
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
private data class Start<N>(val node: N) : Command<N>
private data class Finish<N>(val node: N) : Command<N>