package com.healthcard.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.healthcard.amap.AmapPrivacy
import com.healthcard.viewmodel.MapRouteViewModel

private const val ROUTE_LINE_COLOR = 0xFF10B981.toInt() // 翡翠绿，与应用主题一致

@Composable
fun MapRouteScreen(viewModel: MapRouteViewModel, onBack: () -> Unit) = HealthTheme {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    // 合规初始化必须在创建 MapView 之前
    AmapPrivacy.ensure(context)

    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context).apply { onCreate(Bundle()) } }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // 路线/起点/途经点变化时在地图上绘制
    val route = viewModel.route.value
    LaunchedEffect(route, viewModel.origin.value, viewModel.waypoints.value) {
        val aMap = mapView.map ?: return@LaunchedEffect
        aMap.clear()
        val origin = viewModel.origin.value
        val wps = viewModel.waypoints.value
        origin?.let {
            aMap.addMarker(MarkerOptions().position(LatLng(it.lat, it.lon)).title("起点/终点"))
        }
        wps.forEachIndexed { i, wp ->
            aMap.addMarker(
                MarkerOptions().position(LatLng(wp.lat, wp.lon)).title("途经点 ${i + 1}")
            )
        }
        if (route != null && route.points.isNotEmpty()) {
            val pts = route.points.map { LatLng(it.lat, it.lon) }
            aMap.addPolyline(
                PolylineOptions().addAll(pts).width(18f).color(ROUTE_LINE_COLOR)
            )
            val builder = LatLngBounds.Builder()
            pts.forEach { builder.include(it) }
            // 地图尚未完成布局时 newLatLngBounds 会抛 "Map size can't be 0"，容错处理
            try {
                aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
            } catch (e: Exception) {
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pts.first(), 14f))
            }
        } else if (origin != null) {
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(origin.lat, origin.lon), 15f))
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
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .clickable { onBack() },
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
                text = viewModel.routeName.value.ifBlank { "路线地图" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
            )
        }

        // ---- 距离/时长概览 ----
        route?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.primaryContainer)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RouteStat(
                    icon = Icons.Outlined.Straighten,
                    value = "%.1f".format(it.distanceMeters / 1000.0),
                    unit = "公里",
                    label = "全程",
                )
                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 34.dp)
                        .background(colors.onPrimaryContainer.copy(alpha = 0.15f)),
                )
                RouteStat(
                    icon = Icons.Outlined.Schedule,
                    value = "${(it.durationSeconds + 59) / 60}",
                    unit = "分钟",
                    label = "预计步行",
                )
            }
        }

        // ---- 地图 ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            if (viewModel.isLoading.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.primary)
                        Spacer(modifier = Modifier.size(12.dp))
                        Text("正在规划路线…", color = colors.onSurfaceVariant)
                    }
                }
            }
            viewModel.error.value?.let { msg ->
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.error.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = msg, color = colors.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun RouteStat(icon: ImageVector, value: String, unit: String, label: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.onPrimaryContainer,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer,
                )
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}
