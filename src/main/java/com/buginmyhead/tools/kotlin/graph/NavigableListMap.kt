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
    private val list: List<Pair<K, V>>,
    private val keyToIndex: Map<K, Int>,
    private val fromIndex: Int,
    private val toIndex: Int,
) : NavigableMap<K, V> {

    /**
     * Creates a [NavigableListMap] from the given key-value pair list.
     * Keys must be unique; duplicates result in undefined behavior.
     */
    constructor(elements: List<Pair<K, V>>) : this(
        elements,
        HashMap<K, Int>(elements.size).also { map ->
            for (i in elements.indices) map[elements[i].first] = i
        },
        0,
        elements.size,
    )

    companion object {

        /** Creates a key-only map (values are [Unit]). */
        fun <K> ofKeys(elements: List<K>): NavigableListMap<K, Unit> =
            NavigableListMap(elements.map { it to Unit })

    }

    // --- Key access ---

    /**
     * Returns the key at the given position within this view (0-based offset).
     * @throws IndexOutOfBoundsException if [offset] is out of range.
     */
    fun keyAt(offset: Int): K {
        if (offset !in 0 ..< size) throw IndexOutOfBoundsException("offset=$offset, size=$size")
        return list[fromIndex + offset].first
    }

    /**
     * Returns the global index of [key] in the backing list, or -1 if not present
     * in the backing list (regardless of this view's range).
     */
    fun globalIndexOf(key: K): Int = keyToIndex[key] ?: -1

    // --- Sub-view creation ---

    /** Creates a sub-view for the given global index range \[from, to). O(1) operation. */
    fun subView(from: Int, to: Int): NavigableListMap<K, V> =
        NavigableListMap(list, keyToIndex, from, to)

    /** Creates a map sharing the same key range but with a different value function. */
    fun <V2> withValues(newGetValue: (K) -> V2): NavigableListMap<K, V2> =
        NavigableListMap(list.map { (k, _) -> k to newGetValue(k) }, keyToIndex, fromIndex, toIndex)

    // --- Helpers ---

    private fun checkedGlobalIndex(key: K): Int =
        keyToIndex[key] ?: throw ClassCastException("Key is not in the backing collection.")

    private fun entryAt(idx: Int): MutableMap.MutableEntry<K, V> =
        list[idx].let { (k, v) -> Entry(k, v) }

    // --- Map implementation ---

    override val size: Int get() = toIndex - fromIndex

    override fun isEmpty(): Boolean = fromIndex >= toIndex

    override fun containsKey(key: K): Boolean {
        val idx = keyToIndex[key] ?: return false
        return idx in fromIndex until toIndex
    }

    override fun containsValue(value: V): Boolean =
        (fromIndex until toIndex).any { list[it].second == value }

    override fun get(key: K): V? {
        val idx = keyToIndex[key] ?: return null
        return if (idx in fromIndex until toIndex) list[idx].second else null
    }

    override val keys: NavigableSet<K> = KeySet()

    override val values: MutableCollection<V>
        get() = (fromIndex until toIndex).mapTo(mutableListOf()) { list[it].second }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = (fromIndex until toIndex).mapTo(linkedSetOf()) { i ->
            list[i].let { (k, v) -> Entry(k, v) }
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
        return list[fromIndex].first
    }

    override fun lastKey(): K {
        if (isEmpty()) throw NoSuchElementException()
        return list[toIndex - 1].first
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
        return if (idx >= fromIndex) list[idx].first else null
    }

    override fun floorKey(key: K): K? {
        val g = checkedGlobalIndex(key)
        val idx = minOf(g, toIndex - 1)
        return if (idx >= fromIndex) list[idx].first else null
    }

    override fun ceilingKey(key: K): K? {
        val g = checkedGlobalIndex(key)
        val idx = maxOf(g, fromIndex)
        return if (idx < toIndex) list[idx].first else null
    }

    override fun higherKey(key: K): K? {
        val g = checkedGlobalIndex(key)
        val idx = maxOf(g + 1, fromIndex)
        return if (idx < toIndex) list[idx].first else null
    }

    // --- NavigableMap entry navigation ---

    override fun lowerEntry(key: K): Map.Entry<K, V>? = lowerKey(key)?.let { entryAt(keyToIndex[it]!!) }

    override fun floorEntry(key: K): Map.Entry<K, V>? = floorKey(key)?.let { entryAt(keyToIndex[it]!!) }

    override fun ceilingEntry(key: K): Map.Entry<K, V>? = ceilingKey(key)?.let { entryAt(keyToIndex[it]!!) }

    override fun higherEntry(key: K): Map.Entry<K, V>? = higherKey(key)?.let { entryAt(keyToIndex[it]!!) }

    override fun firstEntry(): Map.Entry<K, V>? =
        if (isEmpty()) null else entryAt(fromIndex)

    override fun lastEntry(): Map.Entry<K, V>? =
        if (isEmpty()) null else entryAt(toIndex - 1)

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
                        && (fromIndex until toIndex).all { i ->
                            val (k, v) = list[i]
                            other[k] == v
                        }
                )

    override fun hashCode(): Int =
        (fromIndex until toIndex).sumOf { i ->
            val (k, v) = list[i]
            (k?.hashCode() ?: 0) xor (v?.hashCode() ?: 0)
        }

    override fun toString(): String =
        (fromIndex until toIndex).joinToString(", ", "{", "}") { i ->
            "${list[i].first}=${list[i].second}"
        }

    private data class Entry<K, V>(
        override val key: K,
        override val value: V
    ) : MutableMap.MutableEntry<K, V> {

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
                return list[cursor++].first
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
                return list[cursor--].first
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