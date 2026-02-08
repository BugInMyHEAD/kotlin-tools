package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.graph.AcyclicGraph.Companion.toAcyclicGraph
import com.buginmyhead.tools.kotlin.graph.AcyclicGraph.Companion.topologicalSort
import com.buginmyhead.tools.kotlin.graph.Graph.Companion.bfs
import com.buginmyhead.tools.kotlin.graph.Graph.Companion.toGraph
import com.buginmyhead.tools.kotlin.graph.MutableGraph.Companion.addEdge
import com.buginmyhead.tools.kotlin.graph.MutableGraph.Companion.toMutableGraph
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

internal class GraphTest : FreeSpec({
    "empty graph on creation" {
        val graph = MutableGraph<Any, Any>()

        graph.nodes shouldBe emptySet()
        graph.edges shouldBe emptyMap()
        graph.ins shouldBe emptyMap()
        graph.outs shouldBe emptyMap()
        graph.sinkNodes shouldBe emptySet()
        graph.sourceNodes shouldBe emptySet()
    }

    "addNode to empty graph" {
        val graph = MutableGraph<String, Int>()
        graph.addNode("A")

        graph.nodes shouldBe setOf("A")
        graph.edges shouldBe emptyMap()
        graph.ins shouldBe mapOf("A" to emptySet())
        graph.outs shouldBe mapOf("A" to emptySet())
        graph.sinkNodes shouldBe setOf("A")
        graph.sourceNodes shouldBe setOf("A")
    }

    "addEdge to empty graph" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)

        graph.nodes shouldBe setOf("A", "B")
        graph.edges shouldBe mapOf(("A" to "B") to 5)
        graph.ins shouldBe mapOf("A" to emptySet(), "B" to setOf("A"))
        graph.outs shouldBe mapOf("A" to setOf("B"), "B" to emptySet())
        graph.sinkNodes shouldBe setOf("B")
        graph.sourceNodes shouldBe setOf("A")
    }

    "removeNode from empty graph" {
        val graph = MutableGraph<String, Int>()
        graph.removeNode("A")

        graph.nodes shouldBe emptySet()
        graph.edges shouldBe emptyMap()
        graph.ins shouldBe emptyMap()
        graph.outs shouldBe emptyMap()
        graph.sinkNodes shouldBe emptySet()
        graph.sourceNodes shouldBe emptySet()
    }

    "addNode does nothing if node exists" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addNode("A")

        graph.nodes shouldBe setOf("A", "B")
        graph.edges shouldBe mapOf(("A" to "B") to 5)
        graph.ins shouldBe mapOf("A" to emptySet(), "B" to setOf("A"))
        graph.outs shouldBe mapOf("A" to setOf("B"), "B" to emptySet())
        graph.sinkNodes shouldBe setOf("B")
        graph.sourceNodes shouldBe setOf("A")
    }

    "addEdge updates weight of existing edge" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("A" to "B", 7)

        graph.nodes shouldBe setOf("A", "B")
        graph.edges shouldBe mapOf(("A" to "B") to 7)
        graph.ins shouldBe mapOf("A" to emptySet(), "B" to setOf("A"))
        graph.outs shouldBe mapOf("A" to setOf("B"), "B" to emptySet())
        graph.sinkNodes shouldBe setOf("B")
        graph.sourceNodes shouldBe setOf("A")
    }

    "node can have multiple incoming and outgoing edges" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("B" to "C", 7)
        graph.addEdge("C" to "B", 11)
        graph.addEdge("B" to "A", 13)

        graph.nodes shouldBe setOf("A", "B", "C")
        graph.edges shouldBe mapOf(
            ("A" to "B") to 5,
            ("B" to "C") to 7,
            ("C" to "B") to 11,
            ("B" to "A") to 13,
        )
        graph.ins shouldBe mapOf(
            "A" to setOf("B"),
            "B" to setOf("A", "C"),
            "C" to setOf("B"),
        )
        graph.outs shouldBe mapOf(
            "A" to setOf("B"),
            "B" to setOf("A", "C"),
            "C" to setOf("B"),
        )
        graph.sinkNodes shouldBe setOf()
        graph.sourceNodes shouldBe setOf()
    }

    "removeNode removes associated edges" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("B" to "C", 7)
        graph.removeNode("B")

        graph.nodes shouldBe setOf("A", "C")
        graph.edges shouldBe emptyMap()
        graph.ins shouldBe mapOf("A" to emptySet(), "C" to emptySet())
        graph.outs shouldBe mapOf("A" to emptySet(), "C" to emptySet())
        graph.sinkNodes shouldBe setOf("A", "C")
        graph.sourceNodes shouldBe setOf("A", "C")
    }

    "toString produces expected output" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("B" to "C", 7)

        val expected = "MutableGraph(ins={A=[], B=[A], C=[B]}, outs={A=[B], B=[C], C=[]})"
        graph.toString() shouldBe expected
    }

    "equals and hashCode are same for the identical graph" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("B" to "C", 7)

        graph shouldBe graph
        graph.hashCode() shouldBe graph.hashCode()
    }

    "equals and hashCode are same for the same graphs" {
        val graphA = MutableGraph<String, Int>()
        graphA.addEdge("A" to "B", 5)
        graphA.addEdge("B" to "C", 7)

        val graphB = MutableGraph<String, Int>()
        graphB.addEdge("A" to "B", 5)
        graphB.addEdge("B" to "C", 7)

        graphA shouldBe graphB
        graphA.hashCode() shouldBe graphB.hashCode()
    }

    "equals and hashCode are different for the different graphs" {
        val graphA = MutableGraph<String, Int>()
        graphA.addEdge("A" to "B", 5)

        val graphB = MutableGraph<String, Int>()
        graphB.addEdge("C" to "D", 7)

        graphA shouldNotBe graphB
        graphA.hashCode() shouldNotBe graphB.hashCode()
    }

    "toGraph copies the graph if it is mutable" {
        val original = MutableGraph<String, Int>()
        original.addEdge("A" to "B", 5)
        original.addEdge("B" to "C", 7)
        val copied = original.toGraph()

        copied.nodes shouldBe original.nodes
        copied.edges shouldBe original.edges
        copied.ins shouldBe original.ins
        copied.outs shouldBe original.outs
        copied.sinkNodes shouldBe original.sinkNodes
        copied.sourceNodes shouldBe original.sourceNodes
    }

    "toGraph does not copy the graph if it is immutable" {
        val original = MutableGraph<String, Int>()
        original.addEdge("A" to "B", 5)
        original.addEdge("B" to "C", 7)
        val immutable = original.toGraph()

        immutable.toGraph() shouldBeSameInstanceAs immutable
    }

    "toMutableGraph copies the graph" {
        val original = MutableGraph<String, Int>()
        original.addEdge("A" to "B", 5)
        original.addEdge("B" to "C", 7)
        val copied = original.toMutableGraph()

        copied.nodes shouldBe original.nodes
        copied.edges shouldBe original.edges
        copied.ins shouldBe original.ins
        copied.outs shouldBe original.outs
        copied.sinkNodes shouldBe original.sinkNodes
        copied.sourceNodes shouldBe original.sourceNodes
    }

    "toMutableGraph transforms the graph" {
        val original = MutableGraph<String, Int>()
        original.addEdge("A" to "B", 5)
        original.addEdge("B" to "C", 7)
        val transformed = original.toMutableGraph(
            nodeTransform = { node -> node.lowercase() }
        ) { from, to, weight, tFrom, tTo -> "$from$to$weight$tFrom$tTo" }

        transformed.nodes shouldBe setOf("a", "b", "c")
        transformed.edges shouldBe mapOf(
            ("a" to "b") to "AB5ab",
            ("b" to "c") to "BC7bc",
        )
        transformed.ins shouldBe mapOf(
            "a" to emptySet(),
            "b" to setOf("a"),
            "c" to setOf("b"),
        )
        transformed.outs shouldBe mapOf(
            "a" to setOf("b"),
            "b" to setOf("c"),
            "c" to emptySet(),
        )
        transformed.sinkNodes shouldBe setOf(
            "c",
        )
        transformed.sourceNodes shouldBe setOf(
            "a",
        )
    }

    "from creates a graph from sourceNodes, weightFunction, nextFunction" {
        val graph = MutableGraph.from(
            sourceNodes = listOf("A", "B"),
            weightFunction = { from, to -> from + to },
        ) { node ->
            when (node) {
                "A" -> listOf("C", "D")
                "B" -> listOf("C", "D")
                "C" -> listOf("E")
                "D" -> listOf("E")
                else -> emptyList()
            }
        }

        graph.nodes shouldBe setOf("A", "B", "C", "D", "E")
        graph.edges shouldBe mapOf(
            ("A" to "C") to "AC",
            ("A" to "D") to "AD",
            ("B" to "C") to "BC",
            ("B" to "D") to "BD",
            ("C" to "E") to "CE",
            ("D" to "E") to "DE",
        )
        graph.ins shouldBe mapOf(
            "A" to emptySet(),
            "B" to emptySet(),
            "C" to setOf("A", "B"),
            "D" to setOf("A", "B"),
            "E" to setOf("C", "D"),
        )
        graph.outs shouldBe mapOf(
            "A" to setOf("C", "D"),
            "B" to setOf("C", "D"),
            "C" to setOf("E"),
            "D" to setOf("E"),
            "E" to emptySet(),
        )
        graph.sourceNodes shouldBe setOf("A", "B")
        graph.sinkNodes shouldBe setOf("E")
    }

    "from creates a graph from sourceNodes, nextFunction" {
        val graph = MutableGraph.from(
            sourceNodes = listOf("A", "B"),
        ) { node ->
            when (node) {
                "A" -> listOf("C", "D")
                "B" -> listOf("C", "D")
                "C" -> listOf("E")
                "D" -> listOf("E")
                else -> emptyList()
            }
        }

        graph.nodes shouldBe setOf("A", "B", "C", "D", "E")
        graph.edges shouldBe mapOf(
            ("A" to "C") to Unit,
            ("A" to "D") to Unit,
            ("B" to "C") to Unit,
            ("B" to "D") to Unit,
            ("C" to "E") to Unit,
            ("D" to "E") to Unit,
        )
        graph.ins shouldBe mapOf(
            "A" to emptySet(),
            "B" to emptySet(),
            "C" to setOf("A", "B"),
            "D" to setOf("A", "B"),
            "E" to setOf("C", "D"),
        )
        graph.outs shouldBe mapOf(
            "A" to setOf("C", "D"),
            "B" to setOf("C", "D"),
            "C" to setOf("E"),
            "D" to setOf("E"),
            "E" to emptySet(),
        )
        graph.sourceNodes shouldBe setOf("A", "B")
        graph.sinkNodes shouldBe setOf("E")
    }

    "toAcyclicGraph copies the graph that is acyclic graph" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("B" to "C", 7)

        val acyclicGraph = graph.toAcyclicGraph()
        acyclicGraph.nodes shouldBe setOf("A", "B", "C")
        acyclicGraph.edges shouldBe mapOf(
            ("A" to "B") to 5,
            ("B" to "C") to 7,
        )
        acyclicGraph.ins shouldBe mapOf(
            "A" to emptySet(),
            "B" to setOf("A"),
            "C" to setOf("B"),
        )
        acyclicGraph.outs shouldBe mapOf(
            "A" to setOf("B"),
            "B" to setOf("C"),
            "C" to emptySet(),
        )
        acyclicGraph.sinkNodes shouldBe setOf("C")
        acyclicGraph.sourceNodes shouldBe setOf("A")
    }

    "toAcyclicGraph fails with CyclicGraphException on cyclic graph 1" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "A", 5)

        shouldThrow<CyclicGraphException> { graph.toAcyclicGraph() }
    }

    "toAcyclicGraph fails with CyclicGraphException on cyclic graph 2" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("B" to "A", 7)

        shouldThrow<CyclicGraphException> { graph.toAcyclicGraph() }
    }

    "toAcyclicGraph fails with CyclicGraphException on cyclic graph 3" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("B" to "C", 7)
        graph.addEdge("C" to "A", 11)

        shouldThrow<CyclicGraphException> { graph.toAcyclicGraph() }
    }

    "CyclicGraphException is an IllegalStateException" {
        CyclicGraphException().shouldBeInstanceOf<IllegalStateException>()
    }

    "bfs forward on empty graph produces no nodes" {
        val graph = MutableGraph<String, Unit>()

        val result = graph.bfs(Graph.Direction.Forward, setOf("A")).toList()
        result shouldBe emptyList()
    }

    "bfs backward on empty graph produces no nodes" {
        val graph = MutableGraph<String, Unit>()

        val result = graph.bfs(Graph.Direction.Backward, setOf("A")).toList()
        result shouldBe emptyList()
    }

    "bfs forward with a node that is not in the graph produces no nodes" {
        val graph = MutableGraph<String, Unit>()
        graph.addEdge("A" to "B")

        val result = graph.bfs(Graph.Direction.Forward, setOf("C")).toList()
        result shouldBe emptyList()
    }

    "bfs backward with a node that is not in the graph produces no nodes" {
        val graph = MutableGraph<String, Unit>()
        graph.addEdge("A" to "B")

        val result = graph.bfs(Graph.Direction.Backward, setOf("C")).toList()
        result shouldBe emptyList()
    }

    "bfs forward with specified startNodes visits part of nodes" {
        val graph = MutableGraph<String, Unit>()
        graph.addEdge("A" to "B")
        graph.addEdge("A" to "C")
        graph.addEdge("B" to "D")
        graph.addEdge("C" to "D")

        val result = graph.bfs(Graph.Direction.Forward, setOf("B", "C")).toList()
        result shouldBe listOf("B", "C", "D")
    }

    "bfs backward with specified startNodes visits part of nodes" {
        val graph = MutableGraph<String, Unit>()
        graph.addEdge("A" to "B")
        graph.addEdge("A" to "C")
        graph.addEdge("B" to "D")
        graph.addEdge("C" to "D")

        val result = graph.bfs(Graph.Direction.Backward, setOf("B", "C")).toList()
        result shouldBe listOf("B", "C", "A")
    }

    "bfs forward on cyclic graph" {
        val graph = MutableGraph<String, Unit>()
        graph.addEdge("A" to "B")
        graph.addEdge("B" to "C")
        graph.addEdge("C" to "A")

        val result = graph.bfs(Graph.Direction.Forward, setOf("A")).toList()
        result shouldBe listOf("A", "B", "C")
    }

    "bfs backward on cyclic graph" {
        val graph = MutableGraph<String, Unit>()
        graph.addEdge("A" to "B")
        graph.addEdge("B" to "C")
        graph.addEdge("C" to "A")

        val result = graph.bfs(Graph.Direction.Backward, setOf("C")).toList()
        result shouldBe listOf("C", "B", "A")
    }

    "topologicalSort forward visits all nodes" {
        val graph = MutableGraph<String, Unit>()
        graph.addEdge("A" to "D")
        graph.addEdge("B" to "C")
        graph.addEdge("C" to "D")
        val acyclicGraph = graph.toAcyclicGraph()
        
        val result = acyclicGraph.topologicalSort(Graph.Direction.Forward).toList()
        result shouldBe listOf("A", "B", "C", "D")
    }

    "topologicalSort backward visits all nodes" {
        val graph = MutableGraph<String, Unit>()
        graph.addEdge("A" to "D")
        graph.addEdge("B" to "C")
        graph.addEdge("C" to "D")
        val acyclicGraph = graph.toAcyclicGraph()

        val result = acyclicGraph.topologicalSort(Graph.Direction.Backward).toList()
        result shouldBe listOf("D", "A", "C", "B")
    }
})