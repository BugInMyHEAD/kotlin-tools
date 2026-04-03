package com.buginmyhead.tools.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows

internal class TraversalTest : FreeSpec({
    "bfs cycleSafe true remove duplicates in roots" {
        val result = bfs(listOf("A", "B", "A", "C")) { emptyList() }.toList()
        result shouldBe listOf("A", "B", "C")
    }

    "bfs cycleSafe false keeps duplicates in roots" {
        val result = bfs(cycleSafe = false, listOf("A", "B", "A", "C")) { emptyList() }.toList()
        result shouldBe listOf("A", "B", "A", "C")
    }

    "bfs cycleSafe true removes duplicates in traversal" {
        val result = bfs(listOf("A")) {
            when (it) {
                "A" -> listOf("B", "C", "B")
                else -> emptyList()
            }
        }.toList()

        result shouldBe listOf("A", "B", "C")
    }

    "bfs cycleSafe false keeps duplicates in traversal" {
        val result = bfs(cycleSafe = false, listOf("A")) {
            when (it) {
                "A" -> listOf("B", "C", "B")
                else -> emptyList()
            }
        }.toList()

        result shouldBe listOf("A", "B", "C", "B")
    }

    "iterable bfs cycleSafe true handles cycles" {
        val result = bfs(listOf("A")) {
            when (it) {
                "A" -> listOf("B")
                "B" -> listOf("A")
                else -> emptyList()
            }
        }.toList()
        result shouldBe listOf("A", "B")
    }

    "sequence bfs cycleSafe true handles cycles" {
        val result = bfs(sequenceOf("A")) {
            when (it) {
                "A" -> yield("B")
                "B" -> yield("A")
                else -> Unit
            }
        }.toList()
        result shouldBe listOf("A", "B")
    }

    "dfsPost yields nodes and freeze captures path" {
        val frozen = dfsPost(
            cycleSafe = true,
            roots = sequenceOf(5),
            initial = { 7 },
            aggregate = { r, _ -> r },
        ) {
            when (it) {
                5 -> {
                    yield(11)
                    yield(13)
                }
                else -> Unit
            }
        }.map { it.freeze() }.toList()

        frozen.map { it.node } shouldBe listOf(11, 13, 5)
        frozen.map { it.result } shouldBe listOf(7, 7, 7)
        frozen.map { it.path } shouldBe listOf(listOf(5, 11), listOf(5, 13), listOf(5))
    }

    "dfsPost pathToRoot captures ancestor chain from current node to root" {
        val paths = dfsPost(
            cycleSafe = true,
            roots = sequenceOf(5),
            initial = { 7 },
            aggregate = { r, _ -> r },
        ) {
            when (it) {
                5 -> {
                    yield(11)
                    yield(13)
                }
                11 -> yield(17)
                else -> Unit
            }
        }.map { it.pathToRoot.toList() }.toList()

        paths shouldBe listOf(
            listOf(17, 11, 5),
            listOf(11, 5),
            listOf(13, 5),
            listOf(5),
        )
    }

    "dfsPost freeze after sequence advanced throws" {
        val staleContexts = dfsPost(
            cycleSafe = true,
            roots = sequenceOf(5),
            initial = { 7 },
            aggregate = { r, _ -> r },
        ) {
            when (it) {
                5 -> yield(11)
                else -> Unit
            }
        }.toList()
        // after full iteration the last context is also terminated
        assertThrows<IllegalStateException> { staleContexts[0].freeze() }
        assertThrows<IllegalStateException> { staleContexts[1].freeze() }
    }
})