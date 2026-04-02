package com.buginmyhead.tools.kotlin.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.util.NavigableMap

internal class NavigableListMapTest : FreeSpec({

    // Full map: keys [A, B, C, D, E], values = key.length * 5
    fun fullMap() = NavigableListMap(listOf("A", "B", "C", "D", "E").map { it to it.length * 5 })

    // Sub-view: keys [B, C, D]
    fun subMap() = fullMap().subView(1..3)

    // --- Map tests ---

    "size returns the number of entries" {
        fullMap().size shouldBe 5
        subMap().size shouldBe 3
    }

    "isEmpty returns true for empty map" {
        fullMap().isEmpty() shouldBe false
        fullMap().subView(IntRange.EMPTY).isEmpty() shouldBe true
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
        val original = NavigableListMap(listOf("X", "Y").map { it to it.length })
        val replaced = original.withValues { it.hashCode() }
        replaced.keys shouldBe setOf("X", "Y")
        replaced["X"] shouldBe "X".hashCode()
    }

    "navigableListSetFrom creates a NavigableSet from a list" {
        val set = navigableListSetFrom(listOf("P", "Q"))
        set shouldBe setOf("P", "Q")
    }

    "equals works with other Map implementations" {
        val map = NavigableListMap(listOf("X", "Y").map { it to it.length * 7 })
        map shouldBe mapOf("X" to 7, "Y" to 7)
        (map == map) shouldBe true
        (map.equals("not a map")) shouldBe false
        (map == mapOf("X" to 7)) shouldBe false
        (map == mapOf("X" to 7, "Y" to 99)) shouldBe false
    }

    "hashCode is consistent with equals" {
        val map1 = NavigableListMap(listOf("A" to 5, "B" to 5))
        val map2 = mapOf("A" to 5, "B" to 5)
        map1.hashCode() shouldBe map2.hashCode()
    }

    "toString formats as map" {
        NavigableListMap(listOf("A" to 5)).toString() shouldBe "{A=5}"
    }

    // --- KeySet (NavigableSet) tests ---

    fun fullKeys() = fullMap().keys
    fun subKeys() = subMap().keys

    "keys size and isEmpty" {
        fullKeys().size shouldBe 5
        fullKeys().isEmpty() shouldBe false
        fullMap().subView(IntRange.EMPTY).keys.isEmpty() shouldBe true
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
        shouldThrow<NoSuchElementException> { fullMap().subView(IntRange.EMPTY).keys.iterator().next() }
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
        shouldThrow<NoSuchElementException> { fullMap().subView(IntRange.EMPTY).keys.first() }
    }

    "keys last throws NoSuchElementException for empty set" {
        shouldThrow<NoSuchElementException> { fullMap().subView(IntRange.EMPTY).keys.last() }
    }

    "keys comparator orders by insertion order" {
        val cmp = fullKeys().comparator()
        (cmp.compare("A", "C") < 0) shouldBe true
        (cmp.compare("D", "B") > 0) shouldBe true
        cmp.compare("C", "C") shouldBe 0
    }

    "keys comparator throws NoSuchElementException for unknown element" {
        shouldThrow<NoSuchElementException> { fullKeys().comparator().compare("A", "Z") }
    }

    "keys lower" {
        subKeys().lower("B") shouldBe null
        subKeys().lower("C") shouldBe "B"
        subKeys().lower("D") shouldBe "C"
        subKeys().lower("E") shouldBe "D"
        subKeys().lower("A") shouldBe null
    }

    "keys lower throws NoSuchElementException for unknown element" {
        shouldThrow<NoSuchElementException> { fullKeys().lower("Z") }
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
        fullMap().subView(IntRange.EMPTY).keys.descendingIterator().hasNext() shouldBe false
    }

    "keys descendingIterator next throws NoSuchElementException when exhausted" {
        shouldThrow<NoSuchElementException> { fullMap().subView(IntRange.EMPTY).keys.descendingIterator().next() }
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

    // --- NavigableMap tests ---

    "comparator orders by insertion order" {
        val cmp = fullMap().comparator()
        (cmp.compare("A", "C") < 0) shouldBe true
        (cmp.compare("D", "B") > 0) shouldBe true
        cmp.compare("C", "C") shouldBe 0
    }

    "comparator throws NoSuchElementException for unknown key" {
        shouldThrow<NoSuchElementException> { fullMap().comparator().compare("A", "Z") }
    }

    "firstKey returns the first key" {
        fullMap().firstKey() shouldBe "A"
        subMap().firstKey() shouldBe "B"
    }

    "firstKey throws NoSuchElementException for empty map" {
        shouldThrow<NoSuchElementException> { fullMap().subView(IntRange.EMPTY).firstKey() }
    }

    "lastKey returns the last key" {
        fullMap().lastKey() shouldBe "E"
        subMap().lastKey() shouldBe "D"
    }

    "lastKey throws NoSuchElementException for empty map" {
        shouldThrow<NoSuchElementException> { fullMap().subView(IntRange.EMPTY).lastKey() }
    }

    "lowerEntry returns entry with greatest key strictly less" {
        subMap().lowerEntry("C")?.let { it.key to it.value } shouldBe ("B" to 5)
        subMap().lowerEntry("B") shouldBe null
    }

    "floorEntry returns entry with greatest key less than or equal" {
        subMap().floorEntry("C")?.let { it.key to it.value } shouldBe ("C" to 5)
        subMap().floorEntry("A") shouldBe null
    }

    "ceilingEntry returns entry with least key greater than or equal" {
        subMap().ceilingEntry("C")?.let { it.key to it.value } shouldBe ("C" to 5)
        subMap().ceilingEntry("E") shouldBe null
    }

    "higherEntry returns entry with least key strictly greater" {
        subMap().higherEntry("C")?.let { it.key to it.value } shouldBe ("D" to 5)
        subMap().higherEntry("D") shouldBe null
    }

    "firstEntry returns first entry or null for empty map" {
        fullMap().firstEntry()?.let { it.key to it.value } shouldBe ("A" to 5)
        fullMap().subView(IntRange.EMPTY).firstEntry() shouldBe null
    }

    "lastEntry returns last entry or null for empty map" {
        fullMap().lastEntry()?.let { it.key to it.value } shouldBe ("E" to 5)
        fullMap().subView(IntRange.EMPTY).lastEntry() shouldBe null
    }

    "navigableKeySet returns the same keys view" {
        fullMap().navigableKeySet() shouldBe fullMap().keys
    }

    "subMap with inclusivity flags" {
        subMap().subMap("B", true, "D", true).keys shouldBe setOf("B", "C", "D")
        subMap().subMap("B", false, "D", false).keys shouldBe setOf("C")
    }

    "headMap with inclusivity flag" {
        subMap().headMap("C", true).keys shouldBe setOf("B", "C")
        subMap().headMap("C", false).keys shouldBe setOf("B")
    }

    "tailMap with inclusivity flag" {
        subMap().tailMap("C", true).keys shouldBe setOf("C", "D")
        subMap().tailMap("C", false).keys shouldBe setOf("D")
    }

    "SortedMap subMap overload" {
        subMap().subMap("B", "D").keys shouldBe setOf("B", "C")
    }

    "SortedMap headMap overload" {
        subMap().headMap("D").keys shouldBe setOf("B", "C")
    }

    "SortedMap tailMap overload" {
        subMap().tailMap("C").keys shouldBe setOf("C", "D")
    }

    "entry setValue throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> {
            (fullMap().firstEntry() as MutableMap.MutableEntry).setValue(7)
        }
    }

    "put throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> {
            (fullMap() as NavigableMap<String, Int>).put("Z", 7)
        }
    }

    "remove throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> {
            (fullMap() as NavigableMap<String, Int>).remove("A")
        }
    }

    "putAll throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> {
            (fullMap() as NavigableMap<String, Int>).putAll(mapOf("Z" to 7))
        }
    }

    "clear throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> {
            (fullMap() as NavigableMap<String, Int>).clear()
        }
    }

    "pollFirstEntry throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullMap().pollFirstEntry() }
    }

    "pollLastEntry throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullMap().pollLastEntry() }
    }

    "descendingMap throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullMap().descendingMap() }
    }

    "descendingKeySet throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullMap().descendingKeySet() }
    }
})