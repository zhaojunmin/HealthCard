package com.healthcard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.healthcard.data.HealthRecord
import com.healthcard.viewmodel.HealthViewModel
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
fun HistoryScreen(viewModel: HealthViewModel, onBackClick: () -> Unit) = HealthTheme {
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
                val countByDate = grouped.mapValues { e -> e.value.map { r -> r.type }.toSet().size }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { SummaryCard(streak = streak, perfectDays = perfectDays, total = total) }
                    item { AchievementsRow(streak = streak, perfectDays = perfectDays, total = total) }
                    item { HeatmapCard(countByDate = countByDate) }
                    item { AiReportCard(viewModel = viewModel) }
                    item {
                        Text(
                            text = "每日记录",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
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

private data class Badge(val label: String, val icon: ImageVector, val earned: Boolean)

@Composable
private fun AchievementsRow(streak: Int, perfectDays: Int, total: Int) {
    val colors = MaterialTheme.colorScheme
    val badges = listOf(
        Badge("连续 7 天", Icons.Filled.LocalFireDepartment, streak >= 7),
        Badge("连续 30 天", Icons.Filled.MilitaryTech, streak >= 30),
        Badge("全勤 7 天", Icons.Filled.WorkspacePremium, perfectDays >= 7),
        Badge("打卡 50 次", Icons.Filled.EmojiEvents, total >= 50),
        Badge("打卡 100 次", Icons.Filled.EmojiEvents, total >= 100),
    )
    Column {
        Text(
            text = "成就",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            badges.forEach { badge ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(72.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (badge.earned) colors.primary else colors.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = badge.icon,
                            contentDescription = badge.label,
                            tint = if (badge.earned) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = badge.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (badge.earned) FontWeight.Bold else FontWeight.Normal,
                        color = if (badge.earned) colors.onSurface else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapCard(countByDate: Map<String, Int>) {
    val colors = MaterialTheme.colorScheme
    val today = LocalDate.now()
    // 对齐到 4 周前的周一，共 5 周
    val startMonday = today.minusWeeks(4).let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
    val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "打卡热力图",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "最近 5 周",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        // 星期表头
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            weekdayLabels.forEach { wd ->
                Text(
                    text = wd,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        // 5 周网格
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            for (week in 0 until 5) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (dow in 0 until 7) {
                        val date = startMonday.plusDays((week * 7 + dow).toLong())
                        val future = date.isAfter(today)
                        val count = countByDate[date.toString()] ?: 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(heatColor(count, future, colors)),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        // 图例
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("少", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Spacer(modifier = Modifier.width(6.dp))
            listOf(0, 1, 2, 3, 4).forEach { c ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(heatColor(c, false, colors)),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text("多", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

private fun heatColor(count: Int, future: Boolean, colors: androidx.compose.material3.ColorScheme): Color {
    if (future) return colors.surfaceVariant.copy(alpha = 0.35f)
    return when {
        count <= 0 -> colors.surfaceVariant
        count == 1 -> colors.primary.copy(alpha = 0.3f)
        count == 2 -> colors.primary.copy(alpha = 0.52f)
        count == 3 -> colors.primary.copy(alpha = 0.74f)
        else -> colors.primary
    }
}

@Composable
private fun AiReportCard(viewModel: HealthViewModel) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI 周报",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            viewModel.reportLoading.value -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("正在根据最近 7 天数据生成…", color = colors.onSurfaceVariant)
                }
            }

            viewModel.reportText.value != null -> {
                Text(
                    text = viewModel.reportText.value ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { viewModel.generateWeeklyReport() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "重新生成",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                    )
                }
            }

            viewModel.reportError.value != null -> {
                Text(
                    text = viewModel.reportError.value ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.error,
                )
                Spacer(modifier = Modifier.height(10.dp))
                GenerateButton(onClick = { viewModel.generateWeeklyReport() }, text = "重试")
            }

            else -> {
                Text(
                    text = "根据你最近 7 天的打卡数据，生成一段个性化的健康周报与建议。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                GenerateButton(onClick = { viewModel.generateWeeklyReport() }, text = "生成周报")
            }
        }
    }
}

@Composable
private fun GenerateButton(onClick: () -> Unit, text: String) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
        ),
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontWeight = FontWeight.Bold)
    }
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

/** ISO 日期 → ("11月11日 · 周二", "今天"/"昨天"/null)。解析失败则回退原串。 */
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
