package com.buginmyhead.tools.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

internal class MemberPropertyBrowserTest : FreeSpec({
    val root = Node(
        value = 5,
        child = Node(
            value = 7,
            children = listOf(
                Node(value = 11),
                Node(value = 13),
            ),
            stringToChild = mapOf(
                "seventeen" to Node(value = 17),
                "nineteen" to Node(value = 19),
            )
        )
    )

    "fieldPropertyValues collects all values of field properties" {
        root.fieldPropertyValues().map(Node::value) shouldContainAll listOf(7)
        root.child!!.fieldPropertyValues() shouldBe emptyList()
    }

    "collectionPropertyValues collects all values of member properties" {
        root.collectionPropertyValues() shouldBe emptyList()
        root.child!!.collectionPropertyValues().map(Node::value) shouldContainAll listOf(11, 13)
    }

    "mapPropertyValues collects all values of map member properties" {
        root.mapPropertyValues() shouldBe emptyList()
        root.child!!.mapPropertyValues().map(Node::value) shouldContainAll listOf(17, 19)
    }

}) {

    data class Node(
        val value: Int,
        val child: Node? = null,
        val children: List<Node> = emptyList(),
        val stringToChild: Map<String, Node> = emptyMap(),
    )

}