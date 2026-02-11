package com.buginmyhead.tools.kotlin.graph

class NotATreeException : IllegalArgumentException {

    constructor(message: String) : super(message)

    constructor(cause: Throwable) : super(cause)

}