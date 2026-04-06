package com.buginmyhead.tools.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

internal class UnsafeMergeCollectionsTest : FreeSpec({

    "UnsafeMergeCollection" - {

        "size returns sum of all collections" {
            UnsafeMergeCollection(listOf(13, 17), listOf(19)).size shouldBe 3
        }

        "size of empty collections returns 0" {
            UnsafeMergeCollection(emptyList<Int>(), emptyList()).size shouldBe 0
        }

        "isEmpty returns true when all collections are empty" {
            UnsafeMergeCollection(emptyList<Int>(), emptyList()).isEmpty() shouldBe true
        }

        "isEmpty returns false when any collection is non-empty" {
            UnsafeMergeCollection(emptyList(), listOf(13)).isEmpty() shouldBe false
        }

        "contains returns true when element exists in any collection" {
            UnsafeMergeCollection(listOf(13), listOf(17)).contains(17) shouldBe true
        }

        "contains returns false when element is absent" {
            UnsafeMergeCollection(listOf(13), listOf(17)).contains(19) shouldBe false
        }

        "containsAll returns true when all elements are found" {
            UnsafeMergeCollection(listOf(13, 17), listOf(19, 23)).containsAll(listOf(13, 23)) shouldBe true
        }

        "containsAll returns false when some elements are missing" {
            UnsafeMergeCollection(listOf(13), listOf(17)).containsAll(listOf(13, 19)) shouldBe false
        }

        "iterator yields elements from all collections in order" {
            UnsafeMergeCollection(listOf(13, 17), listOf(19), listOf(23, 29))
                .iterator().asSequence().toList() shouldBe listOf(13, 17, 19, 23, 29)
        }

        "iterator of empty collections yields nothing" {
            UnsafeMergeCollection(emptyList<Int>(), emptyList())
                .iterator().asSequence().toList() shouldBe emptyList()
        }

    }

    "UnsafeMergeSet" - {

        "delegates size to UnsafeMergeCollection" {
            UnsafeMergeSet(setOf(13, 17), setOf(19)).size shouldBe 3
        }

        "delegates isEmpty to UnsafeMergeCollection" {
            UnsafeMergeSet(emptySet<Int>(), emptySet()).isEmpty() shouldBe true
            UnsafeMergeSet(emptySet(), setOf(13)).isEmpty() shouldBe false
        }

        "delegates contains to UnsafeMergeCollection" {
            UnsafeMergeSet(setOf(13), setOf(17)).contains(17) shouldBe true
            UnsafeMergeSet(setOf(13), setOf(17)).contains(19) shouldBe false
        }

        "delegates containsAll to UnsafeMergeCollection" {
            UnsafeMergeSet(setOf(13), setOf(17)).containsAll(setOf(13, 17)) shouldBe true
            UnsafeMergeSet(setOf(13), setOf(17)).containsAll(setOf(13, 19)) shouldBe false
        }

        "delegates iterator to UnsafeMergeCollection" {
            UnsafeMergeSet(setOf(13), setOf(17, 19))
                .iterator().asSequence().toList() shouldBe listOf(13, 17, 19)
        }

    }

    "UnsafeMergeMap" - {

        "size returns sum of all map sizes" {
            UnsafeMergeMap(mapOf(13 to "a", 17 to "b"), mapOf(19 to "c")).size shouldBe 3
        }

        "isEmpty returns true when all maps are empty" {
            UnsafeMergeMap(emptyMap<Int, String>(), emptyMap()).isEmpty() shouldBe true
        }

        "isEmpty returns false when any map is non-empty" {
            UnsafeMergeMap(emptyMap(), mapOf(13 to "a")).isEmpty() shouldBe false
        }

        "containsKey returns true when key exists in any map" {
            UnsafeMergeMap(mapOf(13 to "a"), mapOf(17 to "b")).containsKey(17) shouldBe true
        }

        "containsKey returns false when key is absent" {
            UnsafeMergeMap(mapOf(13 to "a"), mapOf(17 to "b")).containsKey(19) shouldBe false
        }

        "containsValue returns true when value exists in any map" {
            UnsafeMergeMap(mapOf(13 to "a"), mapOf(17 to "b")).containsValue("b") shouldBe true
        }

        "containsValue returns false when value is absent" {
            UnsafeMergeMap(mapOf(13 to "a"), mapOf(17 to "b")).containsValue("c") shouldBe false
        }

        "get returns value when key exists" {
            UnsafeMergeMap(mapOf(13 to "a"), mapOf(17 to "b"))[17] shouldBe "b"
        }

        "get returns null when key is absent" {
            UnsafeMergeMap(mapOf(13 to "a"), mapOf(17 to "b"))[19] shouldBe null
        }

        "keys returns merged key set" {
            UnsafeMergeMap(mapOf(13 to "a"), mapOf(17 to "b", 19 to "c"))
                .keys.toSet() shouldBe setOf(13, 17, 19)
        }

        "values returns merged values" {
            UnsafeMergeMap(mapOf(13 to "a"), mapOf(17 to "b", 19 to "c"))
                .values.toList() shouldBe listOf("a", "b", "c")
        }

        "entries returns merged entry set" {
            val entries = UnsafeMergeMap(mapOf(13 to "a"), mapOf(17 to "b")).entries
            entries.map { it.key to it.value }.toSet() shouldBe setOf(13 to "a", 17 to "b")
        }

    }

})