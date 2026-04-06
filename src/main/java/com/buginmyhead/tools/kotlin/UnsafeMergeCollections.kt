package com.buginmyhead.tools.kotlin

/**
 * @param original the original [Map]s to back this [UnsafeMergeMap].
 *  The caller must guarantee that
 *  the keys of all maps in [original] are disjoint,
 *  otherwise the behavior of this class is undefined.
 */
class UnsafeMergeMap<K, V>(
    private vararg val original: Map<K, V>
) : Map<K, V> {

    override val size: Int
        get() = original.sumOf(Map<K, V>::size)

    override val keys: Set<K> =
        UnsafeMergeSet(*original.map(Map<K, V>::keys).toTypedArray())

    override val values: Collection<V> =
        UnsafeMergeCollection(*original.map(Map<K, V>::values).toTypedArray())

    override val entries: Set<Map.Entry<K, V>> =
        UnsafeMergeSet(*original.map(Map<K, V>::entries).toTypedArray())

    override fun isEmpty(): Boolean = original.all(Map<K, V>::isEmpty)

    override fun containsKey(key: K): Boolean = original.any { key in it }

    override fun containsValue(value: V): Boolean = original.any { value in it.values }

    override fun get(key: K): V? = original.firstNotNullOfOrNull { it[key] }

}

/**
 * @param original the original [Set]s to back this [UnsafeMergeSet].
 *  The caller must guarantee that
 *  the keys of all maps in [original] are disjoint,
 *  otherwise the behavior of this class is undefined.
 */
class UnsafeMergeSet<T>(
    vararg original: Set<T>
) : Set<T>, Collection<T> by UnsafeMergeCollection(*original)

/**
 * @param original the original [Collection]s to back this [UnsafeMergeCollection].
 *  The caller must guarantee that
 *  the keys of all maps in [original] are disjoint,
 *  otherwise the behavior of this class is undefined.
 */
class UnsafeMergeCollection<T>(
    private vararg val original: Collection<T>
) : Collection<T> {

    override val size: Int get() = original.sumOf(Collection<T>::size)

    override fun isEmpty(): Boolean = original.all(Collection<T>::isEmpty)

    override fun contains(element: T): Boolean = original.any { element in it }

    override fun containsAll(elements: Collection<T>): Boolean = elements.all(::contains)

    override fun iterator() : Iterator<T> = original.asSequence().flatten().iterator()

}