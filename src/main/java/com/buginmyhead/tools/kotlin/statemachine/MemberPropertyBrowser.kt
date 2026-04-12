@file:JvmName("MemberPropertyBrowser")

package com.buginmyhead.tools.kotlin.statemachine

import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

@Suppress("UNCHECKED_CAST")
fun TypeSafeBroker.Key<*>.fieldPropertyValues(): Collection<TypeSafeBroker.Key<*>> =
    this::class.memberProperties
        .filter { it.returnType.jvmErasure.isSubclassOf(TypeSafeBroker.Key::class) }
        .mapNotNull { (it as KProperty1<TypeSafeBroker.Key<*>, TypeSafeBroker.Key<*>?>).get(this) }

@Suppress("UNCHECKED_CAST")
fun TypeSafeBroker.Key<*>.collectionPropertyValues(): Collection<TypeSafeBroker.Key<*>> =
    this::class.memberProperties
        .filter {
            it.returnType.jvmErasure.isSubclassOf(Collection::class)
                    && it.returnType.arguments[0].type?.jvmErasure?.isSubclassOf(TypeSafeBroker.Key::class) == true
        }
        .flatMap { (it as KProperty1<TypeSafeBroker.Key<*>, Collection<TypeSafeBroker.Key<*>>>).get(this) }

@Suppress("UNCHECKED_CAST")
fun TypeSafeBroker.Key<*>.mapPropertyValues(): Collection<TypeSafeBroker.Key<*>> =
    this::class.memberProperties
        .filter {
            it.returnType.jvmErasure.isSubclassOf(Map::class)
                    && it.returnType.arguments[1].type?.jvmErasure?.isSubclassOf(TypeSafeBroker.Key::class) == true
        }
        .flatMap { (it as KProperty1<TypeSafeBroker.Key<*>, Map<in Any, TypeSafeBroker.Key<*>?>>).get(this).values }
        .filterNotNull()