package com.healthcard.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.healthcard.viewmodel.HealthViewModel

private data class Habit(
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
    val title: String,
    val description: String,
    val isChecked: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun MainScreen(
    viewModel: HealthViewModel,
    onHistoryClick: () -> Unit,
    onRouteClick: () -> Unit,
    onConfigClick: () -> Unit,
) = HealthTheme {
    val colors = MaterialTheme.colorScheme
    val count = viewModel.todayCount.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        // ---- 顶部：标题 + 导航 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "健康打卡",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                )
                Text(
                    text = "坚持每一天",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            NavIcon(Icons.Outlined.Route, "跑步路线", onRouteClick)
            Spacer(modifier = Modifier.size(10.dp))
            NavIcon(Icons.Outlined.CalendarMonth, "打卡历史", onHistoryClick)
            Spacer(modifier = Modifier.size(10.dp))
            NavIcon(Icons.Outlined.Settings, "设置", onConfigClick)
        }

        // ---- 今日进度活动环 ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            ProgressRing(done = count, total = 4)
        }

        // ---- 2×2 打卡卡片 ----
        val habits = listOf(
            Habit(Icons.Outlined.DirectionsRun, Icons.Filled.DirectionsRun,
                "健康运动", "跑步五公里", viewModel.isExerciseChecked.value) { viewModel.checkExercise() },
            Habit(Icons.Outlined.Restaurant, Icons.Filled.Restaurant,
                "健康饮食", "少油少盐少脂", viewModel.isDietChecked.value) { viewModel.checkDiet() },
            Habit(Icons.Outlined.Bedtime, Icons.Filled.Bedtime,
                "健康睡觉", "11 点前睡觉", viewModel.isSleepChecked.value) { viewModel.checkSleep() },
            Habit(Icons.Outlined.SelfImprovement, Icons.Filled.SelfImprovement,
                "健康心态", "读书反思", viewModel.isMoodChecked.value) { viewModel.checkMood() },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            for (rowStart in habits.indices step 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    for (i in rowStart until rowStart + 2) {
                        HabitCard(
                            habit = habits[i],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun NavIcon(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ProgressRing(done: Int, total: Int) {
    val colors = MaterialTheme.colorScheme
    val target = if (total == 0) 0f else done.toFloat() / total
    val sweep by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "ringSweep",
    )
    val ringBrush = Brush.sweepGradient(
        listOf(colors.primary, colors.tertiary, colors.primary)
    )

    Box(
        modifier = Modifier.size(196.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            // 轨道
            drawArc(
                color = colors.surfaceVariant,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            // 进度
            if (sweep > 0f) {
                drawArc(
                    brush = ringBrush,
                    startAngle = -90f,
                    sweepAngle = 360f * sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                targetState = done,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith
                        (slideOutVertically { -it } + fadeOut())
                },
                label = "doneCount",
            ) { value ->
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                )
            }
            Text(
                text = "/ $total 已完成",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HabitCard(habit: Habit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val checked = habit.isChecked
    val shape = RoundedCornerShape(20.dp)

    val container by animateColorAsState(
        targetValue = if (checked) colors.primaryContainer else colors.surfaceVariant,
        animationSpec = tween(300),
        label = "container",
    )
    val content by animateColorAsState(
        targetValue = if (checked) colors.onPrimaryContainer else colors.onSurface,
        animationSpec = tween(300),
        label = "content",
    )

    Column(
        modifier = modifier
            .clip(shape)
            .background(container)
            .then(if (checked) Modifier else Modifier.border(1.dp, colors.outline, shape))
            .clickable(enabled = !checked) { habit.onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 图标：未完成 outlined，完成 filled，底色跟随状态，居中
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (checked) colors.primary else colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (checked) habit.iconFilled else habit.iconOutlined,
                contentDescription = habit.title,
                tint = if (checked) colors.onPrimary else colors.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = habit.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = content,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = habit.description,
            style = MaterialTheme.typography.bodyMedium,
            color = if (checked) content.copy(alpha = 0.75f) else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(
                visible = checked,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
            }
            Text(
                text = if (checked) "已完成" else "点击打卡",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (checked) colors.onPrimaryContainer else colors.onSurfaceVariant,
            )
        }
    }
}
