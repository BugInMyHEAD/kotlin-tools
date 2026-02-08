package com.buginmyhead.tools.kotlin.graph

/**
 * It is a simple wrapper over a [Graph] to mark it as immutable.
 * Making immutability is in charge of the [original]'s implementation.
 */
class ImmutableGraph<N, W>(original: Graph<N, W>) : Graph<N, W> by original