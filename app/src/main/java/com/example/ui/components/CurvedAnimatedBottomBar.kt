package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.BottomNavItemData
import kotlin.math.abs
import kotlin.math.round

@Composable
fun CurvedAnimatedBottomBar(
    items: List<BottomNavItemData>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    barColor: Color = Color.White,
    activeColor: Color = Color(0xFF005FAF),
    activeIconTint: Color = Color.White,
    inactiveColor: Color = Color(0xFF64748B),
    barHeight: Dp = 62.dp,
    overhang: Dp = 24.dp
) {
    if (items.isEmpty()) return

    val selectedIndex = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val density = LocalDensity.current
    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Keep as State<Float> and read only inside DrawScope & graphicsLayer to avoid frame-by-frame recomposition
    val animatedIndexState = animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = 420f
        ),
        label = "curved_nav_animated_index"
    )

    val overhangPx = with(density) { overhang.roundToPx() }
    val barPath = remember { Path() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val reportedHeight = (placeable.height - overhangPx).coerceAtLeast(0)
                layout(placeable.width, reportedHeight) {
                    placeable.place(0, -overhangPx)
                }
            }
            .height(overhang + barHeight + navBarBottomInset)
    ) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val slotWidthPx = totalWidthPx / items.size
        val slotWidthDp = maxWidth / items.size

        val notchHalfWidthDp = minOf(34.dp, slotWidthDp * 0.44f)
        val circleRadiusDp = minOf(21.dp, notchHalfWidthDp * 0.62f)
        val circleDiameterDp = circleRadiusDp * 2f

        val barTopPx = with(density) { overhang.toPx() }
        val notchHalfWidthPx = with(density) { notchHalfWidthDp.toPx() }
        val notchDepthPx = with(density) { (circleRadiusDp + 4.dp).toPx() }
        val topCornerRadiusPx = with(density) { 14.dp.toPx() }
        val circleRadiusPx = with(density) { circleRadiusDp.toPx() }
        val baseCircleTopYPx = with(density) { (overhang - circleRadiusDp - 1.dp).toPx() }
        val circleFlightDipMaxPx = with(density) { 3.dp.toPx() }

        val shadowStrokeOuter = remember(density) {
            Stroke(width = with(density) { 10.dp.toPx() }, cap = StrokeCap.Round, join = StrokeJoin.Round)
        }
        val shadowStrokeMid = remember(density) {
            Stroke(width = with(density) { 5.dp.toPx() }, cap = StrokeCap.Round, join = StrokeJoin.Round)
        }
        val borderStroke = remember(density) {
            Stroke(width = with(density) { 1.4.dp.toPx() }, cap = StrokeCap.Round, join = StrokeJoin.Round)
        }

        // 1. Curved Bar Background with Smooth Concave Cutout (Draw-phase only, zero recomposition)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val notchCenterX = (animatedIndexState.value + 0.5f) * slotWidthPx

            val xStart = (notchCenterX - notchHalfWidthPx).coerceIn(0f, width)
            val xEnd = (notchCenterX + notchHalfWidthPx).coerceIn(0f, width)
            val effectiveLeftR = minOf(topCornerRadiusPx, xStart).coerceAtLeast(0f)
            val effectiveRightR = minOf(topCornerRadiusPx, width - xEnd).coerceAtLeast(0f)

            barPath.rewind()
            barPath.moveTo(0f, barTopPx + effectiveLeftR)
            if (effectiveLeftR > 0.5f) {
                barPath.quadraticTo(0f, barTopPx, effectiveLeftR, barTopPx)
            } else {
                barPath.lineTo(0f, barTopPx)
            }

            if (xStart > effectiveLeftR) {
                barPath.lineTo(xStart, barTopPx)
            }

            val halfSpanLeft = notchCenterX - xStart
            barPath.cubicTo(
                xStart + halfSpanLeft * 0.35f, barTopPx,
                notchCenterX - halfSpanLeft * 0.65f, barTopPx + notchDepthPx,
                notchCenterX, barTopPx + notchDepthPx
            )

            val halfSpanRight = xEnd - notchCenterX
            barPath.cubicTo(
                notchCenterX + halfSpanRight * 0.65f, barTopPx + notchDepthPx,
                xEnd - halfSpanRight * 0.35f, barTopPx,
                xEnd, barTopPx
            )

            val rightCornerStart = width - effectiveRightR
            if (rightCornerStart > xEnd) {
                barPath.lineTo(rightCornerStart, barTopPx)
            }

            if (effectiveRightR > 0.5f) {
                barPath.quadraticTo(width, barTopPx, width, barTopPx + effectiveRightR)
            } else {
                barPath.lineTo(width, barTopPx)
            }

            barPath.lineTo(width, height)
            barPath.lineTo(0f, height)
            barPath.close()

            drawPath(
                path = barPath,
                color = Color(0xFF001C3B).copy(alpha = 0.025f),
                style = shadowStrokeOuter
            )
            drawPath(
                path = barPath,
                color = Color(0xFF001C3B).copy(alpha = 0.04f),
                style = shadowStrokeMid
            )
            drawPath(
                path = barPath,
                color = Color(0xFFBFD4EC).copy(alpha = 0.65f),
                style = borderStroke
            )
            drawPath(
                path = barPath,
                color = barColor
            )
        }

        // 2. Floating Active Circular Badge (GPU graphicsLayer transforms only, zero recomposition/relayout)
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val animIdx = animatedIndexState.value
                    val notchCenterX = (animIdx + 0.5f) * slotWidthPx
                    val flightFactor = (1f - abs((animIdx - round(animIdx)) * 2f)).coerceIn(0f, 1f)
                    val cScale = 1f - (0.09f * flightFactor)
                    val dipPx = circleFlightDipMaxPx * flightFactor

                    translationX = notchCenterX - circleRadiusPx
                    translationY = baseCircleTopYPx + dipPx
                    scaleX = cScale
                    scaleY = cScale
                }
                .size(circleDiameterDp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = activeColor.copy(alpha = 0.45f),
                    spotColor = activeColor.copy(alpha = 0.55f)
                )
                .background(color = activeColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val activeItem = items[selectedIndex]
            AnimatedContent(
                targetState = activeItem,
                transitionSpec = {
                    scaleIn(
                        initialScale = 0.72f,
                        animationSpec = spring(
                            dampingRatio = 0.78f,
                            stiffness = 500f
                        )
                    ) togetherWith scaleOut(
                        targetScale = 0.72f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 550f
                        )
                    ) using SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> snap() })
                },
                label = "floating_circle_icon"
            ) { currentItem ->
                Icon(
                    imageVector = currentItem.selectedIcon,
                    contentDescription = null,
                    tint = activeIconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 3. Interactive Navigation Slots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = overhang, bottom = navBarBottomInset)
                .height(barHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex

                val iconAlphaState = animateFloatAsState(
                    targetValue = if (isSelected) 0f else 1f,
                    animationSpec = snap(),
                    label = "nav_icon_alpha_${item.id}"
                )
                val iconScaleState = animateFloatAsState(
                    targetValue = if (isSelected) 0.55f else 1f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                    label = "nav_icon_scale_${item.id}"
                )
                val iconOffsetYPxState = animateFloatAsState(
                    targetValue = if (isSelected) with(density) { (-14).dp.toPx() } else 0f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                    label = "nav_icon_offset_${item.id}"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) activeColor else inactiveColor,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "nav_label_color_${item.id}"
                )
                val labelScaleState = animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                    label = "nav_label_scale_${item.id}"
                )

                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(barHeight)
                        .testTag("bottom_nav_${item.id.lowercase()}")
                        .semantics { selected = isSelected }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(
                                bounded = false,
                                radius = 34.dp,
                                color = activeColor
                            ),
                            role = Role.Tab,
                            onClick = { onSelect(item.id) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                translationY = iconOffsetYPxState.value
                                scaleX = iconScaleState.value
                                scaleY = iconScaleState.value
                                alpha = iconAlphaState.value
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.unselectedIcon,
                            contentDescription = item.label,
                            tint = inactiveColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.label,
                        color = labelColor,
                        fontSize = 9.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .graphicsLayer {
                                val s = labelScaleState.value
                                scaleX = s
                                scaleY = s
                            }
                            .padding(horizontal = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
