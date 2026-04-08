package com.buginmyhead.tools.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

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

    "dfsPost pathToRoot captures ancestor chain from current node to root" {
        val paths = dfsPost(
            cycleSafe = true,
            roots = sequenceOf(13),
            initial = { 17 },
            aggregate = { r, _ -> r },
        ) {
            when (it) {
                13 -> {
                    yield(19)
                    yield(23)
                }
                19 -> yield(29)
                else -> Unit
            }
        }.map { it.pathToRoot.toList() }.toList()

        paths shouldBe listOf(
            listOf(29, 19, 13),
            listOf(19, 13),
            listOf(23, 13),
            listOf(13),
        )
    }
})