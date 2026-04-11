@file:JvmName("MemberPropertyBrowser")

package com.buginmyhead.tools.kotlin.statemachine

import kotlin.reflect.KProperty1
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

@Suppress("UNCHECKED_CAST")
fun Any.fieldPropertyValues(): Collection<Any> =
    this::class.memberProperties
        .filter { it.returnType.jvmErasure.hasAnnotation<StateMachine.State>() }
        .mapNotNull { (it as KProperty1<Any, Any?>).get(this) }

@Suppress("UNCHECKED_CAST")
fun Any.collectionPropertyValues(): Collection<Any> =
    this::class.memberProperties
        .filter {
            it.returnType.jvmErasure.isSubclassOf(Collection::class)
                    && (
                    it.hasAnnotation<StateMachine.State>()
                            || it.returnType.arguments[0].type?.jvmErasure?.hasAnnotation<StateMachine.State>() == true
                    )
        }
        .flatMap { (it as KProperty1<Any, Collection<Any?>>).get(this) }
        .filterNotNull()

@Suppress("UNCHECKED_CAST")
fun Any.mapPropertyValues(): Collection<Any> =
    this::class.memberProperties
        .filter {
            it.returnType.jvmErasure.isSubclassOf(Map::class)
                    && (
                    it.hasAnnotation<StateMachine.State>()
                            || it.returnType.arguments[1].type?.jvmErasure?.hasAnnotation<StateMachine.State>() == true
                    )
        }
        .flatMap { (it as KProperty1<Any, Map<Any, Any?>>).get(this).values }
        .filterNotNull()