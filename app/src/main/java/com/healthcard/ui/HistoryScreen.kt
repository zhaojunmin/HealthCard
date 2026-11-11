package com.healthcard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.healthcard.data.HealthRecord
import java.time.DayOfWeek
import java.time.LocalDate

private data class HabitSpec(
    val type: String,
    val label: String,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
)

private val HABIT_SPECS = listOf(
    HabitSpec("exercise", "运动", Icons.Outlined.DirectionsRun, Icons.Filled.DirectionsRun),
    HabitSpec("diet", "饮食", Icons.Outlined.Restaurant, Icons.Filled.Restaurant),
    HabitSpec("sleep", "睡觉", Icons.Outlined.Bedtime, Icons.Filled.Bedtime),
    HabitSpec("mood", "心态", Icons.Outlined.SelfImprovement, Icons.Filled.SelfImprovement),
)

@Composable
fun HistoryScreen(viewModel: com.healthcard.viewmodel.HealthViewModel, onBackClick: () -> Unit) = HealthTheme {
    val colors = MaterialTheme.colorScheme
    LaunchedEffect(Unit) { viewModel.loadHistory() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        // ---- 顶部栏 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "打卡历史",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
            )
        }

        when {
            viewModel.isLoading.value -> LoadingState()
            viewModel.groupedRecords.value.isEmpty() -> EmptyState()
            else -> {
                val grouped = viewModel.groupedRecords.value
                val days = grouped.entries.toList()
                val dateSet = grouped.keys.toSet()
                val streak = computeStreak(dateSet)
                val perfectDays = grouped.count { it.value.map { r -> r.type }.toSet().size >= 4 }
                val total = grouped.values.sumOf { it.size }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        SummaryCard(streak = streak, perfectDays = perfectDays, total = total)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(days) { (date, records) ->
                        DayCard(date = date, records = records)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(streak: Int, perfectDays: Int, total: Int) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.primaryContainer)
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatItem(value = "$streak", unit = "天", label = "连续打卡", showFlame = streak > 0)
        StatDivider()
        StatItem(value = "$perfectDays", unit = "天", label = "全勤")
        StatDivider()
        StatItem(value = "$total", unit = "次", label = "累计打卡")
    }
}

@Composable
private fun StatItem(value: String, unit: String, label: String, showFlame: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showFlame) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimaryContainer,
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 2.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onPrimaryContainer.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = 34.dp)
            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)),
    )
}

@Composable
private fun DayCard(date: String, records: List<HealthRecord>) {
    val colors = MaterialTheme.colorScheme
    val doneTypes = records.map { it.type }.toSet()
    val doneCount = HABIT_SPECS.count { it.type in doneTypes }
    val perfect = doneCount >= HABIT_SPECS.size
    val shape = RoundedCornerShape(20.dp)
    val (mainLabel, relativeLabel) = formatDate(date)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (perfect) colors.primaryContainer else colors.surface)
            .then(if (perfect) Modifier else Modifier.border(1.dp, colors.outline, shape))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = mainLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (perfect) colors.onPrimaryContainer else colors.onSurface,
            )
            if (relativeLabel != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (perfect) colors.onPrimaryContainer.copy(alpha = 0.12f)
                            else colors.surfaceVariant
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = relativeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (perfect) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            // 完成度 pill
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (perfect) colors.primary else colors.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "$doneCount/${HABIT_SPECS.size}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (perfect) colors.onPrimary else colors.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HABIT_SPECS.forEach { spec ->
                HabitDot(spec = spec, done = spec.type in doneTypes)
            }
        }
    }
}

@Composable
private fun HabitDot(spec: HabitSpec, done: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (done) colors.primary else colors.surfaceVariant)
                .then(if (done) Modifier else Modifier.border(1.dp, colors.outline, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (done) spec.iconFilled else spec.iconOutlined,
                contentDescription = spec.label,
                tint = if (done) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = spec.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (done) colors.onSurface else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "加载中...", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.EventNote,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "还没有打卡记录",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "回到首页完成今天的第一次打卡吧",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

/** 从今天（或昨天）起向前连续有打卡记录的天数。 */
private fun computeStreak(dates: Set<String>): Int {
    if (dates.isEmpty()) return 0
    var day = LocalDate.now()
    if (!dates.contains(day.toString())) day = day.minusDays(1)
    var streak = 0
    while (dates.contains(day.toString())) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

/** ISO 日期 → ("11月11日·周二", "今天"/"昨天"/null)。解析失败则回退原串。 */
private fun formatDate(iso: String): Pair<String, String?> {
    return try {
        val date = LocalDate.parse(iso)
        val weekday = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "周一"
            DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"
            DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"
            DayOfWeek.SATURDAY -> "周六"
            DayOfWeek.SUNDAY -> "周日"
        }
        val today = LocalDate.now()
        val relative = when (date) {
            today -> "今天"
            today.minusDays(1) -> "昨天"
            else -> null
        }
        "${date.monthValue}月${date.dayOfMonth}日 · $weekday" to relative
    } catch (e: Exception) {
        iso to null
    }
}
