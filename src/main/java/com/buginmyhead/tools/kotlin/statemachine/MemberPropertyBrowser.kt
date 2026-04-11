@file:JvmName("MemberPropertyBrowser")

package com.buginmyhead.tools.kotlin.statemachine

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
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

inline fun <reified T : Any> T.mapPropertyValues(): Collection<T> =
    mapPropertyValues(T::class)

fun <T : Any> T.mapPropertyValues(kClass: KClass<T>): Collection<T> =
    this::class.memberProperties
        .filter {
            it.returnType.isSubtypeOf(
                Map::class.createType(
                    arguments = listOf(
                        KTypeProjection(
                            null,
                            null
                        ),
                        KTypeProjection(
                            KVariance.OUT,
                            kClass.starProjectedType
                        ),
                    ),
                    nullable = false,
                )
            )
        }
        .filterIsInstance<KProperty1<T, Map<Any, T>>>()
        .flatMap { it.get(this).values }