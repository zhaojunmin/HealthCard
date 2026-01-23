package com.healthcard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthcard.viewmodel.HealthViewModel

@Composable
fun MainScreen(viewModel: HealthViewModel, onHistoryClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header with history button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "打卡历史",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3),
                modifier = Modifier.clickable { onHistoryClick() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "健康打卡",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )

            Text(
                text = "今日已打卡: ${viewModel.todayCount.value}/4",
                fontSize = 18.sp,
                color = Color(0xFF666666)
            )

            // 第一排两个按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckButton(
                    title = "健康运动",
                    isChecked = viewModel.isExerciseChecked.value,
                    onClick = { viewModel.checkExercise() },
                    color = Color(0xFF4CAF50)
                )
                CheckButton(
                    title = "健康饮食",
                    isChecked = viewModel.isDietChecked.value,
                    onClick = { viewModel.checkDiet() },
                    color = Color(0xFFFF9800)
                )
            }

            // 第二排两个按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckButton(
                    title = "健康睡觉",
                    isChecked = viewModel.isSleepChecked.value,
                    onClick = { viewModel.checkSleep() },
                    color = Color(0xFF9C27B0)
                )
                CheckButton(
                    title = "健康心态",
                    isChecked = viewModel.isMoodChecked.value,
                    onClick = { viewModel.checkMood() },
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
fun CheckButton(
    title: String,
    isChecked: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val buttonModifier = Modifier
            .size(150.dp)
            .clip(CircleShape)
            .background(if (isChecked) color else Color.White)
            .border(2.dp, color, CircleShape)
            .clickable(enabled = !isChecked) { onClick() }

        Column(
            modifier = buttonModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isChecked) "已打卡" else "打卡",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isChecked) Color.White else color
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontSize = 16.sp,
            color = Color(0xFF333333),
            fontWeight = FontWeight.Medium
        )
    }
}
