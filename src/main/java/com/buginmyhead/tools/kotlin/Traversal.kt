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

/**
 * Depth-first post-order search traversal.
 *
 * @param cycleSafe if true, nodes already visited will not be visited again.
 * @param roots the root nodes to start the traversal from.
 * @param flatten a suspend function that yields the children of a node.
 */
fun <T, R> dfsPost(
    cycleSafe: Boolean,
    roots: Sequence<T>,
    initial: (T) -> R,
    aggregate: (parent: R, child: R) -> R,
    flatten: suspend SequenceScope<T>.(T) -> Unit
): Sequence<DfsPostContext<T, R>> = sequence {
    val visited = if (cycleSafe) mutableSetOf<T>() else fakeMutableSet()
    val path = mutableListOf<T>()
    val stack = ArrayDeque<DfsPostContext<T, R>>()
    roots
        .filter(visited::add)
        .onEach(path::addLast)
        .map {
            DfsPostContext(
                it,
                null,
                initial(it),
                sequence { flatten(it) }.iterator(),
                path,
            )
        }
        .forEach(stack::addLast)

    while (stack.isNotEmpty()) {
        val context = stack.last()
        if (context.iterator.hasNext()) {
            val child = context.iterator.next()
            if (visited.add(child)) {
                path.addLast(child)
                stack.addLast(DfsPostContext(
                    child,
                    context,
                    initial(child),
                    sequence { flatten(child) }.iterator(),
                    path,
                ))
            }
        } else {
            context.parent?.result = aggregate(context.parent.result, context.result)
            yield(context)
            stack.removeLast()
            path.removeLast()
        }
    }
}

class DfsPostContext<T, R> internal constructor(
    val node: T,
    internal val parent: DfsPostContext<T, R>?,
    initial: R,
    internal val iterator: Iterator<T>,
    private val mutablePath: List<T>,
) {

    var result: R = initial
        internal set

    val pathToRoot: Sequence<T> =
        generateSequence(this) { it.parent }
            .map(DfsPostContext<T, R>::node)

}