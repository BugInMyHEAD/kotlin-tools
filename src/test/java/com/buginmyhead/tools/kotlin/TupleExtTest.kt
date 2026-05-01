package com.buginmyhead.tools.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

internal class TupleExtTest : FreeSpec({

    "swap returns a new pair with the first and second values swapped" {
        val pair = 0 to "one"
        val swapped = pair.swap()

        swapped.first shouldBe "one"
        swapped.second shouldBe 0
    }

})