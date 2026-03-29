package com.buginmyhead.tools.kotlin.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

internal class ListOrderedMapTest : FreeSpec({

    // Full map: keys [A, B, C, D, E], values = key.length * 5
    fun fullMap() = ListOrderedMap(listOf("A", "B", "C", "D", "E")) { it.length * 5 }

    // Sub-view: keys [B, C, D]
    fun subMap() = fullMap().subView(1, 4)

    // --- Map tests ---

    "size returns the number of entries" {
        fullMap().size shouldBe 5
        subMap().size shouldBe 3
    }

    "isEmpty returns true for empty map" {
        fullMap().isEmpty() shouldBe false
        fullMap().subView(0, 0).isEmpty() shouldBe true
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
        subMap()["B"] shouldBe 5
    }

    "keyAt returns the key at offset" {
        fullMap().keyAt(0) shouldBe "A"
        fullMap().keyAt(4) shouldBe "E"
        subMap().keyAt(0) shouldBe "B"
        subMap().keyAt(2) shouldBe "D"
    }

    "keyAt throws IndexOutOfBoundsException for invalid offset" {
        shouldThrow<IndexOutOfBoundsException> { fullMap().keyAt(-1) }
        shouldThrow<IndexOutOfBoundsException> { fullMap().keyAt(5) }
        shouldThrow<IndexOutOfBoundsException> { subMap().keyAt(3) }
    }

    "globalIndexOf returns the global index in the backing list" {
        fullMap().globalIndexOf("A") shouldBe 0
        fullMap().globalIndexOf("E") shouldBe 4
        fullMap().globalIndexOf("Z") shouldBe -1
        subMap().globalIndexOf("A") shouldBe 0
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
        entries.map { it.key to it.value } shouldBe listOf(
            "A" to 5, "B" to 5, "C" to 5, "D" to 5, "E" to 5,
        )
        entries.first().toString() shouldBe "A=5"
    }

    "entries equals works across Map.Entry implementations" {
        val entry = fullMap().entries.first()
        val other = mapOf("A" to 5).entries.first()
        (entry == other) shouldBe true
    }

    "subView creates a narrower map view" {
        subMap().size shouldBe 3
        subMap().keys shouldBe setOf("B", "C", "D")
        subMap()["B"] shouldBe 5
        subMap()["A"] shouldBe null
    }

    "withValues creates a map with different value function" {
        val original = ListOrderedMap(listOf("X", "Y")) { it.length }
        val replaced = original.withValues { it.hashCode() }
        replaced.keys shouldBe setOf("X", "Y")
        replaced["X"] shouldBe "X".hashCode()
    }

    "ofKeys creates a key-only map" {
        val keyOnly = ListOrderedMap.ofKeys(listOf("P", "Q"))
        keyOnly.keys shouldBe setOf("P", "Q")
        keyOnly["P"] shouldBe Unit
    }

    "equals works with other Map implementations" {
        val map = ListOrderedMap(listOf("X", "Y")) { it.length * 7 }
        map shouldBe mapOf("X" to 7, "Y" to 7)
        (map == map) shouldBe true
        (map.equals("not a map")) shouldBe false
        (map == mapOf("X" to 7)) shouldBe false
        (map == mapOf("X" to 7, "Y" to 99)) shouldBe false
    }

    "hashCode is consistent with equals" {
        val map1 = ListOrderedMap(listOf("A", "B")) { 5 }
        val map2 = mapOf("A" to 5, "B" to 5)
        map1.hashCode() shouldBe map2.hashCode()
    }

    "toString formats as map" {
        ListOrderedMap(listOf("A")) { 5 }.toString() shouldBe "{A=5}"
    }

    // --- KeySet (NavigableSet) tests ---

    fun fullKeys() = fullMap().keys
    fun subKeys() = subMap().keys

    "keys size and isEmpty" {
        fullKeys().size shouldBe 5
        fullKeys().isEmpty() shouldBe false
        fullMap().subView(0, 0).keys.isEmpty() shouldBe true
    }

    "keys contains checks membership in the current view" {
        fullKeys().contains("A") shouldBe true
        fullKeys().contains("Z") shouldBe false
        subKeys().contains("A") shouldBe false
        subKeys().contains("B") shouldBe true
    }

    "keys iterator traverses in order" {
        fullKeys().toList() shouldBe listOf("A", "B", "C", "D", "E")
        subKeys().toList() shouldBe listOf("B", "C", "D")
    }

    "keys iterator next throws NoSuchElementException when exhausted" {
        shouldThrow<NoSuchElementException> { fullMap().subView(0, 0).keys.iterator().next() }
    }

    "keys iterator remove throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullKeys().iterator().remove() }
    }

    "keys first and last" {
        fullKeys().first() shouldBe "A"
        fullKeys().last() shouldBe "E"
        subKeys().first() shouldBe "B"
        subKeys().last() shouldBe "D"
    }

    "keys first throws NoSuchElementException for empty set" {
        shouldThrow<NoSuchElementException> { fullMap().subView(0, 0).keys.first() }
    }

    "keys last throws NoSuchElementException for empty set" {
        shouldThrow<NoSuchElementException> { fullMap().subView(0, 0).keys.last() }
    }

    "keys comparator orders by insertion order" {
        val cmp = fullKeys().comparator()
        (cmp.compare("A", "C") < 0) shouldBe true
        (cmp.compare("D", "B") > 0) shouldBe true
        cmp.compare("C", "C") shouldBe 0
    }

    "keys comparator throws ClassCastException for unknown element" {
        shouldThrow<ClassCastException> { fullKeys().comparator().compare("A", "Z") }
    }

    "keys lower" {
        subKeys().lower("B") shouldBe null
        subKeys().lower("C") shouldBe "B"
        subKeys().lower("D") shouldBe "C"
        subKeys().lower("E") shouldBe "D"
        subKeys().lower("A") shouldBe null
    }

    "keys lower throws ClassCastException for unknown element" {
        shouldThrow<ClassCastException> { fullKeys().lower("Z") }
    }

    "keys floor" {
        subKeys().floor("B") shouldBe "B"
        subKeys().floor("D") shouldBe "D"
        subKeys().floor("E") shouldBe "D"
        subKeys().floor("A") shouldBe null
    }

    "keys ceiling" {
        subKeys().ceiling("B") shouldBe "B"
        subKeys().ceiling("D") shouldBe "D"
        subKeys().ceiling("A") shouldBe "B"
        subKeys().ceiling("E") shouldBe null
    }

    "keys higher" {
        subKeys().higher("B") shouldBe "C"
        subKeys().higher("C") shouldBe "D"
        subKeys().higher("D") shouldBe null
        subKeys().higher("A") shouldBe "B"
        subKeys().higher("E") shouldBe null
    }

    "keys subSet" {
        subKeys().subSet("B", true, "D", true).toSet() shouldBe setOf("B", "C", "D")
        subKeys().subSet("B", false, "D", false).toSet() shouldBe setOf("C")
        subKeys().subSet("C", true, "C", true).toSet() shouldBe setOf("C")
        subKeys().subSet("C", false, "C", false).toSet() shouldBe emptySet()
        subKeys().subSet("A", true, "E", true).toSet() shouldBe setOf("B", "C", "D")
    }

    "keys subSet SortedSet overload" {
        subKeys().subSet("B", "D").toSet() shouldBe setOf("B", "C")
    }

    "keys headSet" {
        subKeys().headSet("C", false).toSet() shouldBe setOf("B")
        subKeys().headSet("C", true).toSet() shouldBe setOf("B", "C")
        subKeys().headSet("A", true).toSet() shouldBe emptySet()
    }

    "keys headSet SortedSet overload" {
        subKeys().headSet("D").toSet() shouldBe setOf("B", "C")
    }

    "keys tailSet" {
        subKeys().tailSet("C", true).toSet() shouldBe setOf("C", "D")
        subKeys().tailSet("C", false).toSet() shouldBe setOf("D")
        subKeys().tailSet("E", true).toSet() shouldBe emptySet()
    }

    "keys tailSet SortedSet overload" {
        subKeys().tailSet("C").toSet() shouldBe setOf("C", "D")
    }

    "keys descendingIterator" {
        subKeys().descendingIterator().asSequence().toList() shouldBe listOf("D", "C", "B")
        fullMap().subView(0, 0).keys.descendingIterator().hasNext() shouldBe false
    }

    "keys descendingIterator next throws NoSuchElementException when exhausted" {
        shouldThrow<NoSuchElementException> { fullMap().subView(0, 0).keys.descendingIterator().next() }
    }

    "keys descendingIterator remove throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullKeys().descendingIterator().remove() }
    }

    "keys pollFirst throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullKeys().pollFirst() }
    }

    "keys pollLast throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullKeys().pollLast() }
    }

    "keys descendingSet throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullKeys().descendingSet() }
    }

    "keys add throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullKeys().add("Z") }
    }

    "keys remove throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullKeys().remove("A") }
    }

    "keys addAll throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullKeys().addAll(listOf("Z")) }
    }

    "keys removeAll throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullKeys().removeAll(setOf("A")) }
    }

    "keys retainAll throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullKeys().retainAll(setOf("A")) }
    }

    "keys clear throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullKeys().clear() }
    }
})