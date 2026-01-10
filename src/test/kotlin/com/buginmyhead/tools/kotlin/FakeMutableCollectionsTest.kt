package com.buginmyhead.tools.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

internal class FakeMutableCollectionsTest : FreeSpec({
    "fakeMutableIterator works like MutableIterator that has no more elements" {
        fakeMutableIterator<Boolean>().hasNext() shouldBe
                mutableListOf<Boolean>().iterator().hasNext()
        runCatching { fakeMutableIterator<Boolean>().next() }.exceptionOrNull() shouldBe
                runCatching { mutableListOf<Boolean>().iterator().next() }.exceptionOrNull()
        runCatching { fakeMutableIterator<Boolean>().remove() }.exceptionOrNull() shouldBe
                runCatching { mutableListOf<Boolean>().iterator().remove() }.exceptionOrNull()
    }

    "fakeMutableSet works like empty MutableSet" {
        fakeMutableSet<Boolean>().size shouldBe
                mutableSetOf<Boolean>().size
        fakeMutableSet<Boolean>().add(true) shouldBe
                mutableSetOf<Boolean>().add(true)
        fakeMutableSet<Boolean>().contains(true) shouldBe
                mutableSetOf<Boolean>().contains(true)
        fakeMutableSet<Boolean>().remove(true) shouldBe
                mutableSetOf<Boolean>().remove(true)
        fakeMutableSet<Boolean>().iterator().hasNext() shouldBe
                mutableSetOf<Boolean>().iterator().hasNext()
    }

    "fakeMutableMap works like empty MutableMap" {
        fakeMutableMap<Boolean, Boolean>().size shouldBe
                mutableMapOf<Boolean, Boolean>().size
        fakeMutableMap<Boolean, Boolean>().put(true, false) shouldBe
                mutableMapOf<Boolean, Boolean>().put(true, false)
        fakeMutableMap<Boolean, Boolean>().get(true) shouldBe
                mutableMapOf<Boolean, Boolean>().get(true)
        fakeMutableMap<Boolean, Boolean>().remove(true) shouldBe
                mutableMapOf<Boolean, Boolean>().remove(true)
        fakeMutableMap<Boolean, Boolean>().entries.size shouldBe
                mutableMapOf<Boolean, Boolean>().entries.size
    }
})