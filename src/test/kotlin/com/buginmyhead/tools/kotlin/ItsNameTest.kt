package com.buginmyhead.tools.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

internal class ItsNameTest : FreeSpec({
    "ItsName lets the property return its name" {
        val a by ItsName
        val b by ItsName
        a shouldBe "a"
        b shouldBe "b"
    }
})