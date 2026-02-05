package com.buginmyhead.tools.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

internal class FunctionalTest : FreeSpec({
    "andThen invokes left-side function firstly, and then right-side function secondly" {
        val add5: (Int) -> Int = { x: Int -> x + 5 }
        val multiply7: (Int) -> Int = { x -> x * 7 }
        val combined = add5 andThen multiply7

        combined(0) shouldBe 35 // (0 + 5) * 7 = 35
    }

    "compose invokes right-side function firstly, and then left-side function secondly" {
        val add5: (Int) -> Int = { x: Int -> x + 5 }
        val multiply7: (Int) -> Int = { x -> x * 7 }
        val combined = add5 compose multiply7

        combined(0) shouldBe 5 // 5 + (0 * 7) = 5
    }
})