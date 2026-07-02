package com.offerverse.switchlab.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * Money math engine for OfferVerse SwitchLab.
 *
 * Design choices:
 * - Base salary uses anniversary-year math: 2025-10-20 to 2029-10-20 is exactly 4.0 years,
 *   then extra days are prorated over 365. This mirrors salary offer-letter thinking better than
 *   dividing every elapsed calendar day by 365 through leap years.
 * - Bonus/RSU values are treated as gross values multiplied by take-home rates unless a field is
 *   explicitly entered as net cash, like relocation received or relocation payback.
 * - RSU share price is flat by user input. This is not a stock forecast.
 */
object CalculatorEngine {
    fun analyze(input: CompensationInput): AnalysisResult {
        val sanitized = input.sanitized()
        val stayTotal = currentJobTotalThrough(sanitized, sanitized.endDate)
        val beforeSwitch = currentJobTotalThrough(sanitized, sanitized.switchDate, leavingAt = sanitized.switchDate)
        val newCompanyYears = salaryYears(sanitized.switchDate, sanitized.endDate)

        val scenarios = sanitized.scenarioBases.map { base ->
            val newCompanyNet = base * newCompanyYears * sanitized.newTakeHomeRate + sanitized.newCompanyRelocationNet
            val total = beforeSwitch + newCompanyNet
            SalaryScenario(
                baseSalary = base,
                newCompanySalaryNet = newCompanyNet,
                totalNet = total,
                deltaVsStay = total - stayTotal,
                isWinner = total >= stayTotal
            )
        }

        val selectedSwitchTotal = beforeSwitch +
            sanitized.selectedSwitchBase * newCompanyYears * sanitized.newTakeHomeRate +
            sanitized.newCompanyRelocationNet

        val breakEvenBase = if (newCompanyYears > 0.0 && sanitized.newTakeHomeRate > 0.0) {
            (stayTotal - beforeSwitch - sanitized.newCompanyRelocationNet) / (newCompanyYears * sanitized.newTakeHomeRate)
        } else {
            0.0
        }

        return AnalysisResult(
            input = sanitized,
            stayTotalNet = stayTotal,
            switchBeforeNewCompanyNet = beforeSwitch,
            selectedSwitchTotalNet = selectedSwitchTotal,
            selectedSwitchDeltaVsStay = selectedSwitchTotal - stayTotal,
            breakEvenBase = breakEvenBase,
            currentRsuVestsThroughStay = rsuVestsThrough(sanitized, sanitized.endDate),
            currentRsuVestsBeforeSwitch = rsuVestsThrough(sanitized, sanitized.switchDate),
            scenarios = scenarios,
            cashTimeline = cashTimeline(sanitized, sanitized.selectedSwitchBase)
        )
    }

    private fun CompensationInput.sanitized(): CompensationInput = copy(
        currentTakeHomeRate = currentTakeHomeRate.coerceIn(0.0, 1.0),
        newTakeHomeRate = newTakeHomeRate.coerceIn(0.0, 1.0),
        scenarioBases = scenarioBases.filter { it > 0.0 }.distinct().sorted().ifEmpty {
            listOf(120000.0, 125000.0, 130000.0, 135000.0, 140000.0, 145000.0, 150000.0)
        }
    )

    private fun currentJobTotalThrough(
        input: CompensationInput,
        asOf: LocalDate,
        leavingAt: LocalDate? = null
    ): Double {
        if (!asOf.isAfter(input.joinDate)) return 0.0

        val salaryNet = input.currentBaseSalary * salaryYears(input.joinDate, asOf) * input.currentTakeHomeRate
        val firstSignOnNet = input.firstSignOnGross * input.currentTakeHomeRate
        val firstSignOnRepayment = firstSignOnCashRepayment(input, leavingAt)
        val secondSignOnNet = earnedSecondSignOnGross(input, asOf) * input.currentTakeHomeRate
        val rsuNet = rsuVestsThrough(input, asOf).sumOf { it.netValue }

        val relocationNet = input.currentRelocationReceivedNet - if (leavingAt != null) {
            input.currentRelocationRepaymentIfSwitchNet
        } else {
            0.0
        }

        return salaryNet + firstSignOnNet - firstSignOnRepayment + secondSignOnNet + rsuNet + relocationNet
    }

    private fun firstSignOnCashRepayment(input: CompensationInput, leavingAt: LocalDate?): Double {
        if (leavingAt == null) return 0.0
        val cliff = input.joinDate.plusYears(input.firstSignOnClawbackYears.toLong())
        if (!leavingAt.isBefore(cliff)) return 0.0
        val totalDays = max(1L, ChronoUnit.DAYS.between(input.joinDate, cliff))
        val workedDays = ChronoUnit.DAYS.between(input.joinDate, leavingAt).coerceIn(0L, totalDays)
        val unearnedFraction = (totalDays - workedDays).toDouble() / totalDays.toDouble()
        return input.firstSignOnGross * unearnedFraction
    }

    private fun earnedSecondSignOnGross(input: CompensationInput, asOf: LocalDate): Double {
        val start = input.joinDate.plusYears(1)
        val end = input.joinDate.plusYears(2)
        if (!asOf.isAfter(start)) return 0.0
        val totalDays = max(1L, ChronoUnit.DAYS.between(start, end))
        val earnedUntil = minOf(asOf, end)
        val daysEarned = ChronoUnit.DAYS.between(start, earnedUntil).coerceIn(0L, totalDays)
        return input.secondSignOnGross * daysEarned.toDouble() / totalDays.toDouble()
    }

