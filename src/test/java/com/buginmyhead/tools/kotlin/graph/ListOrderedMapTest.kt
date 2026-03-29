package com.buginmyhead.tools.kotlin.graph

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

internal class ListOrderedMapTest : FreeSpec({

    fun fullMap() = ListOrderedMap(
        ListOrderedSet(listOf("A", "B", "C", "D", "E")),
    ) { key -> key.length * 5 }

    fun subMap(): ListOrderedMap<String, Int> {
        val fullKeys = ListOrderedSet(listOf("A", "B", "C", "D", "E"))
        val map = ListOrderedMap(fullKeys) { key -> key.hashCode() }
        return map.subMapView(fullKeys.subView(1, 4))
    }

    "size returns the number of entries" {
        fullMap().size shouldBe 5
        subMap().size shouldBe 3
    }

    "isEmpty returns true for empty map" {
        fullMap().isEmpty() shouldBe false
        val emptyKeys = ListOrderedSet(listOf("A")).subView(0, 0)
        ListOrderedMap(emptyKeys) { 0 }.isEmpty() shouldBe true
    }

    "containsKey checks key membership in the current view" {
        fullMap().containsKey("A") shouldBe true
        fullMap().containsKey("Z") shouldBe false

        subMap().containsKey("A") shouldBe false
        subMap().containsKey("B") shouldBe true
        subMap().containsKey("D") shouldBe true
        subMap().containsKey("E") shouldBe false
    }

    "containsValue checks if any value matches" {
        fullMap().containsValue(5) shouldBe true
        fullMap().containsValue(99) shouldBe false
    }

    "get returns value for existing key and null for missing key" {
        fullMap()["A"] shouldBe 5
        fullMap()["Z"] shouldBe null

        subMap()["A"] shouldBe null
        subMap()["B"] shouldBe "B".hashCode()
    }

    "keys returns the ordered key set" {
        fullMap().keys shouldBe setOf("A", "B", "C", "D", "E")
        subMap().keys shouldBe setOf("B", "C", "D")
    }

    "values returns the values in key order" {
        fullMap().values.toList() shouldBe listOf(5, 5, 5, 5, 5)
    }

    "entries returns key-value pairs" {
        val entries = fullMap().entries
        entries.size shouldBe 5
        entries.map { it.key to it.value } shouldBe listOf(
            "A" to 5, "B" to 5, "C" to 5, "D" to 5, "E" to 5,
        )
    }

    "subMapView creates a narrower map view" {
        val sub = subMap()
        sub.size shouldBe 3
        sub.keys shouldBe setOf("B", "C", "D")
        sub["B"] shouldBe "B".hashCode()
        sub["A"] shouldBe null
    }

    "equals works with other Map implementations" {
        val map = ListOrderedMap(
            ListOrderedSet(listOf("X", "Y")),
        ) { key -> key.length * 7 }
        map shouldBe mapOf("X" to 7, "Y" to 7)
        (map == map) shouldBe true
        (map.equals("not a map")) shouldBe false
        (map == mapOf("X" to 7)) shouldBe false
        (map == mapOf("X" to 7, "Y" to 99)) shouldBe false
    }

    "hashCode is consistent with equals" {
        val map1 = ListOrderedMap(
            ListOrderedSet(listOf("A", "B")),
        ) { 5 }
        val map2 = mapOf("A" to 5, "B" to 5)
        map1.hashCode() shouldBe map2.hashCode()
    }

    "toString formats as map" {
        val map = ListOrderedMap(
            ListOrderedSet(listOf("A")),
        ) { 5 }
        map.toString() shouldBe "{A=5}"
    }
})