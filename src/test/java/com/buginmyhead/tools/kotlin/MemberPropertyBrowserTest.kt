package com.buginmyhead.tools.kotlin

import com.buginmyhead.tools.kotlin.statemachine.StateMachine
import com.buginmyhead.tools.kotlin.statemachine.collectionPropertyValues
import com.buginmyhead.tools.kotlin.statemachine.fieldPropertyValues
import com.buginmyhead.tools.kotlin.statemachine.mapPropertyValues
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

internal class MemberPropertyBrowserTest : FreeSpec({
    val root = Node(
        value = 13,
        child = Node(
            value = 17,
            children = listOf(
                Node(value = 19),
                Node(value = 23),
            ),
            stringToChild = mapOf(
                "twenty-nine" to Node(value = 29),
                "thirty-one" to Node(value = 31),
            )
        )
    )

    "fieldPropertyValues collects all values of field properties" {
        root.fieldPropertyValues() shouldContainAll listOf(root.child)
        root.child!!.fieldPropertyValues() shouldBe emptyList()
    }

    "collectionPropertyValues collects all values of member properties" {
        root.collectionPropertyValues() shouldBe emptyList()
        root.child!!.collectionPropertyValues() shouldContainAll root.child.children
    }

    "mapPropertyValues collects all values of map member properties" {
        root.mapPropertyValues() shouldBe emptyList()
        root.child!!.mapPropertyValues() shouldContainAll root.child.stringToChild.values
    }

}) {

    @StateMachine.State
    data class Node(
        val value: Int,
        val child: Node? = null,
        val children: List<Node> = emptyList(),
        val stringToChild: Map<String, Node> = emptyMap(),
    )

}