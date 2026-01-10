package com.buginmyhead.tools.kotlin

/**
 * The methods follow the behavior of [MutableIterator] that has no more elements.
 */
@Suppress("UNCHECKED_CAST")
fun <E> fakeMutableIterator(): MutableIterator<E> = FakeMutableIterator as MutableIterator<E>

/**
 * The methods follow the behavior of empty [MutableSet],
 * but they do not change state of this [MutableSet].
 */
@Suppress("UNCHECKED_CAST")
fun <E> fakeMutableSet(): MutableSet<E> = FakeMutableSet as MutableSet<E>

/**
 * The methods follow the behavior of empty [MutableMap],
 * but they do not change state of this [MutableMap].
 */
@Suppress("UNCHECKED_CAST")
fun <K, V> fakeMutableMap(): MutableMap<K, V> = FakeMutableMap as MutableMap<K, V>

private object FakeMutableIterator : MutableIterator<Any?> {

    override fun hasNext(): Boolean = false

    override fun next() = throw NoSuchElementException()

    override fun remove() = throw IllegalStateException()

}

private object FakeMutableSet : AbstractMutableSet<Any?>() {

    override fun add(element: Any?): Boolean = true

    override fun iterator(): MutableIterator<Any?> = FakeMutableIterator

    override val size: Int get() = 0

}

private object FakeMutableMap : AbstractMutableMap<Any?, Any?>() {

    override fun put(key: Any?, value: Any?): Any? = null

    override fun remove(key: Any?): Any? = null

    override val entries: MutableSet<MutableMap.MutableEntry<Any?, Any?>> get() = fakeMutableSet()

}