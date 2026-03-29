package com.buginmyhead.tools.kotlin.graph

import java.util.NavigableSet
import java.util.SortedSet

/**
 * A [NavigableSet] backed by a list and an index map, where the ordering
 * is defined by the original insertion order of elements.
 *
 * Supports O(1) [contains], O(1) [get] by position, and O(1) [globalIndexOf] reverse lookup.
 * Sub-views created via [subView] share the same backing data and are O(1) to create.
 *
 * Elements must be unique in the backing list. Duplicate elements result in undefined behavior.
 *
 * The set is unmodifiable: [pollFirst], [pollLast], and [MutableIterator.remove] throw
 * [UnsupportedOperationException].
 */
internal class ListOrderedSet<E> private constructor(
    private val list: List<E>,
    private val indexMap: Map<E, Int>,
    private val fromIndex: Int,
    private val toIndex: Int,
) : AbstractSet<E>(), NavigableSet<E> {

    /**
     * Creates a [ListOrderedSet] from the given elements, preserving their order.
     * Elements must be unique; duplicates result in undefined behavior.
     */
    constructor(elements: List<E>) : this(
        elements,
        HashMap<E, Int>(elements.size).also { map ->
            for (i in elements.indices) map[elements[i]] = i
        },
        0,
        elements.size,
    )

    // --- Indexed access ---

    /**
     * Returns the element at the given position within this view (0-based offset).
     * @throws IndexOutOfBoundsException if [offset] is out of range.
     */
    operator fun get(offset: Int): E {
        if (offset < 0 || offset >= size) throw IndexOutOfBoundsException("offset=$offset, size=$size")
        return list[fromIndex + offset]
    }

    /**
     * Returns the global index of [element] in the backing list, or -1 if not present
     * in the backing list (regardless of this view's range).
     */
    fun globalIndexOf(element: E): Int = indexMap[element] ?: -1

    /**
     * Creates a sub-view for the given global index range \[from, to). O(1) operation.
     * The sub-view shares the same backing data.
     */
    fun subView(from: Int, to: Int): ListOrderedSet<E> =
        ListOrderedSet(list, indexMap, from, to)

    // --- Set (AbstractSet) ---

    override val size: Int get() = toIndex - fromIndex

    override fun contains(element: E): Boolean {
        val idx = indexMap[element] ?: return false
        return idx in fromIndex until toIndex
    }

    override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {
        private var cursor = fromIndex
        override fun hasNext(): Boolean = cursor < toIndex
        override fun next(): E {
            if (!hasNext()) throw NoSuchElementException()
            return list[cursor++]
        }

        override fun remove(): Unit = throw UnsupportedOperationException()
    }

    // Unmodifiable — all mutating operations throw UnsupportedOperationException
    override fun add(element: E): Boolean = throw UnsupportedOperationException()
    override fun remove(element: E): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
    override fun removeAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
    override fun retainAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException()
    override fun clear(): Unit = throw UnsupportedOperationException()

    // --- SortedSet ---

    private fun checkedGlobalIndex(e: E): Int =
        indexMap[e] ?: throw ClassCastException("Element is not in the backing collection.")

    override fun comparator(): Comparator<in E> =
        Comparator.comparingInt<E> { checkedGlobalIndex(it) }

    override fun first(): E {
        if (isEmpty()) throw NoSuchElementException()
        return list[fromIndex]
    }

    override fun last(): E {
        if (isEmpty()) throw NoSuchElementException()
        return list[toIndex - 1]
    }

    // --- NavigableSet ---

    override fun lower(e: E): E? {
        val g = checkedGlobalIndex(e)
        val idx = minOf(g, toIndex) - 1
        return if (idx >= fromIndex) list[idx] else null
    }

    override fun floor(e: E): E? {
        val g = checkedGlobalIndex(e)
        val idx = minOf(g, toIndex - 1)
        return if (idx >= fromIndex) list[idx] else null
    }

    override fun ceiling(e: E): E? {
        val g = checkedGlobalIndex(e)
        val idx = maxOf(g, fromIndex)
        return if (idx < toIndex) list[idx] else null
    }

    override fun higher(e: E): E? {
        val g = checkedGlobalIndex(e)
        val idx = maxOf(g + 1, fromIndex)
        return if (idx < toIndex) list[idx] else null
    }

    override fun pollFirst(): E = throw UnsupportedOperationException()
    override fun pollLast(): E = throw UnsupportedOperationException()

    override fun descendingIterator(): MutableIterator<E> = object : MutableIterator<E> {
        private var cursor = toIndex - 1
        override fun hasNext(): Boolean = cursor >= fromIndex
        override fun next(): E {
            if (!hasNext()) throw NoSuchElementException()
            return list[cursor--]
        }

        override fun remove(): Unit = throw UnsupportedOperationException()
    }

    override fun descendingSet(): NavigableSet<E> = throw UnsupportedOperationException()

    override fun subSet(
        fromElement: E,
        fromInclusive: Boolean,
        toElement: E,
        toInclusive: Boolean,
    ): NavigableSet<E> {
        val fromGlobal = checkedGlobalIndex(fromElement)
        val toGlobal = checkedGlobalIndex(toElement)
        val start = maxOf(if (fromInclusive) fromGlobal else fromGlobal + 1, fromIndex)
        val end = minOf(if (toInclusive) toGlobal + 1 else toGlobal, toIndex)
        return ListOrderedSet(list, indexMap, start, maxOf(start, end))
    }

    override fun headSet(toElement: E, inclusive: Boolean): NavigableSet<E> {
        val toGlobal = checkedGlobalIndex(toElement)
        val end = minOf(if (inclusive) toGlobal + 1 else toGlobal, toIndex)
        return ListOrderedSet(list, indexMap, fromIndex, maxOf(fromIndex, end))
    }

    override fun tailSet(fromElement: E, inclusive: Boolean): NavigableSet<E> {
        val fromGlobal = checkedGlobalIndex(fromElement)
        val start = maxOf(if (inclusive) fromGlobal else fromGlobal + 1, fromIndex)
        return ListOrderedSet(list, indexMap, start, toIndex)
    }

    override fun subSet(fromElement: E, toElement: E): SortedSet<E> =
        subSet(fromElement, true, toElement, false)

    override fun headSet(toElement: E): SortedSet<E> =
        headSet(toElement, false)

    override fun tailSet(fromElement: E): SortedSet<E> =
        tailSet(fromElement, true)

}