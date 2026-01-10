package com.buginmyhead.tools.kotlin

/**
 * Breadth-first search traversal.
 *
 * Nodes already visited will not be visited again.
 *
 * @param roots the root nodes to start the traversal from.
 * @param flatten a function that returns the children of a node.
 */
fun <T> bfs(
    roots: Iterable<T>,
    flatten: (T) -> Iterable<T>
): Sequence<T> =
    bfs(true, roots, flatten)

/**
 * Breadth-first search traversal.
 *
 * @param cycleSafe if true, nodes already visited will not be visited again.
 * @param roots the root nodes to start the traversal from.
 * @param flatten a function that returns the children of a node.
 */
fun <T> bfs(
    cycleSafe: Boolean,
    roots: Iterable<T>,
    flatten: (T) -> Iterable<T>
): Sequence<T> =
    bfs(cycleSafe, roots.asSequence()) {
        yieldAll(flatten(it))
    }

/**
 * Breadth-first search traversal.
 *
 * Nodes already visited will not be visited again.
 *
 * @param roots the root nodes to start the traversal from.
 * @param flatten a suspend function that yields the children of a node.
 */
fun <T> bfs(
    roots: Sequence<T>,
    flatten: suspend SequenceScope<T>.(T) -> Unit
): Sequence<T> =
    bfs(true, roots, flatten)

/**
 * Breadth-first search traversal.
 *
 * @param cycleSafe if true, nodes already visited will not be visited again.
 * @param roots the root nodes to start the traversal from.
 * @param flatten a suspend function that yields the children of a node.
 */
fun <T> bfs(
    cycleSafe: Boolean,
    roots: Sequence<T>,
    flatten: suspend SequenceScope<T>.(T) -> Unit
): Sequence<T> = sequence {
    val visited = if (cycleSafe) mutableSetOf<T>() else fakeMutableSet()
    val q = ArrayDeque<T>()
    roots.filter(visited::add).forEach(q::addLast)

    while (q.isNotEmpty()) {
        val node = q.removeFirst()
        yield(node)
        sequence { flatten(node) }.filter(visited::add).forEach(q::addLast)
    }
}