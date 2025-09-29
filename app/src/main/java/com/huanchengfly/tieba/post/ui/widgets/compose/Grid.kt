package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastMap

interface GridScope {
    fun item(
        span: Int = 1,
        content: @Composable () -> Unit
    )
}

fun <T> GridScope.items(
    items: List<T>,
    span: (T) -> Int = { 1 },
    content: @Composable (T) -> Unit
) {
    items.forEach {
        item(span(it)) {
            content(it)
        }
    }
}

fun <T> GridScope.itemsIndexed(
    items: List<T>,
    span: (index: Int, item: T) -> Int = { _, _ -> 1 },
    content: @Composable (index: Int, item: T) -> Unit
) {
    items.forEachIndexed { index, item ->
        item(span(index, item)) {
            content(index, item)
        }
    }
}

internal class GridScopeImpl : GridScope {
    class Item(
        val span: Int = 1,
        val content: @Composable () -> Unit
    )

    val items = mutableListOf<Item>()

    override fun item(span: Int, content: @Composable () -> Unit) {
        items.add(Item(span, content))
    }
}

@Stable
class GridCounter(
    private val initialValue: Int
) {
    var mutableValue: Int = initialValue

    val value: Int
        get() = mutableValue

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GridCounter

        if (initialValue != other.initialValue) return false

        return true
    }

    override fun hashCode(): Int {
        return initialValue
    }
}

@Composable
fun VerticalGrid(
    column: Int,
    modifier: Modifier = Modifier,
    rowModifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: GridScope.() -> Unit
) {
    val gridScope = remember(content) { GridScopeImpl().apply(content) }
    val items = gridScope.items
    val movableItems = remember(gridScope) {
        items.fastMap {
            movableContentWithReceiverOf<RowScope> {
                Box(
                    modifier = Modifier.weight(it.span.toFloat()),
                    contentAlignment = Alignment.Center,
                    content = { it.content() }
                )
            }
        }
    }

    val rows = items.size / column + (if (items.size % column == 0) 0 else 1)
    Column(modifier = modifier, verticalArrangement = verticalArrangement) {
        repeat(rows) { row ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = horizontalArrangement,
                modifier = rowModifier
            ) {
                val start = row * column
                val end = minOf(start + column, items.size)
                for (index in start until end) {
                    movableItems[index](this)
                }
            }
        }
    }
}