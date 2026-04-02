package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.graph.Tree.Companion.ancestorsFrom
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.leaves
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.root
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.subtreeOf
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.toTree
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

internal class TreeTest : FreeSpec({

    "toTree returns the same instance if the graph is Tree" {
        val graph = MutableGraph<String, Unit>()
        graph.addNode("A")
        val tree = graph.toTree()

        tree.toTree() shouldBeSameInstanceAs tree
    }

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

    "Tree equals the graph that has the same structure" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("A" to "C", 7)
        graph.addEdge("C" to "D", 11)
        val tree = graph.toTree()

        graph shouldBe tree
        tree shouldBe graph
        tree.hashCode() shouldBe graph.hashCode()
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

    "subtreeOf the root returns the same tree" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        val tree = graph.toTree()

        val subtreeOfA = tree.subtreeOf("A")
        subtreeOfA shouldBeSameInstanceAs tree
    }

    "subtreeOf returns the correct subtree" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("A" to "C", 7)
        graph.addEdge("C" to "D", 11)
        val tree = graph.toTree()

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

    "subtreeOf on a subtree returns the correct nested subtree" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("A" to "C", 7)
        graph.addEdge("C" to "D", 11)
        graph.addEdge("C" to "E", 13)
        graph.addEdge("E" to "F", 17)
        val tree = graph.toTree()

        val subtreeOfC = tree.subtreeOf("C")
        val nestedSubtreeOfE = subtreeOfC.subtreeOf("E")
        nestedSubtreeOfE.nodes shouldBe setOf("E", "F")
        nestedSubtreeOfE.edges shouldBe mapOf(
            ("E" to "F") to 17,
        )
        nestedSubtreeOfE.root shouldBe "E"
        nestedSubtreeOfE.leaves shouldBe setOf("F")

        val deepSubtreeOfF = nestedSubtreeOfE.subtreeOf("F")
        deepSubtreeOfF.nodes shouldBe setOf("F")
        deepSubtreeOfF.edges shouldBe emptyMap()
        deepSubtreeOfF.root shouldBe "F"
        deepSubtreeOfF.leaves shouldBe setOf("F")
    }

    "subtreeOf on a subtree throws for node outside the subtree" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 5)
        graph.addEdge("A" to "C", 7)
        graph.addEdge("C" to "D", 11)
        val tree = graph.toTree()

        val subtreeOfC = tree.subtreeOf("C")
        shouldThrow<IllegalArgumentException> { subtreeOfC.subtreeOf("B") }
        shouldThrow<IllegalArgumentException> { subtreeOfC.subtreeOf("A") }
    }
})