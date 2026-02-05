package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.graph.AcyclicGraph.Companion.toAcyclicGraph
import com.buginmyhead.tools.kotlin.graph.MutableGraph.Companion.addEdge
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlin.time.Duration.Companion.seconds

internal class GraphCoroutineExtTest : FreeSpec({
    coroutineDebugProbes = true
    coroutineTestScope = true

    "parallelTopologicalFlow forward when b takes longer than c" {
        val a = Task(1)
        val b = Task(2)
        val c = Task(1)
        val d = Task(1)
        val graph = MutableGraph<Task, Unit>()
        graph.addEdge(a to b)
        graph.addEdge(a to c)
        graph.addEdge(b to d)
        graph.addEdge(c to d)
        val acyclicGraph = graph.toAcyclicGraph()

        val result = acyclicGraph.parallelTopologicalFlow(Graph.Direction.Forward) { task ->
            delay(task.time.seconds)
        }.toList()

        result shouldBe listOf(a, c, b, d)
    }

    "parallelTopologicalFlow backward when b takes longer than c" {
        val a = Task(1)
        val b = Task(2)
        val c = Task(1)
        val d = Task(1)
        val graph = MutableGraph<Task, Unit>()
        graph.addEdge(a to b)
        graph.addEdge(a to c)
        graph.addEdge(b to d)
        graph.addEdge(c to d)
        val acyclicGraph = graph.toAcyclicGraph()

        val result = acyclicGraph.parallelTopologicalFlow(Graph.Direction.Backward) { task ->
            delay(task.time.seconds)
        }.toList()

        result shouldBe listOf(d, c, b, a)
    }

    "parallelTopologicalFlow forward when c takes longer than b" {
        val a = Task(1)
        val b = Task(1)
        val c = Task(2)
        val d = Task(1)
        val graph = MutableGraph<Task, Unit>()
        graph.addEdge(a to b)
        graph.addEdge(a to c)
        graph.addEdge(b to d)
        graph.addEdge(c to d)
        val acyclicGraph = graph.toAcyclicGraph()

        val result = acyclicGraph.parallelTopologicalFlow(Graph.Direction.Forward) { task ->
            delay(task.time.seconds)
        }.toList()

        result shouldBe listOf(a, b, c, d)
    }

    "parallelTopologicalFlow backward when c takes longer than b" {
        val a = Task(1)
        val b = Task(1)
        val c = Task(2)
        val d = Task(1)
        val graph = MutableGraph<Task, Unit>()
        graph.addEdge(a to b)
        graph.addEdge(a to c)
        graph.addEdge(b to d)
        graph.addEdge(c to d)
        val acyclicGraph = graph.toAcyclicGraph()

        val result = acyclicGraph.parallelTopologicalFlow(Graph.Direction.Backward) { task ->
            delay(task.time.seconds)
        }.toList()

        result shouldBe listOf(d, b, c, a)
    }
})

private class Task(val time: Long)