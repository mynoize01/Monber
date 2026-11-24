package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanAnnuityCalculatorTest {

    @Test
    fun `annuity calculation returns expected monthly installment for Kupedes 15 percent 24 months`() {
        val result = calculateAnnuityInstallment(
            plafon = 50_000_000.0,
            annualRatePercent = 15.0,
            tenorMonths = 24
        )
        assertTrue(result.isValid)
        // P=50,000,000, r=0.15/12=0.0125, n=24 -> ~2,424,332.35
        assertEquals(2_424_332.35, result.monthlyInstallment, 1.0)
        assertTrue(result.totalInterest > 0.0)
        assertEquals(result.monthlyInstallment * 24.0, result.totalPayment, 0.01)
    }

    @Test
    fun `annuity calculation returns expected monthly installment for KUR 6 percent 12 months`() {
        val result = calculateAnnuityInstallment(
            plafon = 10_000_000.0,
            annualRatePercent = 6.0,
            tenorMonths = 12
        )
        assertTrue(result.isValid)
        // P=10,000,000, r=0.06/12=0.005, n=12 -> ~860,664.30
        assertEquals(860_664.30, result.monthlyInstallment, 1.0)
    }

    @Test
    fun `annuity calculation handles zero percent interest`() {
        val result = calculateAnnuityInstallment(
            plafon = 12_000_000.0,
            annualRatePercent = 0.0,
            tenorMonths = 12
        )
        assertTrue(result.isValid)
        assertEquals(1_000_000.0, result.monthlyInstallment, 0.01)
        assertEquals(0.0, result.totalInterest, 0.01)
    }

    @Test
    fun `annuity calculation rejects invalid plafon or tenor`() {
        assertFalse(calculateAnnuityInstallment(0.0, 15.0, 24).isValid)
        assertFalse(calculateAnnuityInstallment(50_000_000.0, 15.0, 0).isValid)
        assertFalse(calculateAnnuityInstallment(50_000_000.0, -1.0, 12).isValid)
    }
}
