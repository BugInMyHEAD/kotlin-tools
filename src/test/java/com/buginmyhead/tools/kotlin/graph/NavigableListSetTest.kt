package com.buginmyhead.tools.kotlin.graph
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
internal class NavigableListSetTest : FreeSpec({
    // Full set: elements [A, B, C, D, E]
    fun fullSet() = NavigableListSet(listOf("A", "B", "C", "D", "E"))
    // Sub-view: elements [B, C, D]
    fun subSet() = fullSet().subView(1..3)
    // --- Set tests ---
    "size returns the number of elements" {
        fullSet().size shouldBe 5
        subSet().size shouldBe 3
    }
    "isEmpty returns true for empty set" {
        fullSet().isEmpty() shouldBe false
        fullSet().subView(IntRange.EMPTY).isEmpty() shouldBe true
    }
    "contains checks membership in the current view" {
        fullSet().contains("A") shouldBe true
        fullSet().contains("Z") shouldBe false
        subSet().contains("A") shouldBe false
        subSet().contains("B") shouldBe true
        subSet().contains("D") shouldBe true
        subSet().contains("E") shouldBe false
    }
    "iterator traverses in order" {
        fullSet().toList() shouldBe listOf("A", "B", "C", "D", "E")
        subSet().toList() shouldBe listOf("B", "C", "D")
    }
    "iterator next throws NoSuchElementException when exhausted" {
        shouldThrow<NoSuchElementException> { fullSet().subView(IntRange.EMPTY).iterator().next() }
    }
    "iterator remove throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().iterator().remove() }
    }
    "subView creates a narrower set view" {
        subSet().size shouldBe 3
        subSet().toSet() shouldBe setOf("B", "C", "D")
        subSet().contains("B") shouldBe true
        subSet().contains("A") shouldBe false
    }
    // --- SortedSet tests ---
    "comparator orders by insertion order" {
        val cmp = fullSet().comparator()
        (cmp.compare("A", "C") < 0) shouldBe true
        (cmp.compare("D", "B") > 0) shouldBe true
        cmp.compare("C", "C") shouldBe 0
    }
    "comparator throws NoSuchElementException for unknown element" {
        shouldThrow<NoSuchElementException> { fullSet().comparator().compare("A", "Z") }
    }
    "first returns the first element" {
        fullSet().first() shouldBe "A"
        subSet().first() shouldBe "B"
    }
    "first throws NoSuchElementException for empty set" {
        shouldThrow<NoSuchElementException> { fullSet().subView(IntRange.EMPTY).first() }
    }
    "last returns the last element" {
        fullSet().last() shouldBe "E"
        subSet().last() shouldBe "D"
    }
    "last throws NoSuchElementException for empty set" {
        shouldThrow<NoSuchElementException> { fullSet().subView(IntRange.EMPTY).last() }
    }
    // --- NavigableSet navigation ---
    "lower" {
        subSet().lower("B") shouldBe null
        subSet().lower("C") shouldBe "B"
        subSet().lower("D") shouldBe "C"
        subSet().lower("E") shouldBe "D"
        subSet().lower("A") shouldBe null
    }
    "lower throws NoSuchElementException for unknown element" {
        shouldThrow<NoSuchElementException> { fullSet().lower("Z") }
    }
    "floor" {
        subSet().floor("B") shouldBe "B"
        subSet().floor("D") shouldBe "D"
        subSet().floor("E") shouldBe "D"
        subSet().floor("A") shouldBe null
    }
    "ceiling" {
        subSet().ceiling("B") shouldBe "B"
        subSet().ceiling("D") shouldBe "D"
        subSet().ceiling("A") shouldBe "B"
        subSet().ceiling("E") shouldBe null
    }
    "higher" {
        subSet().higher("B") shouldBe "C"
        subSet().higher("C") shouldBe "D"
        subSet().higher("D") shouldBe null
        subSet().higher("A") shouldBe "B"
        subSet().higher("E") shouldBe null
    }
    // --- NavigableSet sub-set views ---
    "subSet with inclusivity flags" {
        subSet().subSet("B", true, "D", true).toSet() shouldBe setOf("B", "C", "D")
        subSet().subSet("B", false, "D", false).toSet() shouldBe setOf("C")
        subSet().subSet("C", true, "C", true).toSet() shouldBe setOf("C")
        subSet().subSet("C", false, "C", false).toSet() shouldBe emptySet()
        subSet().subSet("A", true, "E", true).toSet() shouldBe setOf("B", "C", "D")
    }
    "subSet SortedSet overload" {
        subSet().subSet("B", "D").toSet() shouldBe setOf("B", "C")
    }
    "headSet with inclusivity flag" {
        subSet().headSet("C", false).toSet() shouldBe setOf("B")
        subSet().headSet("C", true).toSet() shouldBe setOf("B", "C")
        subSet().headSet("A", true).toSet() shouldBe emptySet()
    }
    "headSet SortedSet overload" {
        subSet().headSet("D").toSet() shouldBe setOf("B", "C")
    }
    "tailSet with inclusivity flag" {
        subSet().tailSet("C", true).toSet() shouldBe setOf("C", "D")
        subSet().tailSet("C", false).toSet() shouldBe setOf("D")
        subSet().tailSet("E", true).toSet() shouldBe emptySet()
    }
    "tailSet SortedSet overload" {
        subSet().tailSet("C").toSet() shouldBe setOf("C", "D")
    }
    // --- Descending ---
    "descendingIterator" {
        subSet().descendingIterator().asSequence().toList() shouldBe listOf("D", "C", "B")
        fullSet().subView(IntRange.EMPTY).descendingIterator().hasNext() shouldBe false
    }
    "descendingIterator next throws NoSuchElementException when exhausted" {
        shouldThrow<NoSuchElementException> { fullSet().subView(IntRange.EMPTY).descendingIterator().next() }
    }
    "descendingIterator remove throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().descendingIterator().remove() }
    }
    "descendingSet throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().descendingSet() }
    }
    // --- Unmodifiable mutators ---
    "pollFirst throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().pollFirst() }
    }
    "pollLast throws UnsupportedOperationException" {
        shouldThrow<UnsupportedOperationException> { fullSet().pollLast() }
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
