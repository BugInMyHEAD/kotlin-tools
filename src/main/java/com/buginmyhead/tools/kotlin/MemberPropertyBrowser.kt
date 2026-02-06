@file:JvmName("MemberPropertyBrowser")

package com.buginmyhead.tools.kotlin

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

inline fun <reified T : Any> T.fieldPropertyValues(): Collection<T> =
    fieldPropertyValues(T::class)

fun <T : Any> T.fieldPropertyValues(kClass: KClass<T>): Collection<T> =
    this::class.memberProperties
        .filter { it.returnType.jvmErasure.isSubclassOf(kClass) }
        .filterIsInstance<KProperty1<T, T?>>()
        .mapNotNull { it.get(this) }

inline fun <reified T : Any> T.collectionPropertyValues(): Collection<T> =
    collectionPropertyValues(T::class)

fun <T : Any> T.collectionPropertyValues(kClass: KClass<T>): Collection<T> =
    this::class.memberProperties
        .filter {
            it.returnType.isSubtypeOf(
                Collection::class.createType(
                    arguments = listOf(
                        KTypeProjection(
                            KVariance.OUT,
                            kClass.starProjectedType
                        ),
                    ),
                    nullable = false,
                )
            )
        }
        .filterIsInstance<KProperty1<T, Collection<T>>>()
        .flatMap { it.get(this) }

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