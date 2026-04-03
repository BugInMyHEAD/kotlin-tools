package com.buginmyhead.tools.kotlin

/**
 * Excludes annotated declarations from test coverage verification.
 *
 * Typically used for `inline` properties and functions whose getter/body bytecode
 * is never invoked at runtime because the Kotlin compiler inlines them at call sites.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION)
internal annotation class IgnoreTestCoverage