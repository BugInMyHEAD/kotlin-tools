package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.graph.Tree.Companion.ancestorsFrom
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.leaves
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.root
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.subtreeOf
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.toTree
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

internal class TreeTest : FreeSpec({
    "toTree copies the graph that is a tree" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("A" to "C", 7)
        graph.addEdge("C" to "D", 11)
        val tree = graph.toTree()

        tree.nodes shouldBe setOf("A", "B", "C", "D")
        tree.edges shouldBe mapOf(
            ("A" to "B") to 5,
            ("A" to "C") to 7,
            ("A" to "C") to 7,
            ("C" to "D") to 11,
        )
        tree.sourceNodes shouldBe setOf("A")
        tree.sinkNodes shouldBe setOf("B", "D")
    }

    "toTree fails with NotATreeException if graph has a cycle" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("B" to "C", 7)
        graph.addEdge("C" to "A", 11)

        shouldThrow<NotATreeException> { graph.toTree() }
    }

    "toTree fails with NotATreeException if graph has multiple roots" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "C", 5)
        graph.addEdge("B" to "D", 7)

        shouldThrow<NotATreeException> { graph.toTree() }
    }

    "toTree fails with NotATreeException if any node has multiple parents" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("A" to "C", 7)
        graph.addEdge("B" to "D", 11)
        graph.addEdge("C" to "D", 13)

        shouldThrow<NotATreeException> { graph.toTree() }
    }

    "root returns the single root node" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("A" to "C", 7)
        val tree = graph.toTree()

        tree.root shouldBe "A"
    }

    "leaves returns the leaf nodes" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("A" to "C", 7)
        graph.addEdge("C" to "D", 11)
        val tree = graph.toTree()

        tree.leaves shouldBe setOf("B", "D")
    }

    "ancestorsFrom returns the ancestors from the specified node to the root" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("A" to "C", 7)
        graph.addEdge("C" to "D", 11)
        val tree = graph.toTree()

        val ancestorsFromA = tree.ancestorsFrom("A").toList()
        ancestorsFromA shouldBe listOf("A")

        val ancestorsFromB = tree.ancestorsFrom("B").toList()
        ancestorsFromB shouldBe listOf("B", "A")

        val ancestorsFromC = tree.ancestorsFrom("C").toList()
        ancestorsFromC shouldBe listOf("C", "A")

        val ancestorsFromD = tree.ancestorsFrom("D").toList()
        ancestorsFromD shouldBe listOf("D", "C", "A")
    }

    "subtreeOf throws IllegalArgumentException if node does not exist" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        val tree = graph.toTree()

        shouldThrow<IllegalArgumentException> { tree.subtreeOf("C") }
    }

    "subtreeOf returns the correct subtree" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("A" to "C", 7)
        graph.addEdge("C" to "D", 11)
        val tree = graph.toTree()

        val subtreeOfA = tree.subtreeOf("A")
        subtreeOfA.nodes shouldBe graph.nodes
        subtreeOfA.edges shouldBe graph.edges
        subtreeOfA.outs shouldBe graph.outs
        subtreeOfA.ins shouldBe graph.ins
        subtreeOfA.sourceNodes shouldBe graph.sourceNodes
        subtreeOfA.sinkNodes shouldBe graph.sinkNodes

        val subtreeOfB = tree.subtreeOf("B")
        subtreeOfB.nodes shouldBe setOf("B")
        subtreeOfB.edges shouldBe emptyMap()
        subtreeOfB.outs shouldBe mapOf(
            "B" to emptySet(),
        )
        subtreeOfB.ins shouldBe mapOf(
            "B" to emptySet(),
        )
        subtreeOfB.sourceNodes shouldBe setOf("B")
        subtreeOfB.sinkNodes shouldBe setOf("B")

        val subtreeOfC = tree.subtreeOf("C")
        subtreeOfC.nodes shouldBe setOf("C", "D")
        subtreeOfC.edges shouldBe mapOf(
            ("C" to "D") to 11,
        )
        subtreeOfC.outs shouldBe mapOf(
            "C" to setOf("D"),
            "D" to emptySet(),
        )
        subtreeOfC.ins shouldBe mapOf(
            "C" to emptySet(),
            "D" to setOf("C"),
        )
        subtreeOfC.sourceNodes shouldBe setOf("C")
        subtreeOfC.sinkNodes shouldBe setOf("D")

        val subtreeOfD = tree.subtreeOf("D")
        subtreeOfD.nodes shouldBe setOf("D")
        subtreeOfD.edges shouldBe emptyMap()
        subtreeOfD.outs shouldBe mapOf(
            "D" to emptySet(),
        )
        subtreeOfD.ins shouldBe mapOf(
            "D" to emptySet(),
        )
        subtreeOfD.sourceNodes shouldBe setOf("D")
        subtreeOfD.sinkNodes shouldBe setOf("D")
    }
})