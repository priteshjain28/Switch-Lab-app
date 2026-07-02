package com.offerverse.switchlab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offerverse.switchlab.model.AnalysisResult
import com.offerverse.switchlab.model.CalculatorEngine
import com.offerverse.switchlab.model.CompensationInput
import com.offerverse.switchlab.model.RsuVest
import com.offerverse.switchlab.model.SalaryScenario
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OfferVerseApp() }
    }
}

@Composable
private fun OfferVerseApp() {
    val colors = darkColorScheme()
    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = AppColors.Background) {
            SwitchLabScreen()
        }
    }
}

@Composable
private fun SwitchLabScreen() {
    val joinDate = rememberPreference("joinDate", "2025-10-20")
    val switchDate = rememberPreference("switchDate", "2026-11-01")
    val endDate = rememberPreference("endDate", "2029-11-01")
    val currentBase = rememberPreference("currentBase", "110000")
    val currentTakeHome = rememberPreference("currentTakeHome", "76")
    val newTakeHome = rememberPreference("newTakeHome", "70")
    val firstSignOn = rememberPreference("firstSignOn", "18200")
    val secondSignOn = rememberPreference("secondSignOn", "13500")
    val rsuShares = rememberPreference("rsuShares", "170")
    val rsuPrice = rememberPreference("rsuPrice", "241.70")
    val currentRelocation = rememberPreference("currentRelocation", "7000")
    val relocationRepay = rememberPreference("relocationRepay", "3500")
    val newRelocation = rememberPreference("newRelocation", "0")
    val selectedBase = rememberPreference("selectedBase", "140000")
    val scenarioBases = rememberPreference("scenarioBases", "120000,125000,130000,135000,140000,145000,150000")
    val firstVest = rememberPreference("firstVest", "2026-10-15")
    val secondVest = rememberPreference("secondVest", "2027-10-15")

    val input = CompensationInput(
        joinDate = joinDate.value.asDate(LocalDate.of(2025, 10, 20)),
        switchDate = switchDate.value.asDate(LocalDate.of(2026, 11, 1)),
        endDate = endDate.value.asDate(LocalDate.of(2029, 11, 1)),
        currentBaseSalary = currentBase.value.asMoney(110000.0),
        currentTakeHomeRate = currentTakeHome.value.asRate(0.76),
        newTakeHomeRate = newTakeHome.value.asRate(0.70),
        firstSignOnGross = firstSignOn.value.asMoney(18200.0),
        secondSignOnGross = secondSignOn.value.asMoney(13500.0),
        rsuShares = rsuShares.value.asMoney(170.0),
        rsuSharePrice = rsuPrice.value.asMoney(241.70),
        currentRelocationReceivedNet = currentRelocation.value.asMoney(7000.0),
        currentRelocationRepaymentIfSwitchNet = relocationRepay.value.asMoney(3500.0),
        newCompanyRelocationNet = newRelocation.value.asMoney(0.0),
        selectedSwitchBase = selectedBase.value.asMoney(140000.0),
        scenarioBases = scenarioBases.value.asMoneyList(),
        firstAnniversaryVestDate = firstVest.value.asDate(LocalDate.of(2026, 10, 15)),
        secondAnniversaryVestDate = secondVest.value.asDate(LocalDate.of(2027, 10, 15))
    )

    val analysis = CalculatorEngine.analyze(input)
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HeroHeader() }
        item { Dashboard(analysis = analysis) }
        item { DeltaBarChart(analysis.scenarios) }
        item { CashLineChart(analysis) }
        item { DecisionCard(analysis) }
        item {
            ActionButtons(
                onShare = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, buildReport(analysis))
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share SwitchLab report"))
                },
                onReset = {
                    joinDate.value = "2025-10-20"
                    switchDate.value = "2026-11-01"
                    endDate.value = "2029-11-01"
                    currentBase.value = "110000"
                    currentTakeHome.value = "76"
                    newTakeHome.value = "70"
                    firstSignOn.value = "18200"
                    secondSignOn.value = "13500"
                    rsuShares.value = "170"
                    rsuPrice.value = "241.70"
                    currentRelocation.value = "7000"
                    relocationRepay.value = "3500"
                    newRelocation.value = "0"
                    selectedBase.value = "140000"
                    scenarioBases.value = "120000,125000,130000,135000,140000,145000,150000"
                    firstVest.value = "2026-10-15"
                    secondVest.value = "2027-10-15"
                }
            )
        }
        item {
            ScenarioTable(
                stayTotal = analysis.stayTotalNet,
                scenarios = analysis.scenarios
            )
        }
        item { RsuTable(title = "Amazon RSU vesting included if you stay", vests = analysis.currentRsuVestsThroughStay) }
        item { RsuTable(title = "Amazon RSU vesting included before switch", vests = analysis.currentRsuVestsBeforeSwitch) }
        item {
            InputSection(
                joinDate = joinDate,
                switchDate = switchDate,
                endDate = endDate,
                currentBase = currentBase,
                currentTakeHome = currentTakeHome,
                newTakeHome = newTakeHome,
                firstSignOn = firstSignOn,
                secondSignOn = secondSignOn,
                rsuShares = rsuShares,
                rsuPrice = rsuPrice,
                currentRelocation = currentRelocation,
                relocationRepay = relocationRepay,
                newRelocation = newRelocation,
                selectedBase = selectedBase,
                scenarioBases = scenarioBases,
                firstVest = firstVest,
                secondVest = secondVest
            )
        }
        item { FooterNotes() }
    }
}

