package com.buginmyhead.tools.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

internal class FunctionalTest : FreeSpec({
    "then invokes left-side function firstly, and then right-side function secondly" {
        val add13: (Int) -> Int = { x: Int -> x + 13 }
        val multiply17: (Int) -> Int = { x -> x * 17 }
        val combined = add13 then multiply17

        combined(0) shouldBe 221 // (0 + 13) * 17 = 221
    }

    "compose invokes right-side function firstly, and then left-side function secondly" {
        val add13: (Int) -> Int = { x: Int -> x + 13 }
        val multiply17: (Int) -> Int = { x -> x * 17 }
        val combined = add13 compose multiply17

        combined(0) shouldBe 13 // 13 + (0 * 17) = 13
    }
})