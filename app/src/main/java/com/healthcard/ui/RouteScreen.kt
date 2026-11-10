package com.healthcard.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthcard.a2ui.A2uiSurface
import com.healthcard.viewmodel.RouteViewModel

@Composable
fun RouteScreen(viewModel: RouteViewModel, onBack: () -> Unit, onConfigClick: () -> Unit) {
    // 定位权限申请：授予后发起路线规划（Key 从「模型配置」读取）
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.planRoutes()
        } else {
            viewModel.errorMessage.value = "未授予定位权限，无法规划周边路线"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← 返回",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3),
                modifier = Modifier.clickable { onBack() }
            )
            Text(
                text = "跑步路线规划",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
            )
            Text(
                text = "配置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3),
                modifier = Modifier.clickable { onConfigClick() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                },
                enabled = !viewModel.isLoading.value,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("获取周边跑步路线")
            }

            Spacer(Modifier.height(16.dp))

            when {
                viewModel.isLoading.value -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.width(12.dp))
                        Text("正在规划路线…", color = Color(0xFF666666))
                    }
                }

                viewModel.errorMessage.value != null -> {
                    Text(
                        text = viewModel.errorMessage.value ?: "",
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }

                viewModel.surface.value != null -> {
                    A2uiSurface(
                        message = viewModel.surface.value!!,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                else -> {
                    Text(
                        text = "点击上方按钮获取 AI 规划的周边跑步路线（请先在右上角「配置」中设置 API Key）。",
                        color = Color(0xFF999999),
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
