package dev.neko.kvmeow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neko.kvmeow.hid.HidScancodes

private val DeepIndigo = Color(0xFF2E1A47)
private val SoftLavender = Color(0xFFE6E6FA)
private val DarkSlate = Color(0xFF1E1E2E)
private val AmberGold = Color(0xFFFFFBEB)
private val CardPurple = Color(0xFF3B225C)
private val AccentLilac = Color(0xFFA78BFA)
private val AccentGreen = Color(0xFF98FB98)
private val KawaiiPink = Color(0xFFFFAFCC)

@Composable
fun FullAccountantCalculatorView(
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onClose: () -> Unit,
    onSendScancode: (Byte) -> Unit,
    onSendText: (String) -> Unit,
    savedTape: String,
    onSaveTape: (String) -> Unit,
    savedMemory: Double,
    onSaveMemory: (Double) -> Unit
) {
    var displayValue by remember { mutableStateOf("0") }
    var memoryValue by remember { mutableStateOf(savedMemory) }
    var currentOperator by remember { mutableStateOf<String?>(null) }
    var operandA by remember { mutableStateOf<Double?>(null) }
    var isEnteringNewNumber by remember { mutableStateOf(true) }

    // Running calculation audit tape entries (e.g. "12,450.00 + 450.00 = 12,900.00")
    val tapeEntries = remember {
        mutableStateListOf<String>().apply {
            if (savedTape.isNotBlank() && savedTape != "0") {
                addAll(savedTape.split("\n").filter { it.isNotBlank() })
            }
        }
    }
    val tapeListState = rememberLazyListState()

    LaunchedEffect(tapeEntries.size) {
        if (tapeEntries.isNotEmpty()) {
            tapeListState.animateScrollToItem(tapeEntries.size - 1)
            onSaveTape(tapeEntries.joinToString("\n"))
        }
    }

    LaunchedEffect(memoryValue) {
        onSaveMemory(memoryValue)
    }

    fun commitKeystrokesToHost(text: String) {
        onSendText(text)
    }

    fun pushTapeEntry(entry: String) {
        tapeEntries.add(entry)
        if (tapeEntries.size > 100) tapeEntries.removeAt(0)
    }

    fun computeResult(): Double {
        val a = operandA ?: 0.0
        val b = displayValue.toDoubleOrNull() ?: 0.0
        return when (currentOperator) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b != 0.0) a / b else 0.0
            else -> b
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlate)
            .statusBarsPadding()
            .padding(14.dp)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🧮", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isFullScreen) "Full Accountant Calculator Deck" else "Aggressive 10-Key Pad",
                        color = AmberGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Memory: ${if (memoryValue != 0.0) memoryValue.toString() else "[empty]"}",
                        color = if (memoryValue != 0.0) AccentGreen else AccentLilac,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row {
                IconButton(onClick = onToggleFullScreen) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle Fullscreen",
                        tint = SoftLavender
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = KawaiiPink
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Running Calculation Audit Tape Display
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepIndigo),
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isFullScreen) 1.2f else 0.7f)
                .border(1.5.dp, CardPurple, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text(
                    text = "📜 AUDIT TAPE / RECENT CALCULATIONS",
                    color = AccentLilac,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(
                    state = tapeListState,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    if (tapeEntries.isEmpty()) {
                        item {
                            Text(
                                text = "Ready for number crunching...",
                                color = SoftLavender.copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        items(tapeEntries) { entry ->
                            Text(
                                text = entry,
                                color = SoftLavender,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = CardPurple, thickness = 1.dp)

                // Current Active Entry Readout
                Text(
                    text = displayValue,
                    color = AmberGold,
                    fontSize = if (isFullScreen) 36.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Memory & Quick Operator Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalcButton(label = "MC", color = CardPurple, modifier = Modifier.weight(1f)) {
                memoryValue = 0.0
            }
            CalcButton(label = "MR", color = CardPurple, modifier = Modifier.weight(1f)) {
                displayValue = memoryValue.toString()
                isEnteringNewNumber = true
                commitKeystrokesToHost(displayValue)
            }
            CalcButton(label = "M+", color = CardPurple, modifier = Modifier.weight(1f)) {
                memoryValue += (displayValue.toDoubleOrNull() ?: 0.0)
                pushTapeEntry("M+ (${displayValue}) -> $memoryValue")
            }
            CalcButton(label = "M-", color = CardPurple, modifier = Modifier.weight(1f)) {
                memoryValue -= (displayValue.toDoubleOrNull() ?: 0.0)
                pushTapeEntry("M- (${displayValue}) -> $memoryValue")
            }
            CalcButton(label = "%", color = CardPurple, modifier = Modifier.weight(1f)) {
                val curr = displayValue.toDoubleOrNull() ?: 0.0
                val pct = curr / 100.0
                displayValue = pct.toString()
                commitKeystrokesToHost(displayValue)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Keypad Grid Matrix
        val keyRows = listOf(
            listOf(
                Triple("CLR", HidScancodes.KEY_ESCAPE, KawaiiPink),
                Triple("BS", HidScancodes.KEY_BACKSPACE, KawaiiPink.copy(alpha = 0.7f)),
                Triple("/", HidScancodes.KEY_KP_SLASH, CardPurple),
                Triple("*", HidScancodes.KEY_KP_ASTERISK, CardPurple)
            ),
            listOf(
                Triple("7", HidScancodes.KEY_KP_7, DeepIndigo),
                Triple("8", HidScancodes.KEY_KP_8, DeepIndigo),
                Triple("9", HidScancodes.KEY_KP_9, DeepIndigo),
                Triple("-", HidScancodes.KEY_KP_MINUS, CardPurple)
            ),
            listOf(
                Triple("4", HidScancodes.KEY_KP_4, DeepIndigo),
                Triple("5", HidScancodes.KEY_KP_5, DeepIndigo),
                Triple("6", HidScancodes.KEY_KP_6, DeepIndigo),
                Triple("+", HidScancodes.KEY_KP_PLUS, CardPurple)
            ),
            listOf(
                Triple("1", HidScancodes.KEY_KP_1, DeepIndigo),
                Triple("2", HidScancodes.KEY_KP_2, DeepIndigo),
                Triple("3", HidScancodes.KEY_KP_3, DeepIndigo),
                Triple("⏎", HidScancodes.KEY_KP_ENTER, AccentLilac)
            ),
            listOf(
                Triple("0", HidScancodes.KEY_KP_0, DeepIndigo),
                Triple("00", HidScancodes.KEY_NONE, DeepIndigo),
                Triple("000", HidScancodes.KEY_NONE, DeepIndigo),
                Triple(".", HidScancodes.KEY_KP_DOT, DeepIndigo)
            )
        )

        Column(
            modifier = Modifier.weight(if (isFullScreen) 2.2f else 1.6f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (row in keyRows) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for ((label, scancode, color) in row) {
                        CalcButton(
                            label = label,
                            color = color,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            when (label) {
                                "CLR" -> {
                                    displayValue = "0"
                                    operandA = null
                                    currentOperator = null
                                    isEnteringNewNumber = true
                                    onSendScancode(scancode)
                                }
                                "BS" -> {
                                    displayValue = if (displayValue.length > 1) displayValue.dropLast(1) else "0"
                                    onSendScancode(scancode)
                                }
                                "00" -> {
                                    if (!isEnteringNewNumber && displayValue != "0") {
                                        displayValue += "00"
                                    }
                                    onSendScancode(HidScancodes.KEY_KP_0)
                                    onSendScancode(HidScancodes.KEY_KP_0)
                                }
                                "000" -> {
                                    if (!isEnteringNewNumber && displayValue != "0") {
                                        displayValue += "000"
                                    }
                                    onSendScancode(HidScancodes.KEY_KP_0)
                                    onSendScancode(HidScancodes.KEY_KP_0)
                                    onSendScancode(HidScancodes.KEY_KP_0)
                                }
                                "+", "-", "*", "/" -> {
                                    val currentVal = displayValue.toDoubleOrNull() ?: 0.0
                                    if (operandA != null && currentOperator != null) {
                                        val res = computeResult()
                                        pushTapeEntry("${operandA} $currentOperator $currentVal = $res")
                                        operandA = res
                                        displayValue = res.toString()
                                    } else {
                                        operandA = currentVal
                                    }
                                    currentOperator = label
                                    isEnteringNewNumber = true
                                    onSendScancode(scancode)
                                }
                                "⏎" -> {
                                    if (operandA != null && currentOperator != null) {
                                        val res = computeResult()
                                        pushTapeEntry("${operandA} $currentOperator $displayValue = $res")
                                        displayValue = res.toString()
                                        operandA = null
                                        currentOperator = null
                                        isEnteringNewNumber = true
                                    }
                                    onSendScancode(scancode)
                                }
                                else -> {
                                    // Digit or dot
                                    if (isEnteringNewNumber) {
                                        displayValue = if (label == ".") "0." else label
                                        isEnteringNewNumber = false
                                    } else {
                                        if (label != "." || !displayValue.contains(".")) {
                                            displayValue += label
                                        }
                                    }
                                    if (scancode != HidScancodes.KEY_NONE) {
                                        onSendScancode(scancode)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalcButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = if (color == KawaiiPink) DarkSlate else AmberGold,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
