@file:JvmName("MemberPropertyBrowser")

package com.buginmyhead.tools.kotlin

import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

inline fun <reified T> T.fieldPropertyValues(): Collection<T> =
    this::class.memberProperties
        .filter { it.returnType.jvmErasure.isSubclassOf(T::class) }
        .filterIsInstance<KProperty1<T, T?>>()
        .mapNotNull { it.get(this) }

inline fun <reified T> T.collectionPropertyValues(): Collection<T> =
    this::class.memberProperties
        .filter {
            it.returnType.isSubtypeOf(
                Collection::class.createType(
                    arguments = listOf(
                        KTypeProjection(
                            KVariance.OUT,
                            T::class.starProjectedType
                        ),
                    ),
                    nullable = false,
                )
            )
        }
        .filterIsInstance<KProperty1<T, Collection<T>>>()
        .flatMap { it.get(this) }

inline fun <reified T> T.mapPropertyValues(): Collection<T> =
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
                            T::class.starProjectedType
                        ),
                    ),
                    nullable = false,
                )
            )
        }
        .filterIsInstance<KProperty1<T, Map<Any, T>>>()
        .flatMap { it.get(this).values }