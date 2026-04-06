package com.buginmyhead.tools.kotlin.graph

import com.buginmyhead.tools.kotlin.graph.Tree.Companion.ancestorsFrom
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.leaves
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.root
import com.buginmyhead.tools.kotlin.graph.Tree.Companion.subtreeAt
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
        graph.addEdge("A" to "B", 13)
        graph.addEdge("A" to "C", 17)
        graph.addEdge("C" to "D", 19)
        val tree = graph.toTree()

        tree.nodes shouldBe setOf("A", "B", "C", "D")
        tree.edges shouldBe mapOf(
            ("A" to "B") to 13,
            ("A" to "C") to 17,
            ("A" to "C") to 17,
            ("C" to "D") to 19,
        )
        tree.sourceNodes shouldBe setOf("A")
        tree.sinkNodes shouldBe setOf("B", "D")
    }

    "Tree equals the graph that has the same structure" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 13)
        graph.addEdge("A" to "C", 17)
        graph.addEdge("C" to "D", 19)
        val tree = graph.toTree()

        graph shouldBe tree
        tree shouldBe graph
        tree.hashCode() shouldBe graph.hashCode()
    }

    "toTree fails with NotATreeException if graph has a cycle" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 13)
        graph.addEdge("B" to "C", 17)
        graph.addEdge("C" to "A", 19)

        shouldThrow<NotATreeException> { graph.toTree() }
    }

    "toTree fails with NotATreeException if graph has multiple roots" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "C", 13)
        graph.addEdge("B" to "D", 17)

        shouldThrow<NotATreeException> { graph.toTree() }
    }

    "toTree fails with NotATreeException if any node has multiple parents" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 13)
        graph.addEdge("A" to "C", 17)
        graph.addEdge("B" to "D", 19)
        graph.addEdge("C" to "D", 23)

        shouldThrow<NotATreeException> { graph.toTree() }
    }

    "root returns the single root node" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 13)
        graph.addEdge("A" to "C", 17)
        val tree = graph.toTree()

        tree.root shouldBe "A"
    }

    "leaves returns the leaf nodes" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 13)
        graph.addEdge("A" to "C", 17)
        graph.addEdge("C" to "D", 19)
        val tree = graph.toTree()

        tree.leaves shouldBe setOf("B", "D")
    }

    "ancestorsFrom returns the ancestors from the specified node to the root" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 13)
        graph.addEdge("A" to "C", 17)
        graph.addEdge("C" to "D", 19)
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

    "subtreeAt throws IllegalArgumentException if node does not exist" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 13)
        val tree = graph.toTree()

        shouldThrow<IllegalArgumentException> { tree.subtreeAt("C") }
    }

    "subtreeAt the root returns the same tree" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 13)
        val tree = graph.toTree()

        val subtreeAtA = tree.subtreeAt("A")
        subtreeAtA shouldBeSameInstanceAs tree
    }

    "subtreeAt returns the correct subtree" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 13)
        graph.addEdge("B" to "E", 23)
        graph.addEdge("A" to "C", 17)
        graph.addEdge("C" to "D", 19)
        val tree = graph.toTree()

        val subtreeAtB = tree.subtreeAt("B")
        subtreeAtB.nodes shouldBe setOf("B", "E")
        subtreeAtB.edges shouldBe mapOf(
            ("B" to "E") to 23,
        )
        subtreeAtB.outs shouldBe mapOf(
            "B" to setOf("E"),
            "E" to emptySet(),
        )
        subtreeAtB.ins shouldBe mapOf(
            "B" to emptySet(),
            "E" to setOf("B"),
        )
        subtreeAtB.sourceNodes shouldBe setOf("B")
        subtreeAtB.sinkNodes shouldBe setOf("E")
        val graphOfB = MutableGraph<String, Int>()
        graphOfB.addEdge("B" to "E", 23)
        graphOfB shouldBe subtreeAtB
        subtreeAtB shouldBe graphOfB

        val subtreeAtE = tree.subtreeAt("E")
        subtreeAtE.nodes shouldBe setOf("E")
        subtreeAtE.edges shouldBe emptyMap()
        subtreeAtE.outs shouldBe mapOf(
            "E" to emptySet(),
        )
        subtreeAtE.ins shouldBe mapOf(
            "E" to emptySet(),
        )
        subtreeAtE.sourceNodes shouldBe setOf("E")
        subtreeAtE.sinkNodes shouldBe setOf("E")
        val graphOfE = MutableGraph<String, Int>()
        graphOfE.addNode("E")
        graphOfE shouldBe subtreeAtE
        subtreeAtE shouldBe graphOfE

        val subtreeAtC = tree.subtreeAt("C")
        subtreeAtC.nodes shouldBe setOf("C", "D")
        subtreeAtC.edges shouldBe mapOf(
            ("C" to "D") to 19,
        )
        subtreeAtC.outs shouldBe mapOf(
            "C" to setOf("D"),
            "D" to emptySet(),
        )
        subtreeAtC.ins shouldBe mapOf(
            "C" to emptySet(),
            "D" to setOf("C"),
        )
        subtreeAtC.sourceNodes shouldBe setOf("C")
        subtreeAtC.sinkNodes shouldBe setOf("D")
        val graphOfC = MutableGraph<String, Int>()
        graphOfC.addEdge("C" to "D", 19)
        graphOfC shouldBe subtreeAtC
        subtreeAtC shouldBe graphOfC

        val subtreeAtD = tree.subtreeAt("D")
        subtreeAtD.nodes shouldBe setOf("D")
        subtreeAtD.edges shouldBe emptyMap()
        subtreeAtD.outs shouldBe mapOf(
            "D" to emptySet(),
        )
        subtreeAtD.ins shouldBe mapOf(
            "D" to emptySet(),
        )
        subtreeAtD.sourceNodes shouldBe setOf("D")
        subtreeAtD.sinkNodes shouldBe setOf("D")
        val graphOfD = MutableGraph<String, Int>()
        graphOfD.addNode("D")
        graphOfD shouldBe subtreeAtD
        subtreeAtD shouldBe graphOfD
    }

    "subtreeAt on a subtree returns the correct nested subtree" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 13)
        graph.addEdge("A" to "C", 17)
        graph.addEdge("C" to "D", 19)
        graph.addEdge("C" to "E", 23)
        graph.addEdge("E" to "F", 29)
        val tree = graph.toTree()

        val subtreeAtC = tree.subtreeAt("C")
        val nestedSubtreeAtE = subtreeAtC.subtreeAt("E")
        nestedSubtreeAtE.nodes shouldBe setOf("E", "F")
        nestedSubtreeAtE.edges shouldBe mapOf(
            ("E" to "F") to 29,
        )
        nestedSubtreeAtE.root shouldBe "E"
        nestedSubtreeAtE.leaves shouldBe setOf("F")
        val graphOfE = MutableGraph<String, Int>()
        graphOfE.addEdge("E" to "F", 29)
        graphOfE shouldBe nestedSubtreeAtE
        nestedSubtreeAtE shouldBe graphOfE

        val deepSubtreeAtF = nestedSubtreeAtE.subtreeAt("F")
        deepSubtreeAtF.nodes shouldBe setOf("F")
        deepSubtreeAtF.edges shouldBe emptyMap()
        deepSubtreeAtF.root shouldBe "F"
        deepSubtreeAtF.leaves shouldBe setOf("F")
        val graphOfF = MutableGraph<String, Int>()
        graphOfF.addNode("F")
        graphOfF shouldBe deepSubtreeAtF
        deepSubtreeAtF shouldBe graphOfF
    }

    "subtreeAt on a subtree throws for node outside the subtree" {
        val graph = MutableGraph<String, Int>()
        graph.addEdge("A" to "B", 13)
        graph.addEdge("A" to "C", 17)
        graph.addEdge("C" to "D", 19)
        val tree = graph.toTree()

        val subtreeAtC = tree.subtreeAt("C")
        shouldThrow<IllegalArgumentException> { subtreeAtC.subtreeAt("B") }
        shouldThrow<IllegalArgumentException> { subtreeAtC.subtreeAt("A") }
    }
})