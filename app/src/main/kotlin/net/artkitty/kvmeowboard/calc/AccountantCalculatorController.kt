package net.artkitty.kvmeowboard.calc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * 🧮 AccountantCalculatorController 🧮
 * Features:
 * - Running audit calculation tape log
 * - 10-key input ('0'-'9', "00", "000", '.')
 * - Basic operators (+, -, *, /) and Percentage (%)
 * - Memory registers (MC, MR, M+, M-)
 * - +/- Sign inversion, CE/C clear
 * - Direct Bluetooth HID result typing via onSendHid
 */
class AccountantCalculatorController(
    private val onSendHid: (String) -> Unit
) {
    var displayValue by mutableStateOf("0")
        private set

    val tape = mutableStateListOf<String>()

    var memoryRegister by mutableDoubleStateOf(0.0)
        private set

    private var currentOperand: Double? = null
    private var pendingOperator: String? = null
    private var isEnteringNewNumber = true

    fun onDigit(digit: String) {
        if (isEnteringNewNumber || displayValue == "0") {
            displayValue = if (digit == ".") "0." else digit
            isEnteringNewNumber = false
        } else {
            if (digit == "." && displayValue.contains(".")) return
            displayValue += digit
        }
    }

    fun onDoubleZero() {
        if (!isEnteringNewNumber && displayValue != "0") {
            displayValue += "00"
        }
    }

    fun onTripleZero() {
        if (!isEnteringNewNumber && displayValue != "0") {
            displayValue += "000"
        }
    }

    fun onBackspace() {
        if (!isEnteringNewNumber && displayValue.length > 1) {
            displayValue = displayValue.dropLast(1)
        } else {
            displayValue = "0"
            isEnteringNewNumber = true
        }
    }

    fun onClear() {
        displayValue = "0"
        currentOperand = null
        pendingOperator = null
        isEnteringNewNumber = true
    }

    fun onClearTape() {
        tape.clear()
    }

    fun onToggleSign() {
        val d = displayValue.toDoubleOrNull() ?: return
        displayValue = formatNumber(-d)
    }

    fun onPercentage() {
        val d = displayValue.toDoubleOrNull() ?: return
        val res = if (currentOperand != null) {
            currentOperand!! * (d / 100.0)
        } else {
            d / 100.0
        }
        displayValue = formatNumber(res)
        tape.add("%: $displayValue")
    }

    fun onOperator(op: String) {
        val currentVal = displayValue.toDoubleOrNull() ?: 0.0
        if (currentOperand == null) {
            currentOperand = currentVal
        } else if (pendingOperator != null && !isEnteringNewNumber) {
            currentOperand = calculate(currentOperand!!, currentVal, pendingOperator!!)
            displayValue = formatNumber(currentOperand!!)
        }
        pendingOperator = op
        isEnteringNewNumber = true
        tape.add("${formatNumber(currentVal)} $op")
    }

    fun onEquals() {
        val currentVal = displayValue.toDoubleOrNull() ?: 0.0
        if (currentOperand != null && pendingOperator != null) {
            val res = calculate(currentOperand!!, currentVal, pendingOperator!!)
            tape.add("${formatNumber(currentOperand!!)} $pendingOperator ${formatNumber(currentVal)} = ${formatNumber(res)}")
            displayValue = formatNumber(res)
            currentOperand = null
            pendingOperator = null
            isEnteringNewNumber = true
        }
    }

    fun onMemoryClear() {
        memoryRegister = 0.0
        tape.add("MC: 0")
    }

    fun onMemoryRecall() {
        displayValue = formatNumber(memoryRegister)
        isEnteringNewNumber = true
        tape.add("MR: $displayValue")
    }

    fun onMemoryAdd() {
        val d = displayValue.toDoubleOrNull() ?: 0.0
        memoryRegister += d
        tape.add("M+: ${formatNumber(memoryRegister)}")
    }

    fun onMemorySubtract() {
        val d = displayValue.toDoubleOrNull() ?: 0.0
        memoryRegister -= d
        tape.add("M-: ${formatNumber(memoryRegister)}")
    }

    fun sendResultToHid() {
        onSendHid(displayValue)
    }

    private fun calculate(a: Double, b: Double, op: String): Double {
        return when (op) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b != 0.0) a / b else 0.0
            else -> b
        }
    }

    private fun formatNumber(d: Double): String {
        return if (d == d.toLong().toDouble()) {
            d.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", d).trimEnd('0').trimEnd('.')
        }
    }
}
