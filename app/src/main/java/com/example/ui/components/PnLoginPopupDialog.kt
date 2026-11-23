package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.ui.theme.BentoOnSurface
import com.example.ui.theme.BentoOutlineVariant
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoSurface

@Composable
fun PnLoginPopupDialog(
    initialPn: String,
    onDismiss: () -> Unit,
    onLogin: (String) -> Unit,
    onLogoffClick: (() -> Unit)? = null
) {
    var pnInput by remember(initialPn) { mutableStateOf(initialPn) }
    var promptFontSize by remember { mutableStateOf(10.5.sp) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 600.dp)
                .padding(6.dp),
            shape = RoundedCornerShape(18.dp),
            color = BentoSurface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 10.dp)
            ) {
                // Header text + White pill input + Left overlapping orange profile badge
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Masukan nomor PN Mantri untuk masuk menu kelolaan Anda",
                            color = BentoPrimary,
                            fontSize = promptFontSize,
                            lineHeight = (promptFontSize.value + 2.5f).sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { result ->
                                if (result.hasVisualOverflow && promptFontSize.value > 7.5f) {
                                    promptFontSize = (promptFontSize.value - 0.4f).sp
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 64.dp, end = 2.dp, bottom = 5.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                    .border(
                                        width = 1.4.dp,
                                        color = BentoPrimary,
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(start = 72.dp, end = 18.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = pnInput,
                                    onValueChange = { pnInput = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { onLogin(pnInput) }
                                    ),
                                    textStyle = TextStyle(
                                        color = BentoOnSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    cursorBrush = SolidColor(BentoPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pn_login_input"),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (pnInput.isEmpty()) {
                                                Text(
                                                    text = "Masukan PN",
                                                    color = Color(0xFF7D8590),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        }

                        // Overlapping orange circle avatar badge on the left end of the pill input
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .offset(y = 3.dp)
                                .size(58.dp)
                                .zIndex(2f)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = CircleShape,
                                    ambientColor = Color.Black.copy(alpha = 0.16f),
                                    spotColor = Color.Black.copy(alpha = 0.16f)
                                )
                                .clip(CircleShape)
                                .background(BentoOutlineVariant)
                                .semantics { contentDescription = "Ikon Login PN" },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(32.dp)) {
                                val strokeWidth = 2.1.dp.toPx()
                                val strokeStyle = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                                val w = size.width
                                val h = size.height

                                // Head circle
                                drawCircle(
                                    color = Color.White,
                                    radius = w * 0.19f,
                                    center = Offset(w * 0.5f, h * 0.31f),
                                    style = strokeStyle
                                )

                                // Shoulder arch with flat baseline
                                val shoulderRect = Rect(
                                    left = w * 0.16f,
                                    top = h * 0.56f,
                                    right = w * 0.84f,
                                    bottom = h * 1.14f
                                )
                                val shoulderPath = Path().apply {
                                    arcTo(
                                        rect = shoulderRect,
                                        startAngleDegrees = 180f,
                                        sweepAngleDegrees = 180f,
                                        forceMoveTo = true
                                    )
                                    close()
                                }
                                drawPath(
                                    path = shoulderPath,
                                    color = Color.White,
                                    style = strokeStyle
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Full-width corporate blue LOGIN pill button
                    Button(
                        onClick = { onLogin(pnInput) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pn_login_submit_button")
                    ) {
                        Text(
                            text = "LOGIN",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom action row: Batal on the left (and Logoff on the right if already logged in)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("pn_login_cancel_button")
                        ) {
                            Text(
                                text = "Batal",
                                color = BentoPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (onLogoffClick != null && initialPn.isNotBlank()) {
                            TextButton(
                                onClick = onLogoffClick,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("pn_login_logoff_button")
                            ) {
                                Text(
                                    text = "Logoff",
                                    color = Color(0xFFD32F2F),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                    }
                }
            }
        }
    }
}