    private fun rsuVestsThrough(input: CompensationInput, asOf: LocalDate): List<RsuVest> {
        val vestSchedule = listOf(
            RsuScheduleItem(input.firstAnniversaryVestDate, 0.05, "First anniversary 5%"),
            RsuScheduleItem(input.secondAnniversaryVestDate, 0.15, "Second anniversary 15%"),
            RsuScheduleItem(input.secondAnniversaryVestDate.plusMonths(6), 0.20, "Six-month vest 20%"),
            RsuScheduleItem(input.secondAnniversaryVestDate.plusMonths(12), 0.20, "Six-month vest 20%"),
            RsuScheduleItem(input.secondAnniversaryVestDate.plusMonths(18), 0.20, "Six-month vest 20%"),
            RsuScheduleItem(input.secondAnniversaryVestDate.plusMonths(24), 0.20, "Final six-month vest 20%")
        )

        return vestSchedule
            .filter { !it.date.isAfter(asOf) }
            .map { item ->
                val shares = input.rsuShares * item.percent
                val gross = shares * input.rsuSharePrice
                RsuVest(
                    date = item.date,
                    label = item.label,
                    percent = item.percent,
                    shares = shares,
                    grossValue = gross,
                    netValue = gross * input.currentTakeHomeRate
                )
            }
    }

    private fun cashTimeline(input: CompensationInput, selectedBase: Double): List<CashPoint> {
        val checkpoints = buildList {
            add(input.switchDate)
            add(input.switchDate.plusYears(1))
            add(input.switchDate.plusYears(2))
            add(input.endDate)
        }.distinct().filter { !it.isBefore(input.joinDate) }.sorted()

        return checkpoints.map { date ->
            val stay = currentJobTotalThrough(input, date)
            val switch = if (!date.isAfter(input.switchDate)) {
                currentJobTotalThrough(input, date, leavingAt = input.switchDate)
            } else {
                currentJobTotalThrough(input, input.switchDate, leavingAt = input.switchDate) +
                    selectedBase * salaryYears(input.switchDate, date) * input.newTakeHomeRate +
                    input.newCompanyRelocationNet
            }
            CashPoint(label = date.format(MONTH_YEAR), date = date, stayNet = stay, switchNet = switch)
        }
    }

    fun salaryYears(start: LocalDate, end: LocalDate): Double {
        if (!end.isAfter(start)) return 0.0
        var fullYears = end.year - start.year
        while (start.plusYears(fullYears.toLong()).isAfter(end)) fullYears--
        val anchor = start.plusYears(fullYears.toLong())
        val remainingDays = ChronoUnit.DAYS.between(anchor, end).coerceAtLeast(0L)
        return fullYears.toDouble() + remainingDays.toDouble() / 365.0
    }

    private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
}

data class CompensationInput(
    val joinDate: LocalDate = LocalDate.of(2025, 10, 20),
    val switchDate: LocalDate = LocalDate.of(2026, 11, 1),
    val endDate: LocalDate = LocalDate.of(2029, 11, 1),
    val currentBaseSalary: Double = 110000.0,
    val currentTakeHomeRate: Double = 0.76,
    val newTakeHomeRate: Double = 0.70,
    val firstSignOnGross: Double = 18200.0,
    val firstSignOnClawbackYears: Int = 1,
    val secondSignOnGross: Double = 13500.0,
    val rsuShares: Double = 170.0,
    val rsuSharePrice: Double = 241.70,
    val currentRelocationReceivedNet: Double = 7000.0,
    val currentRelocationRepaymentIfSwitchNet: Double = 3500.0,
    val newCompanyRelocationNet: Double = 0.0,
    val selectedSwitchBase: Double = 140000.0,
    val scenarioBases: List<Double> = listOf(120000.0, 125000.0, 130000.0, 135000.0, 140000.0, 145000.0, 150000.0),
    val firstAnniversaryVestDate: LocalDate = LocalDate.of(2026, 10, 15),
    val secondAnniversaryVestDate: LocalDate = LocalDate.of(2027, 10, 15)
)

data class AnalysisResult(
    val input: CompensationInput,
    val stayTotalNet: Double,
    val switchBeforeNewCompanyNet: Double,
    val selectedSwitchTotalNet: Double,
    val selectedSwitchDeltaVsStay: Double,
    val breakEvenBase: Double,
    val currentRsuVestsThroughStay: List<RsuVest>,
    val currentRsuVestsBeforeSwitch: List<RsuVest>,
    val scenarios: List<SalaryScenario>,
    val cashTimeline: List<CashPoint>
)

data class SalaryScenario(
    val baseSalary: Double,
    val newCompanySalaryNet: Double,
    val totalNet: Double,
    val deltaVsStay: Double,
    val isWinner: Boolean
)

data class RsuVest(
    val date: LocalDate,
    val label: String,
    val percent: Double,
    val shares: Double,
    val grossValue: Double,
    val netValue: Double
)

data class CashPoint(
    val label: String,
    val date: LocalDate,
    val stayNet: Double,
    val switchNet: Double
)

private data class RsuScheduleItem(
    val date: LocalDate,
    val percent: Double,
    val label: String
)
