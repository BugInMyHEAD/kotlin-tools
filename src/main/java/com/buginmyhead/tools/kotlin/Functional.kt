package com.buginmyhead.tools.kotlin

inline infix fun <P, Q, R> ((P) -> Q).andThen(crossinline later: (Q) -> R): (P) -> R =
    { p -> later(this(p)) }

inline infix fun <P, Q, R> ((Q) -> R).compose(crossinline earlier: (P) -> Q): (P) -> R =
    { p -> this(earlier(p)) }