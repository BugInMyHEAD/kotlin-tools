package com.buginmyhead.tools.kotlin.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

internal class ListOrderedSetTest : FreeSpec({

    // Backing list: [A, B, C, D, E] with global indices 0..4
    fun fullSet() = ListOrderedSet(listOf("A", "B", "C", "D", "E"))

    // Sub-view: [B, C, D] with global indices 1..3
    fun subViewBCD() = fullSet().subView(1, 4)

    "size returns the number of elements" {
        fullSet().size shouldBe 5
        subViewBCD().size shouldBe 3
        fullSet().subView(0, 0).size shouldBe 0
    }

    "isEmpty returns true for empty set" {
        fullSet().isEmpty() shouldBe false
        fullSet().subView(0, 0).isEmpty() shouldBe true
    }

    "contains checks membership in the current view" {
        val full = fullSet()
        full.contains("A") shouldBe true
        full.contains("E") shouldBe true
        full.contains("Z") shouldBe false

        val sub = subViewBCD()
        sub.contains("A") shouldBe false
        sub.contains("B") shouldBe true
        sub.contains("D") shouldBe true
        sub.contains("E") shouldBe false
    }

    "iterator traverses elements in order" {
        fullSet().iterator().asSequence().toList() shouldBe listOf("A", "B", "C", "D", "E")
        subViewBCD().iterator().asSequence().toList() shouldBe listOf("B", "C", "D")
        fullSet().subView(0, 0).iterator().hasNext() shouldBe false
    }

    "iterator next throws NoSuchElementException when exhausted" {
        val iter = fullSet().subView(0, 0).iterator()
        shouldThrow<NoSuchElementException> { iter.next() }
    }

    "iterator remove throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().iterator().remove() }
    }

    "get returns element at offset within the view" {
        fullSet()[0] shouldBe "A"
        fullSet()[4] shouldBe "E"

        subViewBCD()[0] shouldBe "B"
        subViewBCD()[2] shouldBe "D"
    }

    "get throws IndexOutOfBoundsException for invalid offset" {
        shouldThrow<IndexOutOfBoundsException> { fullSet()[-1] }
        shouldThrow<IndexOutOfBoundsException> { fullSet()[5] }
        shouldThrow<IndexOutOfBoundsException> { subViewBCD()[3] }
    }

    "globalIndexOf returns the global index in the backing list" {
        val full = fullSet()
        full.globalIndexOf("A") shouldBe 0
        full.globalIndexOf("E") shouldBe 4
        full.globalIndexOf("Z") shouldBe -1

        val sub = subViewBCD()
        sub.globalIndexOf("A") shouldBe 0
        sub.globalIndexOf("B") shouldBe 1
        sub.globalIndexOf("Z") shouldBe -1
    }

    "subView creates a lightweight view over the global index range" {
        val sub = fullSet().subView(2, 4)
        sub.size shouldBe 2
        sub.toSet() shouldBe setOf("C", "D")
    }

    "equals works with other Set implementations" {
        fullSet() shouldBe setOf("A", "B", "C", "D", "E")
        subViewBCD() shouldBe setOf("B", "C", "D")
    }

    "first returns the first element" {
        fullSet().first() shouldBe "A"
        subViewBCD().first() shouldBe "B"
    }

    "first throws NoSuchElementException for empty set" {
        shouldThrow<NoSuchElementException> { fullSet().subView(0, 0).first() }
    }

    "last returns the last element" {
        fullSet().last() shouldBe "E"
        subViewBCD().last() shouldBe "D"
    }

    "last throws NoSuchElementException for empty set" {
        shouldThrow<NoSuchElementException> { fullSet().subView(0, 0).last() }
    }

    "comparator orders by insertion order" {
        val cmp = fullSet().comparator()
        (cmp.compare("A", "C") < 0) shouldBe true
        (cmp.compare("D", "B") > 0) shouldBe true
        cmp.compare("C", "C") shouldBe 0
    }

    "comparator throws ClassCastException for unknown element" {
        shouldThrow<ClassCastException> { fullSet().comparator().compare("A", "Z") }
    }

    "lower returns the greatest element strictly less" {
        val sub = subViewBCD()
        sub.lower("B") shouldBe null
        sub.lower("C") shouldBe "B"
        sub.lower("D") shouldBe "C"
        sub.lower("E") shouldBe "D"
        sub.lower("A") shouldBe null
    }

    "lower throws ClassCastException for unknown element" {
        shouldThrow<ClassCastException> { fullSet().lower("Z") }
    }

    "floor returns the greatest element less than or equal" {
        val sub = subViewBCD()
        sub.floor("B") shouldBe "B"
        sub.floor("D") shouldBe "D"
        sub.floor("E") shouldBe "D"
        sub.floor("A") shouldBe null
    }

    "ceiling returns the smallest element greater than or equal" {
        val sub = subViewBCD()
        sub.ceiling("B") shouldBe "B"
        sub.ceiling("D") shouldBe "D"
        sub.ceiling("A") shouldBe "B"
        sub.ceiling("E") shouldBe null
    }

    "higher returns the smallest element strictly greater" {
        val sub = subViewBCD()
        sub.higher("B") shouldBe "C"
        sub.higher("C") shouldBe "D"
        sub.higher("D") shouldBe null
        sub.higher("A") shouldBe "B"
        sub.higher("E") shouldBe null
    }

    "subSet creates a navigable subset" {
        val sub = subViewBCD()
        sub.subSet("B", true, "D", true).toSet() shouldBe setOf("B", "C", "D")
        sub.subSet("B", false, "D", false).toSet() shouldBe setOf("C")
        sub.subSet("C", true, "C", true).toSet() shouldBe setOf("C")
        sub.subSet("C", false, "C", false).toSet() shouldBe emptySet()
        sub.subSet("A", true, "E", true).toSet() shouldBe setOf("B", "C", "D")
    }

    "subSet SortedSet overload uses inclusive-from exclusive-to" {
        subViewBCD().subSet("B", "D").toSet() shouldBe setOf("B", "C")
    }

    "headSet creates a head view" {
        val sub = subViewBCD()
        sub.headSet("C", false).toSet() shouldBe setOf("B")
        sub.headSet("C", true).toSet() shouldBe setOf("B", "C")
        sub.headSet("A", true).toSet() shouldBe emptySet()
    }

    "headSet SortedSet overload uses exclusive-to" {
        subViewBCD().headSet("D").toSet() shouldBe setOf("B", "C")
    }

    "tailSet creates a tail view" {
        val sub = subViewBCD()
        sub.tailSet("C", true).toSet() shouldBe setOf("C", "D")
        sub.tailSet("C", false).toSet() shouldBe setOf("D")
        sub.tailSet("E", true).toSet() shouldBe emptySet()
    }

    "tailSet SortedSet overload uses inclusive-from" {
        subViewBCD().tailSet("C").toSet() shouldBe setOf("C", "D")
    }

    "descendingIterator traverses in reverse" {
        subViewBCD().descendingIterator().asSequence().toList() shouldBe listOf("D", "C", "B")
        fullSet().subView(0, 0).descendingIterator().hasNext() shouldBe false
    }

    "descendingIterator next throws NoSuchElementException when exhausted" {
        val iter = fullSet().subView(0, 0).descendingIterator()
        shouldThrow<NoSuchElementException> { iter.next() }
    }

    "descendingIterator remove throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().descendingIterator().remove() }
    }

    "pollFirst throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().pollFirst() }
    }

    "pollLast throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().pollLast() }
    }

    "descendingSet throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().descendingSet() }
    }

    "add throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().add("Z") }
    }

    "remove throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().remove("A") }
    }

    "addAll throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().addAll(listOf("Z")) }
    }

    "removeAll throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().removeAll(setOf("A")) }
    }

    "retainAll throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().retainAll(setOf("A")) }
    }

    "clear throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().clear() }
    }
})