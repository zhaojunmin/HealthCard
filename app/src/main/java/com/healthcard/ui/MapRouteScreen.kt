package com.healthcard.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun MapRouteScreen(viewModel: MapRouteViewModel, onBack: () -> Unit) {
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

    // 路线/起终点变化时在地图上绘制
    val route = viewModel.route.value
    LaunchedEffect(route, viewModel.origin.value, viewModel.destination.value) {
        val aMap = mapView.map ?: return@LaunchedEffect
        aMap.clear()
        val origin = viewModel.origin.value
        val dest = viewModel.destination.value
        origin?.let {
            aMap.addMarker(MarkerOptions().position(LatLng(it.lat, it.lon)).title("起点"))
        }
        dest?.let {
            aMap.addMarker(
                MarkerOptions().position(LatLng(it.lat, it.lon))
                    .title(viewModel.destName.value.ifBlank { "终点" })
            )
        }
        if (route != null && route.points.isNotEmpty()) {
            val pts = route.points.map { LatLng(it.lat, it.lon) }
            aMap.addPolyline(
                PolylineOptions().addAll(pts).width(18f).color(0xFF2196F3.toInt())
            )
            val builder = LatLngBounds.Builder()
            pts.forEach { builder.include(it) }
            aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
        } else if (dest != null) {
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(dest.lat, dest.lon), 15f))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
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
                text = viewModel.destName.value.ifBlank { "路线地图" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
            )
        }

        route?.let {
            Text(
                text = "全程约 %.1f 公里 · 预计步行 %d 分钟".format(
                    it.distanceMeters / 1000.0,
                    (it.durationSeconds + 59) / 60,
                ),
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            if (viewModel.isLoading.value) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            viewModel.error.value?.let { msg ->
                Text(
                    text = msg,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                )
            }
        }
    }
}
