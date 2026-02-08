package com.buginmyhead.tools.kotlin.graph

class NotATreeException : IllegalStateException {

    constructor(message: String) : super(message)

    constructor(cause: Throwable) : super(cause)

}