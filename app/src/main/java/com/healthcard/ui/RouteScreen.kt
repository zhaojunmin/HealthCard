package com.healthcard.ui

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.healthcard.a2ui.A2uiSurface
import com.healthcard.viewmodel.RouteViewModel

@Composable
fun RouteScreen(
    viewModel: RouteViewModel,
    onBack: () -> Unit,
    onConfigClick: () -> Unit,
    onShowRouteOnMap: (index: Int) -> Unit,
) = HealthTheme {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

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
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "返回", onBack)
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "跑步路线",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
            )
            Spacer(modifier = Modifier.weight(1f))
            CircleIconButton(Icons.Outlined.Settings, "配置", onConfigClick)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // 目标距离
            SectionLabel("目标距离")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(3, 5, 10).forEach { km ->
                    SelectChip(
                        label = "$km 公里",
                        selected = viewModel.targetDistanceKm.value == km,
                        onClick = { viewModel.targetDistanceKm.value = km },
                    )
                }
            }

            // 场景偏好
            Spacer(modifier = Modifier.height(18.dp))
            SectionLabel("场景偏好")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    RouteViewModel.PREF_ANY,
                    RouteViewModel.PREF_PARK,
                    RouteViewModel.PREF_RIVER,
                    RouteViewModel.PREF_GREENWAY,
                ).forEach { pref ->
                    SelectChip(
                        label = pref,
                        selected = viewModel.preference.value == pref,
                        onClick = { viewModel.preference.value = pref },
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsRun,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "获取周边跑步路线",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // 定位到的位置信息
            val place = viewModel.placeName.value
            val loc = viewModel.location.value
            if (place != null || loc != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.primaryContainer)
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "当前位置",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = place ?: "未能解析地点名称",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onPrimaryContainer,
                        )
                        if (loc != null) {
                            Text(
                                text = "经纬度 %.5f, %.5f".format(loc.latitude, loc.longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onPrimaryContainer.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when {
                viewModel.isLoading.value -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("正在规划路线…", color = colors.onSurfaceVariant)
                    }
                }

                viewModel.errorMessage.value != null -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.error.copy(alpha = 0.1f))
                            .padding(14.dp),
                    ) {
                        Text(
                            text = viewModel.errorMessage.value ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.error,
                        )
                    }
                }

                viewModel.surface.value != null -> {
                    A2uiSurface(
                        message = viewModel.surface.value!!,
                        modifier = Modifier.fillMaxWidth(),
                        onAction = { action -> handleRouteAction(action, context, onShowRouteOnMap) },
                    )
                }

                else -> {
                    EmptyHint()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
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
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun EmptyHint() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant)
            .padding(18.dp),
    ) {
        Text(
            text = "获取 AI 规划的周边跑步路线",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "选择目标距离与场景偏好后点击上方按钮。首次使用请先在右上角「配置」中设置 API Key。",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

/** 处理 A2UI 按钮动作。支持 "route:<index>" → 打开应用内地图展示该条完整路线。 */
private fun handleRouteAction(
    action: String,
    context: Context,
    onShowRouteOnMap: (index: Int) -> Unit,
) {
    if (action.startsWith("route:")) {
        val index = action.removePrefix("route:").trim().toIntOrNull()
        if (index != null) {
            onShowRouteOnMap(index)
        } else {
            Toast.makeText(context, "无效的路线", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = CircleShape
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) colors.primary else colors.surfaceVariant)
            .then(if (selected) Modifier else Modifier.border(1.dp, colors.outline, shape))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}
