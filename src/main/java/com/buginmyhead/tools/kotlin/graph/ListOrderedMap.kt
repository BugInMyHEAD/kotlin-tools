package com.buginmyhead.tools.kotlin.graph

import java.util.NavigableSet
import java.util.SortedSet

/**
 * An unmodifiable [Map] backed by a list and an index map, where key ordering
 * is defined by the original insertion order.
 *
 * Supports O(1) [get], [containsKey], [keyAt], and [globalIndexOf].
 * Sub-views created via [subView] share the same backing data and are O(1) to create.
 *
 * The [keys] property returns a [NavigableSet] view over the key range.
 */
internal class ListOrderedMap<K, V> private constructor(
    private val list: List<K>,
    private val indexMap: Map<K, Int>,
    private val fromIndex: Int,
    private val toIndex: Int,
    private val getValue: (K) -> V,
) : Map<K, V> {

    /**
     * Creates a [ListOrderedMap] from the given key list and value function.
     * Keys must be unique; duplicates result in undefined behavior.
     */
    constructor(elements: List<K>, getValue: (K) -> V) : this(
        elements,
        HashMap<K, Int>(elements.size).also { map ->
            for (i in elements.indices) map[elements[i]] = i
        },
        0,
        elements.size,
        getValue,
    )

    companion object {

        /** Creates a key-only map (values are [Unit]). */
        fun <K> ofKeys(elements: List<K>): ListOrderedMap<K, Unit> =
            ListOrderedMap(elements) { }

    }

    // --- Key access ---

    /**
     * Returns the key at the given position within this view (0-based offset).
     * @throws IndexOutOfBoundsException if [offset] is out of range.
     */
    fun keyAt(offset: Int): K {
        if (offset < 0 || offset >= size) throw IndexOutOfBoundsException("offset=$offset, size=$size")
        return list[fromIndex + offset]
    }

    /**
     * Returns the global index of [key] in the backing list, or -1 if not present
     * in the backing list (regardless of this view's range).
     */
    fun globalIndexOf(key: K): Int = indexMap[key] ?: -1

    // --- Sub-view creation ---

    /** Creates a sub-view for the given global index range \[from, to). O(1) operation. */
    fun subView(from: Int, to: Int): ListOrderedMap<K, V> =
        ListOrderedMap(list, indexMap, from, to, getValue)

    /** Creates a map sharing the same key range but with a different value function. O(1) operation. */
    fun <V2> withValues(newGetValue: (K) -> V2): ListOrderedMap<K, V2> =
        ListOrderedMap(list, indexMap, fromIndex, toIndex, newGetValue)

    // --- Map implementation ---

    override val size: Int get() = toIndex - fromIndex

    override fun isEmpty(): Boolean = fromIndex >= toIndex

    override fun containsKey(key: K): Boolean {
        val idx = indexMap[key] ?: return false
        return idx in fromIndex until toIndex
    }

    override fun containsValue(value: V): Boolean =
        (fromIndex until toIndex).any { getValue(list[it]) == value }

    override fun get(key: K): V? {
        val idx = indexMap[key] ?: return null
        return if (idx in fromIndex until toIndex) getValue(key) else null
    }

    override val keys: NavigableSet<K> = KeySet()

    override val values: Collection<V>
        get() = (fromIndex until toIndex).map { getValue(list[it]) }

    override val entries: Set<Map.Entry<K, V>>
        get() = (fromIndex until toIndex).mapTo(linkedSetOf()) { i ->
            Entry(list[i], getValue(list[i]))
        }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Map<*, *>) return false
        if (other.size != size) return false
        return (fromIndex until toIndex).all { i ->
            val key = list[i]
            other[key] == getValue(key)
        }
    }

    override fun hashCode(): Int = (fromIndex until toIndex).sumOf { i ->
        val key = list[i]
        val value = getValue(key)
        (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
    }

    override fun toString(): String =
        (fromIndex until toIndex).joinToString(", ", "{", "}") { i ->
            val key = list[i]
            "$key=${getValue(key)}"
        }

    private class Entry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V> {
        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> && key == other.key && value == other.value

        override fun hashCode(): Int =
            (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)

        override fun toString(): String = "$key=$value"
    }

    // --- NavigableSet keys ---

    inner class KeySet : AbstractSet<K>(), NavigableSet<K> {

        override val size: Int get() = this@ListOrderedMap.size

        override fun contains(element: K): Boolean = this@ListOrderedMap.containsKey(element)

        override fun iterator(): MutableIterator<K> = object : MutableIterator<K> {
            private var cursor = fromIndex
            override fun hasNext(): Boolean = cursor < toIndex
            override fun next(): K {
                if (!hasNext()) throw NoSuchElementException()
                return list[cursor++]
            }

            override fun remove(): Unit = throw UnsupportedOperationException()
        }

        // Unmodifiable
        override fun add(element: K): Boolean = throw UnsupportedOperationException()
        override fun remove(element: K): Boolean = throw UnsupportedOperationException()
        override fun addAll(elements: Collection<K>): Boolean = throw UnsupportedOperationException()
        override fun removeAll(elements: Collection<K>): Boolean = throw UnsupportedOperationException()
        override fun retainAll(elements: Collection<K>): Boolean = throw UnsupportedOperationException()
        override fun clear(): Unit = throw UnsupportedOperationException()

        // --- SortedSet ---

        private fun checkedGlobalIndex(e: K): Int =
            indexMap[e] ?: throw ClassCastException("Element is not in the backing collection.")

        override fun comparator(): Comparator<in K> =
            Comparator.comparingInt<K> { checkedGlobalIndex(it) }

        override fun first(): K {
            if (isEmpty()) throw NoSuchElementException()
            return list[fromIndex]
        }

        override fun last(): K {
            if (isEmpty()) throw NoSuchElementException()
            return list[toIndex - 1]
        }

        // --- NavigableSet ---

        override fun lower(e: K): K? {
            val g = checkedGlobalIndex(e)
            val idx = minOf(g, toIndex) - 1
            return if (idx >= fromIndex) list[idx] else null
        }

        override fun floor(e: K): K? {
            val g = checkedGlobalIndex(e)
            val idx = minOf(g, toIndex - 1)
            return if (idx >= fromIndex) list[idx] else null
        }

        override fun ceiling(e: K): K? {
            val g = checkedGlobalIndex(e)
            val idx = maxOf(g, fromIndex)
            return if (idx < toIndex) list[idx] else null
        }

        override fun higher(e: K): K? {
            val g = checkedGlobalIndex(e)
            val idx = maxOf(g + 1, fromIndex)
            return if (idx < toIndex) list[idx] else null
        }

        override fun pollFirst(): K = throw UnsupportedOperationException()
        override fun pollLast(): K = throw UnsupportedOperationException()

        override fun descendingIterator(): MutableIterator<K> = object : MutableIterator<K> {
            private var cursor = toIndex - 1
            override fun hasNext(): Boolean = cursor >= fromIndex
            override fun next(): K {
                if (!hasNext()) throw NoSuchElementException()
                return list[cursor--]
            }

            override fun remove(): Unit = throw UnsupportedOperationException()
        }

        override fun descendingSet(): NavigableSet<K> = throw UnsupportedOperationException()

        override fun subSet(
            fromElement: K, fromInclusive: Boolean,
            toElement: K, toInclusive: Boolean,
        ): NavigableSet<K> {
            val fromGlobal = checkedGlobalIndex(fromElement)
            val toGlobal = checkedGlobalIndex(toElement)
            val start = maxOf(if (fromInclusive) fromGlobal else fromGlobal + 1, fromIndex)
            val end = minOf(if (toInclusive) toGlobal + 1 else toGlobal, toIndex)
            return this@ListOrderedMap.subView(start, maxOf(start, end)).keys
        }

        override fun headSet(toElement: K, inclusive: Boolean): NavigableSet<K> {
            val toGlobal = checkedGlobalIndex(toElement)
            val end = minOf(if (inclusive) toGlobal + 1 else toGlobal, toIndex)
            return this@ListOrderedMap.subView(fromIndex, maxOf(fromIndex, end)).keys
        }

        override fun tailSet(fromElement: K, inclusive: Boolean): NavigableSet<K> {
            val fromGlobal = checkedGlobalIndex(fromElement)
            val start = maxOf(if (inclusive) fromGlobal else fromGlobal + 1, fromIndex)
            return this@ListOrderedMap.subView(start, toIndex).keys
        }

        override fun subSet(fromElement: K, toElement: K): SortedSet<K> =
            subSet(fromElement, true, toElement, false)

        override fun headSet(toElement: K): SortedSet<K> =
            headSet(toElement, false)

        override fun tailSet(fromElement: K): SortedSet<K> =
            tailSet(fromElement, true)

    }

}