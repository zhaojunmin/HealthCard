package com.healthcard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthcard.data.HealthRecord
import com.healthcard.viewmodel.HealthViewModel

@Composable
fun HistoryScreen(viewModel: HealthViewModel, onBackClick: () -> Unit) {
    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Add padding at the top to avoid status bar overlap
        Spacer(modifier = Modifier.height(48.dp))
        
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2196F3))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "返回",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.clickable { onBackClick() }
            )
            Text(
                text = "打卡历史",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(50.dp))
        }

        // Loading indicator
        if (viewModel.isLoading.value) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "加载中...")
            }
        } else {
            // History list
            if (viewModel.groupedRecords.value.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "暂无打卡记录",
                        fontSize = 18.sp,
                        color = Color(0xFF666666)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    viewModel.groupedRecords.value.forEach { (date, records) ->
                        item {
                            HistoryDayCard(date = date, records = records)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryDayCard(date: String, records: List<HealthRecord>) {
    val recordTypes = records.map { it.type }.toSet()
    val allTypes = listOf("exercise", "diet", "sleep", "mood")
    val typeNames = mapOf(
        "exercise" to "健康运动",
        "diet" to "健康饮食",
        "sleep" to "健康睡觉",
        "mood" to "健康心态"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = date,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                allTypes.forEach { type ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isChecked = recordTypes.contains(type)
                        val color = if (isChecked) Color(0xFF4CAF50) else Color(0xFFE0E0E0)
                        val textColor = if (isChecked) Color.White else Color(0xFF666666)

                        Column(
                            modifier = Modifier
                                .size(80.dp)
                                .background(color, RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isChecked) "已打卡" else "未打卡",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = typeNames[type] ?: type,
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "完成 ${recordTypes.size}/4",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}