@Composable
private fun HeroHeader() {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF7C3AED), Color(0xFF0891B2), Color(0xFF111827))
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("OfferVerse SwitchLab", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text(
                    "Native Android salary switch simulator for base, bonus, RSUs, relocation clawback, taxes, break-even, and cash path.",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Amazon → Cap One")
                    Pill("4-year view")
                    Pill("H1B cash focus")
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(50.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun Dashboard(analysis: AnalysisResult) {
    SectionCard(title = "Money dashboard") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricTile("Stay total", money(analysis.stayTotalNet), "Amazon through ${analysis.input.endDate.format(DATE_SHORT)}", Modifier.weight(1f))
                MetricTile("Switch total", money(analysis.selectedSwitchTotalNet), "At ${money(analysis.input.selectedSwitchBase)} base", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricTile("Break-even base", money(analysis.breakEvenBase), "Minimum to match staying", Modifier.weight(1f))
                MetricTile(
                    "Delta",
                    signedMoney(analysis.selectedSwitchDeltaVsStay),
                    if (analysis.selectedSwitchDeltaVsStay >= 0) "Switch wins" else "Amazon wins",
                    Modifier.weight(1f),
                    highlight = analysis.selectedSwitchDeltaVsStay >= 0
                )
            }
            SmallFact("Amazon money before switch", money(analysis.switchBeforeNewCompanyNet))
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, hint: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val bg = if (highlight) AppColors.Win.copy(alpha = 0.18f) else AppColors.Surface2
    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, color = AppColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Text(hint, color = AppColors.Muted, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun SmallFact(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface2, RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = AppColors.Muted, fontSize = 13.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DecisionCard(analysis: AnalysisResult) {
    val scenarioAboveBreakEven = analysis.scenarios.firstOrNull { it.totalNet >= analysis.stayTotalNet }
    val headline = when {
        analysis.selectedSwitchDeltaVsStay > 5000.0 -> "Switch is financially better."
        abs(analysis.selectedSwitchDeltaVsStay) <= 5000.0 -> "This is basically break-even."
        else -> "Staying wins financially."
    }
    SectionCard(title = "Decision readout") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(headline, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(
                "At ${money(analysis.input.selectedSwitchBase)} base, the switch path ends at ${money(analysis.selectedSwitchTotalNet)}, which is ${signedMoney(analysis.selectedSwitchDeltaVsStay)} versus staying. Your break-even base is ${money(analysis.breakEvenBase)}.",
                color = AppColors.Text,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            if (scenarioAboveBreakEven != null) {
                Text(
                    "In your scenario ladder, the first base that matches or beats Amazon is ${money(scenarioAboveBreakEven.baseSalary)}.",
                    color = AppColors.Cyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DeltaBarChart(scenarios: List<SalaryScenario>) {
    SectionCard(title = "Switch advantage by base salary") {
        if (scenarios.isEmpty()) return@SectionCard
        val maxAbs = scenarios.maxOf { abs(it.deltaVsStay) }.coerceAtLeast(1.0)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(AppColors.Surface2, RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                val centerY = size.height / 2f
                drawLine(AppColors.Muted.copy(alpha = 0.6f), Offset(0f, centerY), Offset(size.width, centerY), strokeWidth = 2f)
                val slot = size.width / scenarios.size
                val barWidth = slot * 0.56f
                scenarios.forEachIndexed { index, scenario ->
                    val barHeight = ((abs(scenario.deltaVsStay) / maxAbs) * (size.height * 0.42f)).toFloat()
                    val left = index * slot + (slot - barWidth) / 2f
                    val top = if (scenario.deltaVsStay >= 0.0) centerY - barHeight else centerY
                    val color = if (scenario.deltaVsStay >= 0.0) AppColors.Win else AppColors.Loss
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, max(4f, barHeight)),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                scenarios.forEach {
                    Text("${(it.baseSalary / 1000).toInt()}k", color = AppColors.Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
            Text("Bars above the center line beat staying. Bars below lose to staying.", color = AppColors.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CashLineChart(analysis: AnalysisResult) {
    SectionCard(title = "Cumulative cash path") {
        val points = analysis.cashTimeline
        if (points.size < 2) return@SectionCard
        val maxY = points.maxOf { max(it.stayNet, it.switchNet) }.coerceAtLeast(1.0)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(AppColors.Surface2, RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                fun y(value: Double): Float = (size.height - 20f - ((value / maxY) * (size.height - 42f))).toFloat()
                fun x(index: Int): Float = 18f + index * ((size.width - 36f) / (points.lastIndex.coerceAtLeast(1)))

                val stayPath = Path()
                val switchPath = Path()
                points.forEachIndexed { index, point ->
                    val px = x(index)
                    val stayY = y(point.stayNet)
                    val switchY = y(point.switchNet)
                    if (index == 0) {
                        stayPath.moveTo(px, stayY)
                        switchPath.moveTo(px, switchY)
                    } else {
                        stayPath.lineTo(px, stayY)
                        switchPath.lineTo(px, switchY)
                    }
                    drawCircle(AppColors.Purple, radius = 5f, center = Offset(px, stayY))
                    drawCircle(AppColors.Cyan, radius = 5f, center = Offset(px, switchY))
                }
                drawPath(stayPath, AppColors.Purple, style = Stroke(width = 5f))
                drawPath(switchPath, AppColors.Cyan, style = Stroke(width = 5f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(AppColors.Purple, "Stay")
                LegendDot(AppColors.Cyan, "Switch @ ${money(analysis.input.selectedSwitchBase)}")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                points.forEach { Text(it.label, color = AppColors.Muted, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(50.dp)))
        Text(label, color = AppColors.Text, fontSize = 12.sp)
    }
}

@Composable
private fun ScenarioTable(stayTotal: Double, scenarios: List<SalaryScenario>) {
    SectionCard(title = "What-if salary ladder") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            scenarios.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Surface2, RoundedCornerShape(18.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(money(row.baseSalary), color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                        Text("Total: ${money(row.totalNet)}", color = AppColors.Muted, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            signedMoney(row.deltaVsStay),
                            color = if (row.deltaVsStay >= 0.0) AppColors.Win else AppColors.Loss,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Text(if (row.totalNet >= stayTotal) "Switch wins" else "Stay wins", color = AppColors.Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RsuTable(title: String, vests: List<RsuVest>) {
    SectionCard(title = title) {
        if (vests.isEmpty()) {
            Text("No RSU vest included before this date.", color = AppColors.Muted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vests.forEach { vest ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.Surface2, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(vest.date.format(DATE_SHORT), color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${percent(vest.percent)} • ${oneDecimal(vest.shares)} shares", color = AppColors.Muted, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(money(vest.netValue), color = AppColors.Cyan, fontWeight = FontWeight.Black)
                            Text("net est.", color = AppColors.Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(onShare: () -> Unit, onReset: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onShare,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Purple)
        ) { Text("Share report") }
        Button(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Surface2)
        ) { Text("Reset") }
    }
}

@Composable
private fun InputSection(
    joinDate: MutableState<String>,
    switchDate: MutableState<String>,
    endDate: MutableState<String>,
    currentBase: MutableState<String>,
    currentTakeHome: MutableState<String>,
    newTakeHome: MutableState<String>,
    firstSignOn: MutableState<String>,
    secondSignOn: MutableState<String>,
    rsuShares: MutableState<String>,
    rsuPrice: MutableState<String>,
    currentRelocation: MutableState<String>,
    relocationRepay: MutableState<String>,
    newRelocation: MutableState<String>,
    selectedBase: MutableState<String>,
    scenarioBases: MutableState<String>,
    firstVest: MutableState<String>,
    secondVest: MutableState<String>
) {
    SectionCard(title = "Inputs") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Dates use yyyy-mm-dd. Take-home rates can be entered as 76 or 0.76.", color = AppColors.Muted, fontSize = 12.sp)
            InputField("Join date", joinDate)
            InputField("Switch date", switchDate)
            InputField("Analysis end date", endDate)
            HorizontalDivider(color = AppColors.Muted.copy(alpha = 0.16f))
            InputField("Current base salary", currentBase)
            InputField("Current take-home %", currentTakeHome)
            InputField("New company take-home %", newTakeHome)
            InputField("First sign-on gross", firstSignOn)
            InputField("Second-year sign-on gross", secondSignOn)
            InputField("RSU shares", rsuShares)
            InputField("RSU share price", rsuPrice)
            InputField("First anniversary vest date", firstVest)
            InputField("Second anniversary vest date", secondVest)
            InputField("Current relocation received net", currentRelocation)
            InputField("Current relocation repay if switch net", relocationRepay)
            InputField("New company relocation net", newRelocation)
            InputField("Selected switch base for chart", selectedBase)
            InputField("Scenario bases", scenarioBases)
        }
    }
}

@Composable
private fun InputField(label: String, state: MutableState<String>) {
    OutlinedTextField(
        value = state.value,
        onValueChange = { state.value = it },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FooterNotes() {
    SectionCard(title = "Important") {
        Text(
            "This is an estimator. RSUs use the share price you enter, tax is approximated using your take-home percentages, and repayment policies can vary by payroll/legal handling. Use the output to negotiate the base and relocation number, then verify with payroll/offer docs.",
            color = AppColors.Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            content()
        }
    }
}

@Composable
private fun rememberPreference(key: String, default: String): MutableState<String> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("switchlab", Context.MODE_PRIVATE) }
    val state = rememberSaveable { mutableStateOf(prefs.getString(key, default) ?: default) }
    LaunchedEffect(key, state.value) {
        prefs.edit().putString(key, state.value).apply()
    }
    return state
}

private fun buildReport(analysis: AnalysisResult): String = buildString {
    appendLine("OfferVerse SwitchLab report")
    appendLine("Stay total: ${money(analysis.stayTotalNet)}")
    appendLine("Switch total @ ${money(analysis.input.selectedSwitchBase)} base: ${money(analysis.selectedSwitchTotalNet)}")
    appendLine("Delta: ${signedMoney(analysis.selectedSwitchDeltaVsStay)}")
    appendLine("Break-even base: ${money(analysis.breakEvenBase)}")
    appendLine()
    appendLine("Scenario ladder:")
    analysis.scenarios.forEach {
        appendLine("${money(it.baseSalary)} base -> ${money(it.totalNet)} total (${signedMoney(it.deltaVsStay)})")
    }
}

private fun String.asMoney(default: Double): Double = cleanNumber().toDoubleOrNull() ?: default

private fun String.asRate(default: Double): Double {
    val raw = cleanNumber().toDoubleOrNull() ?: return default
    return if (raw > 1.0) raw / 100.0 else raw
}

private fun String.asDate(default: LocalDate): LocalDate = runCatching { LocalDate.parse(trim()) }.getOrDefault(default)

private fun String.asMoneyList(): List<Double> = split(',', ';', '\n', ' ')
    .mapNotNull { it.cleanNumber().toDoubleOrNull() }
    .filter { it > 0.0 }

private fun String.cleanNumber(): String = trim().replace("$", "").replace(",", "").replace("%", "")

private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale.US).apply {
    maximumFractionDigits = 0
}.format(value)

private fun signedMoney(value: Double): String = if (value >= 0.0) "+${money(value)}" else "-${money(abs(value))}"

private fun percent(value: Double): String = "${(value * 100).toInt()}%"
private fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

private val DATE_SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

private object AppColors {
    val Background = Color(0xFF050713)
    val Card = Color(0xFF0E1324)
    val Surface2 = Color(0xFF161C31)
    val Text = Color(0xFFE5E7EB)
    val Muted = Color(0xFF9CA3AF)
    val Purple = Color(0xFF8B5CF6)
    val Cyan = Color(0xFF22D3EE)
    val Win = Color(0xFF22C55E)
    val Loss = Color(0xFFEF4444)
}

@Composable
private fun darkColorScheme() = androidx.compose.material3.darkColorScheme(
    primary = AppColors.Purple,
    secondary = AppColors.Cyan,
    background = AppColors.Background,
    surface = AppColors.Card,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = AppColors.Text,
    onSurface = AppColors.Text
)
