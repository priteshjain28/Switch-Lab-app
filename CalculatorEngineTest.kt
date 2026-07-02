package com.offerverse.switchlab

import com.offerverse.switchlab.model.CalculatorEngine
import com.offerverse.switchlab.model.CompensationInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {
    @Test
    fun defaultAmazonToCapOneNumbersMatchExpectedScenario() {
        val result = CalculatorEngine.analyze(CompensationInput())

        assertEquals(399468.13, result.stayTotalNet, 1.0)
        assertEquals(105579.19, result.switchBeforeNewCompanyNet, 1.0)
        assertEquals(139947.12, result.breakEvenBase, 1.0)

        val base140 = result.scenarios.first { it.baseSalary == 140000.0 }
        assertEquals(399579.19, base140.totalNet, 1.0)
        assertTrue(base140.deltaVsStay > 0.0)
    }

    @Test
    fun salaryYearsTreatAnniversaryAsFullYear() {
        assertEquals(
            4.0,
            CalculatorEngine.salaryYears(
                java.time.LocalDate.of(2025, 10, 20),
                java.time.LocalDate.of(2029, 10, 20)
            ),
            0.0001
        )
    }
}
