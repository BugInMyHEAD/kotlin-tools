package com.buginmyhead.tools.kotlin.graph

import java.util.NavigableMap
import java.util.NavigableSet
import java.util.SortedMap

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
    private val window: IntRange,
) : NavigableMap<K, V> {

    /**
     * Creates a [NavigableListMap] from the given key-value pair list.
     * Keys must be unique; duplicates result in undefined behavior.
     */
    constructor(elements: List<Pair<K, V>>) : this(
        elements,
        elements.withIndex().associate { it.value.first to it.index },
        elements.indices,
    )

    companion object

    // --- Key access ---

    /**
     * Returns the key at the given position within this view (0-based offset).
     * @throws IndexOutOfBoundsException if [offset] is out of range.
     */
    fun keyAt(offset: Int): K {
        if (offset !in 0 ..< size) throw IndexOutOfBoundsException("offset=$offset, size=$size")
        return list[window.first + offset].first
    }

    /**
     * Returns the global index of [key] in the backing list, or -1 if not present
     * in the backing list (regardless of this view's range).
     */
    fun globalIndexOf(key: K): Int = keyToIndex[key] ?: -1

    // --- Sub-view creation ---

    /** Creates a sub-view for the given global index range. O(1) operation. */
    fun subView(range: IntRange): NavigableListMap<K, V> =
        NavigableListMap(list, keyToIndex, range)

    /** Creates a map sharing the same key range but with a different value function. */
    fun <V2> withValues(newGetValue: (K) -> V2): NavigableListMap<K, V2> =
        NavigableListMap(list.map { (k, _) -> k to newGetValue(k) }, keyToIndex, window)

    // --- Helpers ---

    private fun entryAt(idx: Int): MutableMap.MutableEntry<K, V> =
        list[idx].let { (k, v) -> Entry(k, v) }

    // --- Map implementation ---

    override val size: Int get() = maxOf(0, window.last - window.first + 1)

    override fun isEmpty(): Boolean = window.isEmpty()

    override fun containsKey(key: K): Boolean {
        val idx = keyToIndex[key] ?: return false
        return idx in window
    }

    override fun containsValue(value: V): Boolean =
        window.any { list[it].second == value }

    override fun get(key: K): V? {
        val idx = keyToIndex[key] ?: return null
        return if (idx in window) list[idx].second else null
    }

    override val keys = NavigableListSet(list.map { (k, v) -> k }, keyToIndex, window)

    override val values: MutableCollection<V>
        get() = window.mapTo(mutableListOf()) { list[it].second }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = window.mapTo(linkedSetOf()) { i ->
            list[i].let { (k, v) -> Entry(k, v) }
        }

    // --- Unmodifiable mutators ---

    override fun put(key: K, value: V): V? = throw UnsupportedOperationException()
    override fun remove(key: K): V? = throw UnsupportedOperationException()
    override fun putAll(from: Map<out K, V>) = throw UnsupportedOperationException()
    override fun clear() = throw UnsupportedOperationException()

    // --- SortedMap ---

    override fun comparator(): Comparator<in K> =
        compareBy(keyToIndex::getValue)

    override fun firstKey(): K {
        if (isEmpty()) throw NoSuchElementException()
        return list[window.first].first
    }

    override fun lastKey(): K {
        if (isEmpty()) throw NoSuchElementException()
        return list[window.last].first
    }

    override fun subMap(fromKey: K, toKey: K): SortedMap<K, V> =
        subMap(fromKey, true, toKey, false)

    override fun headMap(toKey: K): SortedMap<K, V> =
        headMap(toKey, false)

    override fun tailMap(fromKey: K): SortedMap<K, V> =
        tailMap(fromKey, true)

    // --- NavigableMap key navigation ---

    override fun lowerKey(key: K): K? {
        val g = keyToIndex.getValue(key)
        val idx = minOf(g - 1, window.last)
        return if (idx in window) list[idx].first else null
    }

    override fun floorKey(key: K): K? {
        val g = keyToIndex.getValue(key)
        val idx = minOf(g, window.last)
        return if (idx in window) list[idx].first else null
    }

    override fun ceilingKey(key: K): K? {
        val g = keyToIndex.getValue(key)
        val idx = maxOf(g, window.first)
        return if (idx in window) list[idx].first else null
    }

    override fun higherKey(key: K): K? {
        val g = keyToIndex.getValue(key)
        val idx = maxOf(g + 1, window.first)
        return if (idx in window) list[idx].first else null
    }

    // --- NavigableMap entry navigation ---

    override fun lowerEntry(key: K): Map.Entry<K, V>? = lowerKey(key)?.let { entryAt(keyToIndex[it]!!) }

    override fun floorEntry(key: K): Map.Entry<K, V>? = floorKey(key)?.let { entryAt(keyToIndex[it]!!) }

    override fun ceilingEntry(key: K): Map.Entry<K, V>? = ceilingKey(key)?.let { entryAt(keyToIndex[it]!!) }

    override fun higherEntry(key: K): Map.Entry<K, V>? = higherKey(key)?.let { entryAt(keyToIndex[it]!!) }

    override fun firstEntry(): Map.Entry<K, V>? =
        if (isEmpty()) null else entryAt(window.first)

    override fun lastEntry(): Map.Entry<K, V>? =
        if (isEmpty()) null else entryAt(window.last)

    override fun pollFirstEntry(): Map.Entry<K, V> = throw UnsupportedOperationException()

    override fun pollLastEntry(): Map.Entry<K, V> = throw UnsupportedOperationException()

    // --- NavigableMap sub-map views ---

    override fun subMap(fromKey: K, fromInclusive: Boolean, toKey: K, toInclusive: Boolean): NavigableMap<K, V> {
        val fromGlobal = keyToIndex.getValue(fromKey)
        val toGlobal = keyToIndex.getValue(toKey)
        val start = maxOf(if (fromInclusive) fromGlobal else fromGlobal + 1, window.first)
        val endInclusive = minOf(if (toInclusive) toGlobal else toGlobal - 1, window.last)
        return subView(start..endInclusive)
    }

    override fun headMap(toKey: K, inclusive: Boolean): NavigableMap<K, V> {
        val toGlobal = keyToIndex.getValue(toKey)
        val endInclusive = minOf(if (inclusive) toGlobal else toGlobal - 1, window.last)
        return subView(window.first..endInclusive)
    }

    override fun tailMap(fromKey: K, inclusive: Boolean): NavigableMap<K, V> {
        val fromGlobal = keyToIndex.getValue(fromKey)
        val start = maxOf(if (inclusive) fromGlobal else fromGlobal + 1, window.first)
        return subView(start..window.last)
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
                        && window.all { i ->
                            val (k, v) = list[i]
                            other[k] == v
                        }
                )

    override fun hashCode(): Int =
        window.sumOf { i ->
            val (k, v) = list[i]
            (k?.hashCode() ?: 0) xor (v?.hashCode() ?: 0)
        }

    override fun toString(): String =
        window.joinToString(", ", "{", "}") { i ->
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

}