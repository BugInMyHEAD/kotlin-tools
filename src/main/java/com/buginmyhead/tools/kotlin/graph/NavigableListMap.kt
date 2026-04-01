package com.buginmyhead.tools.kotlin.graph

import java.util.NavigableMap
import java.util.NavigableSet
import java.util.SortedMap
import java.util.SortedSet

/**
 * An unmodifiable [NavigableMap] backed by a list and an index map, where key ordering
 * is defined by the original insertion order.
 *
 * Supports O(1) [get], [containsKey], [keyAt], and [globalIndexOf].
 * Sub-views created via [subView] share the same backing data and are O(1) to create.
 *
 * The [keys] property returns a [NavigableSet] view over the key range.
 */
internal class NavigableListMap<K, V> private constructor(
    private val list: List<K>,
    private val indexMap: Map<K, Int>,
    private val fromIndex: Int,
    private val toIndex: Int,
    private val getValue: (K) -> V,
) : NavigableMap<K, V> {

    /**
     * Creates a [NavigableListMap] from the given key list and value function.
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
        fun <K> ofKeys(elements: List<K>): NavigableListMap<K, Unit> =
            NavigableListMap(elements) { }

    }

    // --- Key access ---

    /**
     * Returns the key at the given position within this view (0-based offset).
     * @throws IndexOutOfBoundsException if [offset] is out of range.
     */
    fun keyAt(offset: Int): K {
        if (offset !in 0 ..< size) throw IndexOutOfBoundsException("offset=$offset, size=$size")
        return list[fromIndex + offset]
    }

    /**
     * Returns the global index of [key] in the backing list, or -1 if not present
     * in the backing list (regardless of this view's range).
     */
    fun globalIndexOf(key: K): Int = indexMap[key] ?: -1

    // --- Sub-view creation ---

    /** Creates a sub-view for the given global index range \[from, to). O(1) operation. */
    fun subView(from: Int, to: Int): NavigableListMap<K, V> =
        NavigableListMap(list, indexMap, from, to, getValue)

    /** Creates a map sharing the same key range but with a different value function. O(1) operation. */
    fun <V2> withValues(newGetValue: (K) -> V2): NavigableListMap<K, V2> =
        NavigableListMap(list, indexMap, fromIndex, toIndex, newGetValue)

    // --- Helpers ---

    private fun checkedGlobalIndex(key: K): Int =
        indexMap[key] ?: throw ClassCastException("Key is not in the backing collection.")

    private fun entryOf(key: K): MutableMap.MutableEntry<K, V> = Entry(key, getValue(key))

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

    override val values: MutableCollection<V>
        get() = (fromIndex until toIndex).mapTo(mutableListOf()) { getValue(list[it]) }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = (fromIndex until toIndex).mapTo(linkedSetOf()) { i ->
            Entry(list[i], getValue(list[i]))
        }

    // --- Unmodifiable mutators ---

    override fun put(key: K, value: V): V? = throw UnsupportedOperationException()
    override fun remove(key: K): V? = throw UnsupportedOperationException()
    override fun putAll(from: Map<out K, V>) = throw UnsupportedOperationException()
    override fun clear() = throw UnsupportedOperationException()

    // --- SortedMap ---

    override fun comparator(): Comparator<in K> =
        Comparator.comparingInt<K> { checkedGlobalIndex(it) }

    override fun firstKey(): K {
        if (isEmpty()) throw NoSuchElementException()
        return list[fromIndex]
    }

    override fun lastKey(): K {
        if (isEmpty()) throw NoSuchElementException()
        return list[toIndex - 1]
    }

    override fun subMap(fromKey: K, toKey: K): SortedMap<K, V> =
        subMap(fromKey, true, toKey, false)

    override fun headMap(toKey: K): SortedMap<K, V> =
        headMap(toKey, false)

    override fun tailMap(fromKey: K): SortedMap<K, V> =
        tailMap(fromKey, true)

    // --- NavigableMap key navigation ---

    override fun lowerKey(key: K): K? {
        val g = checkedGlobalIndex(key)
        val idx = minOf(g, toIndex) - 1
        return if (idx >= fromIndex) list[idx] else null
    }

    override fun floorKey(key: K): K? {
        val g = checkedGlobalIndex(key)
        val idx = minOf(g, toIndex - 1)
        return if (idx >= fromIndex) list[idx] else null
    }

    override fun ceilingKey(key: K): K? {
        val g = checkedGlobalIndex(key)
        val idx = maxOf(g, fromIndex)
        return if (idx < toIndex) list[idx] else null
    }

    override fun higherKey(key: K): K? {
        val g = checkedGlobalIndex(key)
        val idx = maxOf(g + 1, fromIndex)
        return if (idx < toIndex) list[idx] else null
    }

    // --- NavigableMap entry navigation ---

    override fun lowerEntry(key: K): Map.Entry<K, V>? = lowerKey(key)?.let(::entryOf)

    override fun floorEntry(key: K): Map.Entry<K, V>? = floorKey(key)?.let(::entryOf)

    override fun ceilingEntry(key: K): Map.Entry<K, V>? = ceilingKey(key)?.let(::entryOf)

    override fun higherEntry(key: K): Map.Entry<K, V>? = higherKey(key)?.let(::entryOf)

    override fun firstEntry(): Map.Entry<K, V>? =
        if (isEmpty()) null else entryOf(list[fromIndex])

    override fun lastEntry(): Map.Entry<K, V>? =
        if (isEmpty()) null else entryOf(list[toIndex - 1])

    override fun pollFirstEntry(): Map.Entry<K, V> = throw UnsupportedOperationException()

    override fun pollLastEntry(): Map.Entry<K, V> = throw UnsupportedOperationException()

    // --- NavigableMap sub-map views ---

    override fun subMap(fromKey: K, fromInclusive: Boolean, toKey: K, toInclusive: Boolean): NavigableMap<K, V> {
        val fromGlobal = checkedGlobalIndex(fromKey)
        val toGlobal = checkedGlobalIndex(toKey)
        val start = maxOf(if (fromInclusive) fromGlobal else fromGlobal + 1, fromIndex)
        val end = minOf(if (toInclusive) toGlobal + 1 else toGlobal, toIndex)
        return subView(start, maxOf(start, end))
    }

    override fun headMap(toKey: K, inclusive: Boolean): NavigableMap<K, V> {
        val toGlobal = checkedGlobalIndex(toKey)
        val end = minOf(if (inclusive) toGlobal + 1 else toGlobal, toIndex)
        return subView(fromIndex, maxOf(fromIndex, end))
    }

    override fun tailMap(fromKey: K, inclusive: Boolean): NavigableMap<K, V> {
        val fromGlobal = checkedGlobalIndex(fromKey)
        val start = maxOf(if (inclusive) fromGlobal else fromGlobal + 1, fromIndex)
        return subView(start, toIndex)
    }

    // --- Descending views ---

    override fun descendingMap(): NavigableMap<K, V> = throw UnsupportedOperationException()

    override fun navigableKeySet(): NavigableSet<K> = keys

    override fun descendingKeySet(): NavigableSet<K> = throw UnsupportedOperationException()

    // --- Object overrides ---

    override fun equals(other: Any?): Boolean =
        other === this
                || (
                other is Map<*, *>
                        && size == other.size
                        && keys.all { key -> other[key] == getValue(key) }
                )

    override fun hashCode(): Int =
        keys.sumOf { key -> (key?.hashCode() ?: 0) xor (getValue(key)?.hashCode() ?: 0) }

    override fun toString(): String =
        (fromIndex until toIndex).joinToString(", ", "{", "}") { i ->
            val key = list[i]
            "$key=${getValue(key)}"
        }

    private class Entry<K, V>(override val key: K, override val value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V = throw UnsupportedOperationException()

        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> && key == other.key && value == other.value

        override fun hashCode(): Int =
            (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)

        override fun toString(): String = "$key=$value"
    }

    // --- NavigableSet keys ---

    inner class KeySet : AbstractSet<K>(), NavigableSet<K> {

        override val size: Int get() = this@NavigableListMap.size

        override fun contains(element: K): Boolean = this@NavigableListMap.containsKey(element)

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

        // --- SortedSet (delegates to map) ---

        override fun comparator(): Comparator<in K> = this@NavigableListMap.comparator()

        override fun first(): K = this@NavigableListMap.firstKey()

        override fun last(): K = this@NavigableListMap.lastKey()

        // --- NavigableSet (delegates to map) ---

        override fun lower(e: K): K? = this@NavigableListMap.lowerKey(e)

        override fun floor(e: K): K? = this@NavigableListMap.floorKey(e)

        override fun ceiling(e: K): K? = this@NavigableListMap.ceilingKey(e)

        override fun higher(e: K): K? = this@NavigableListMap.higherKey(e)

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
        ): NavigableSet<K> =
            this@NavigableListMap.subMap(fromElement, fromInclusive, toElement, toInclusive).navigableKeySet()

        override fun headSet(toElement: K, inclusive: Boolean): NavigableSet<K> =
            this@NavigableListMap.headMap(toElement, inclusive).navigableKeySet()

        override fun tailSet(fromElement: K, inclusive: Boolean): NavigableSet<K> =
            this@NavigableListMap.tailMap(fromElement, inclusive).navigableKeySet()

        override fun subSet(fromElement: K, toElement: K): SortedSet<K> =
            subSet(fromElement, true, toElement, false)

        override fun headSet(toElement: K): SortedSet<K> =
            headSet(toElement, false)

        override fun tailSet(fromElement: K): SortedSet<K> =
            tailSet(fromElement, true)

    }

}