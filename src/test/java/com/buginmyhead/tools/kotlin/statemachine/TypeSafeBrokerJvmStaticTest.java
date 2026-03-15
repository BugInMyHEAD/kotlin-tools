package com.buginmyhead.tools.kotlin.statemachine;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

/**
 * Tests the {@code @JvmStatic} entry points on {@link TypeSafeBroker}
 * that are only reachable from Java.
 */
class TypeSafeBrokerJvmStaticTest {

    @Test
    void create_default() {
        assertInstanceOf(TypeSafeBrokerOnWeakIdentityHashMap.class, TypeSafeBroker.create());
    }

    @Test
    void create_withSynchronization() {
        assertInstanceOf(SynchronizedTypeSafeBroker.class, TypeSafeBroker.create(true));
    }

}