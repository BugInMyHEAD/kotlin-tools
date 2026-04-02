package com.buginmyhead.tools.kotlin.graph

import java.util.NavigableSet
import java.util.SortedSet

/**
 * An unmodifiable [NavigableSet] backed by a list and an index map, where element ordering
 * is defined by the original insertion order.
 *
 * Supports O(1) [contains] and [subView].
 * Sub-views share the same backing data and are O(1) to create.
 */
internal class NavigableListSet<E> private constructor(
    private val list: List<E>,
    private val elementToIndex: Map<E, Int>,
    private val window: IntRange,
) : AbstractSet<E>(), NavigableSet<E> {

    /**
     * Creates a [NavigableListSet] from the given element list.
     * Elements must be unique; duplicates result in undefined behavior.
     */
    constructor(elements: List<E>) : this(
        elements,
        elements.withIndex().associate { it.value to it.index },
        elements.indices,
    )

    // --- Sub-view creation ---

    /** Creates a sub-view for the given global index range. O(1) operation. */
    fun subView(range: IntRange): NavigableListSet<E> =
        NavigableListSet(list, elementToIndex, range)

    // --- Set implementation ---

    override val size: Int get() = maxOf(0, window.last - window.first + 1)

    override fun isEmpty(): Boolean = window.isEmpty()

    override fun contains(element: E): Boolean {
        val idx = elementToIndex[element] ?: return false
        return idx in window
    }

    override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {

        private var cursor = window.first
        override fun hasNext(): Boolean = cursor <= window.last
        override fun next(): E {
            if (!hasNext()) throw NoSuchElementException()
            return list[cursor++]
        }

        override fun remove(): Unit = throw UnsupportedOperationException()

    }

    // --- SortedSet ---

    override fun comparator(): Comparator<in E> =
        compareBy(elementToIndex::getValue)

    override fun first(): E {
        if (isEmpty()) throw NoSuchElementException()
        return list[window.first]
    }

    override fun last(): E {
        if (isEmpty()) throw NoSuchElementException()
        return list[window.last]
    }

    override fun subSet(fromElement: E, toElement: E): SortedSet<E> =
        subSet(fromElement, true, toElement, false)

    override fun headSet(toElement: E): SortedSet<E> =
        headSet(toElement, false)

    override fun tailSet(fromElement: E): SortedSet<E> =
        tailSet(fromElement, true)

    // --- NavigableSet navigation ---

    override fun lower(e: E): E? {
        val g = elementToIndex.getValue(e)
        val idx = minOf(g - 1, window.last)
        return if (idx in window) list[idx] else null
    }

    override fun floor(e: E): E? {
        val g = elementToIndex.getValue(e)
        val idx = minOf(g, window.last)
        return if (idx in window) list[idx] else null
    }

    override fun ceiling(e: E): E? {
        val g = elementToIndex.getValue(e)
        val idx = maxOf(g, window.first)
        return if (idx in window) list[idx] else null
    }

    override fun higher(e: E): E? {
        val g = elementToIndex.getValue(e)
        val idx = maxOf(g + 1, window.first)
        return if (idx in window) list[idx] else null
    }

    override fun pollFirst(): E = throw UnsupportedOperationException()

    override fun pollLast(): E = throw UnsupportedOperationException()

    override fun descendingIterator(): MutableIterator<E> = object : MutableIterator<E> {

        private var cursor = window.last
        override fun hasNext(): Boolean = cursor >= window.first
        override fun next(): E {
            if (!hasNext()) throw NoSuchElementException()
            return list[cursor--]
        }

        override fun remove(): Unit = throw UnsupportedOperationException()

    }

    override fun descendingSet(): NavigableSet<E> = throw UnsupportedOperationException()

    // --- NavigableSet sub-set views ---

    override fun subSet(
        fromElement: E, fromInclusive: Boolean,
        toElement: E, toInclusive: Boolean,
    ): NavigableSet<E> {
        val fromGlobal = elementToIndex.getValue(fromElement)
        val toGlobal = elementToIndex.getValue(toElement)
        val start = maxOf(if (fromInclusive) fromGlobal else fromGlobal + 1, window.first)
        val endInclusive = minOf(if (toInclusive) toGlobal else toGlobal - 1, window.last)
        return subView(start..endInclusive)
    }

    override fun headSet(toElement: E, inclusive: Boolean): NavigableSet<E> {
        val toGlobal = elementToIndex.getValue(toElement)
        val endInclusive = minOf(if (inclusive) toGlobal else toGlobal - 1, window.last)
        return subView(window.first..endInclusive)
    }

    override fun tailSet(fromElement: E, inclusive: Boolean): NavigableSet<E> {
        val fromGlobal = elementToIndex.getValue(fromElement)
        val start = maxOf(if (inclusive) fromGlobal else fromGlobal + 1, window.first)
        return subView(start..window.last)
    }

    // --- Unmodifiable mutators ---

    override fun add(element: E): Boolean = throw UnsupportedOperationException()
    override fun remove(element: E): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
    override fun removeAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
    override fun retainAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
    override fun clear(): Unit = throw UnsupportedOperationException()

}