package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Header search trigger: bold orange U-shaped hanging pocket button
 * attached flush to the top edge of the header.
 */
@Composable
fun HeaderPocketSearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pocketColor: Color = Color(0xFFF97316),
    iconTint: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pocketShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 22.dp,
        bottomEnd = 22.dp
    )

    Box(
        modifier = modifier
            .width(44.dp)
            .height(42.dp)
            .shadow(4.dp, pocketShape)
            .clip(pocketShape)
            .background(color = pocketColor, shape = pocketShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                role = Role.Button,
                onClick = onClick
            )
            .testTag("header_search_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Cari Nasabah",
            tint = iconTint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp)
        )
    }
}

/**
 * Expanded full-width header search bar matching Reference Image 1:
 * Light rounded bar with a prominent hanging U-shaped accent pocket tab on the right.
 */
@Composable
fun HeaderPocketSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    pillColor: Color = Color(0xFFFFFBF7),
    pocketColor: Color = Color(0xFFF97316),
    iconTint: Color = Color.White,
    textColor: Color = Color(0xFF001C3B),
    hintColor: Color = Color(0xFF64748B)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
        ) {
            // Pill body anchored slightly below top so pocket tab protrudes upward cleanly
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(34.dp)
                    .shadow(3.dp, RoundedCornerShape(10.dp))
                    .background(pillColor, RoundedCornerShape(10.dp))
                    .padding(start = 12.dp, end = 52.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF005FAF)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("header_search_input"),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Cari nama, no. rek, CIF, atau mantri...",
                                        color = hintColor,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Bersihkan pencarian",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Hanging U-shaped accent pocket tab on the right (Reference Image 1)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 1.dp)
                    .width(36.dp)
                    .height(30.dp)
                    .shadow(4.dp, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                    .background(
                        color = pocketColor,
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 20.dp, color = Color.White),
                        onClick = { focusRequester.requestFocus() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Cari",
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Close button to collapse header search back to normal header
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    role = Role.Button,
                    onClick = onClose
                )
                .testTag("header_search_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Tutup Pencarian",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Table search bar inspired by Reference Image 2:
 * Stadium-rounded light bar with a protruding circular accent badge on the left
 * connected seamlessly via a liquid bottom-left teardrop fillet.
 */
@Composable
fun TableLiquidSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    pillColor: Color = Color.White,
    pillBorderColor: Color = Color(0xFFFFCCB0),
    accentColor: Color = Color(0xFF005FAF),
    activeAccentColor: Color = Color(0xFFF97316),
    iconTint: Color = Color.White,
    textColor: Color = Color(0xFF001C3B),
    hintColor: Color = Color(0xFF64748B)
) {
    var isFocused by remember { mutableStateOf(false) }
    val isActive = isFocused || query.isNotEmpty()

    val currentAccent by animateColorAsState(
        targetValue = if (isFocused) activeAccentColor else accentColor,
        animationSpec = tween(durationMillis = 220),
        label = "table_search_accent"
    )
    val badgeScale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 400f),
        label = "table_search_badge_scale"
    )

    val totalHeight = 40.dp
    val topProtrusion = 6.dp

    Box(
        modifier = modifier.height(totalHeight),
        contentAlignment = Alignment.BottomStart
    ) {
        // Vector silhouette: Stadium pill + protruding circle + liquid bottom-left teardrop fillet
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val circleRadius = height / 2f
            val cx = circleRadius
            val cy = circleRadius

            val protrusionPx = topProtrusion.toPx()
            val pillHeight = height - protrusionPx
            val pillRadius = pillHeight / 2f
            val pillLeft = cx * 0.55f

            // 1. Soft shadow beneath pill bar
            drawRoundRect(
                color = Color(0xFF001C3B).copy(alpha = 0.06f),
                topLeft = Offset(pillLeft, protrusionPx + 1.5.dp.toPx()),
                size = Size((width - pillLeft).coerceAtLeast(0f), pillHeight),
                cornerRadius = CornerRadius(pillRadius, pillRadius)
            )

            // 2. Main stadium pill bar
            drawRoundRect(
                color = pillColor,
                topLeft = Offset(pillLeft, protrusionPx),
                size = Size((width - pillLeft).coerceAtLeast(0f), pillHeight),
                cornerRadius = CornerRadius(pillRadius, pillRadius)
            )
            drawRoundRect(
                color = if (isActive) currentAccent.copy(alpha = 0.45f) else pillBorderColor,
                topLeft = Offset(pillLeft, protrusionPx),
                size = Size((width - pillLeft).coerceAtLeast(0f), pillHeight),
                cornerRadius = CornerRadius(pillRadius, pillRadius),
                style = Stroke(width = 0.9.dp.toPx())
            )

            // 3. Left circular badge with C1-tangent liquid bottom-left fillet
            val joinAngleDeg = 22f
            val joinRad = Math.toRadians(joinAngleDeg.toDouble())
            val cosJ = cos(joinRad).toFloat() // ~0.927
            val sinJ = sin(joinRad).toFloat() // ~0.375

            val joinX = cx + circleRadius * cosJ
            val joinY = cy + circleRadius * sinJ

            val liquidPath = Path().apply {
                arcTo(
                    rect = Rect(
                        left = cx - circleRadius,
                        top = cy - circleRadius,
                        right = cx + circleRadius,
                        bottom = cy + circleRadius
                    ),
                    startAngleDegrees = joinAngleDeg,
                    sweepAngleDegrees = -(joinAngleDeg + 270f), // Ends at 90° (bottom of circle: cx, height)
                    forceMoveTo = true
                )
                // Extend along bottom edge and sweep smoothly back into circle at joinAngleDeg
                val tailEndX = cx + circleRadius * 1.24f
                lineTo(tailEndX, height)
                cubicTo(
                    cx + circleRadius * 0.74f, height,
                    joinX - circleRadius * 0.28f * sinJ, joinY + circleRadius * 0.28f * cosJ,
                    joinX, joinY
                )
                close()
            }

            // Subtle ambient shadow behind the liquid circle badge
            drawCircle(
                color = currentAccent.copy(alpha = 0.20f),
                radius = circleRadius,
                center = Offset(cx, cy + 1.5.dp.toPx())
            )

            drawPath(
                path = liquidPath,
                color = currentAccent
            )
        }

        // Left circular search icon centered inside the circular badge
        Box(
            modifier = Modifier
                .size(totalHeight)
                .scale(badgeScale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Icon Pencarian",
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }

        // Text field + clear button inside the stadium pill body
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight - topProtrusion)
                .padding(start = totalHeight + 8.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                ),
                singleLine = true,
                cursorBrush = SolidColor(currentAccent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* keep live filtering */ }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .testTag("search_field"),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = hintColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Bersihkan pencarian",
                        tint = Color(0xFF43474E),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
