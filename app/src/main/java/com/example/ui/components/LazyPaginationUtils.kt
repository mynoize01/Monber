package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Immutable
data class LazyPagedResult<T>(
    val visibleItems: List<T>,
    val totalCount: Int,
    val hasMore: Boolean,
    val isLoadingMore: Boolean
)

/**
 * Memuat daftar item secara bertahap (Lazy Loading / pagination) mengikuti posisi scroll
 * agar rendering ratusan hingga ribuan data nasabah tetap ringan dan responsif.
 */
@Composable
fun <T> rememberLazyPagedItems(
    items: List<T>,
    listState: LazyListState,
    pageSize: Int = 30,
    prefetchDistance: Int = 6,
    resetKey: Any? = Unit
): LazyPagedResult<T> {
    var loadedLimit by remember(items, resetKey) { mutableIntStateOf(pageSize) }
    var isLoadingMore by remember(items, resetKey) { mutableStateOf(false) }

    val totalCount = items.size
    val effectiveLimit = loadedLimit.coerceAtMost(totalCount)
    val hasMore = effectiveLimit < totalCount

    val visibleItems = remember(items, effectiveLimit) {
        if (effectiveLimit >= totalCount) items else items.subList(0, effectiveLimit)
    }

    LaunchedEffect(listState, items, resetKey, effectiveLimit, totalCount) {
        if (effectiveLimit >= totalCount) {
            isLoadingMore = false
            return@LaunchedEffect
        }
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= (effectiveLimit - prefetchDistance).coerceAtLeast(0)
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                isLoadingMore = true
                delay(70L)
                loadedLimit = (loadedLimit + pageSize).coerceAtMost(totalCount)
                isLoadingMore = false
            }
    }

    return LazyPagedResult(
        visibleItems = visibleItems,
        totalCount = totalCount,
        hasMore = hasMore,
        isLoadingMore = isLoadingMore
    )
}

@Composable
fun LazyLoadingFooterRow(
    visibleCount: Int,
    totalCount: Int,
    accentColor: Color = Color(0xFF005FAF),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(15.dp),
            strokeWidth = 2.dp,
            color = accentColor
        )
        Text(
            text = "  Memuat nasabah ($visibleCount / $totalCount)...",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF64748B)
        )
    }
}
