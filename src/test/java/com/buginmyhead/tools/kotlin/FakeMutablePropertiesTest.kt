package com.buginmyhead.tools.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

internal class FakeMutablePropertiesTest : FreeSpec({
    "fakeMutableProperty0Of always returns fixedValue" {
        var x: Int by (null as Container?)?.run { ::content } ?: fakeMutableProperty0Of<Int>(5)
        x = 7

        x shouldBe 5
    }

    "fakeMutableProperty0By always returns return from getter" {
        var x: Int by (null as Container?)?.run { ::content } ?: fakeMutableProperty0By<Int> { 5 }
        x = 7

        x shouldBe 5
    }
}) {

    private class Container(
        var content: Int
    )

}