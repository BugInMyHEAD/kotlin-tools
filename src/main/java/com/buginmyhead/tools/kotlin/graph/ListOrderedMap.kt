package com.buginmyhead.tools.kotlin.graph

/**
 * An unmodifiable [Map] backed by a [ListOrderedSet] for keys and a value-lookup function.
 *
 * Supports O(1) [get], [containsKey], and [size].
 * Sub-views created via [subMapView] share the same backing data and are O(1) to create.
 */
internal class ListOrderedMap<K, V>(
    private val orderedKeys: ListOrderedSet<K>,
    private val getValue: (K) -> V,
) : Map<K, V> {

    /** Creates a sub-view using the given key sub-view. O(1) operation. */
    fun subMapView(keySubView: ListOrderedSet<K>): ListOrderedMap<K, V> =
        ListOrderedMap(keySubView, getValue)

    override val size: Int get() = orderedKeys.size

    override fun isEmpty(): Boolean = orderedKeys.isEmpty()

    override fun containsKey(key: K): Boolean = key in orderedKeys

    override fun containsValue(value: V): Boolean = orderedKeys.any { getValue(it) == value }

    override fun get(key: K): V? = if (key in orderedKeys) getValue(key) else null

    override val keys: Set<K> get() = orderedKeys

    override val values: Collection<V>
        get() = orderedKeys.map(getValue)

    override val entries: Set<Map.Entry<K, V>>
        get() = orderedKeys.mapTo(linkedSetOf()) { key ->
            Entry(key, getValue(key))
        }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Map<*, *>) return false
        if (other.size != size) return false
        return orderedKeys.all { key -> other[key] == getValue(key) }
    }

    override fun hashCode(): Int = orderedKeys.sumOf { key -> key.hashCode() xor getValue(key).hashCode() }

    override fun toString(): String =
        orderedKeys.joinToString(", ", "{", "}") { key -> "$key=${getValue(key)}" }

    private data class Entry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

}