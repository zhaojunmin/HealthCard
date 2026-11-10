package com.healthcard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthcard.service.KeepAliveService
import com.healthcard.ui.ConfigScreen
import com.healthcard.ui.HistoryScreen
import com.healthcard.ui.MainScreen
import com.healthcard.ui.MapRouteScreen
import com.healthcard.ui.RouteScreen
import com.healthcard.viewmodel.ConfigViewModel
import com.healthcard.viewmodel.HealthViewModel
import com.healthcard.viewmodel.MapRouteViewModel
import com.healthcard.viewmodel.RouteViewModel

class MainActivity : ComponentActivity() {

    // 通知权限（Android 13+）请求结果：无论是否授予都启动保活服务，
    // 权限仅影响常驻通知是否可见，服务本身仍会运行。
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            KeepAliveService.start(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureKeepAliveRunning()
        setContent {
            MaterialTheme {
                Surface {
                    val viewModel: HealthViewModel = viewModel(factory = HealthViewModel.Factory(application))
                    val routeViewModel: RouteViewModel = viewModel(factory = RouteViewModel.Factory(application))
                    val configViewModel: ConfigViewModel = viewModel(factory = ConfigViewModel.Factory(application))
                    val mapRouteViewModel: MapRouteViewModel = viewModel(factory = MapRouteViewModel.Factory(application))
                    var currentScreen by remember { mutableStateOf("main") }

                    when (currentScreen) {
                        "main" -> {
                            MainScreen(
                                viewModel = viewModel,
                                onHistoryClick = { currentScreen = "history" },
                                onRouteClick = { currentScreen = "route" },
                                onConfigClick = { currentScreen = "config" },
                            )
                        }
                        "history" -> {
                            HistoryScreen(viewModel) {
                                currentScreen = "main"
                            }
                        }
                        "route" -> {
                            RouteScreen(
                                viewModel = routeViewModel,
                                onBack = { currentScreen = "main" },
                                onConfigClick = { currentScreen = "config" },
                                onShowRouteOnMap = { index ->
                                    routeViewModel.routes.value.getOrNull(index)?.let { r ->
                                        mapRouteViewModel.planRoute(
                                            originWgs = routeViewModel.location.value,
                                            waypointsGcj = r.waypoints.map {
                                                com.healthcard.amap.RoutePoint(it.lat, it.lon)
                                            },
                                            name = r.name,
                                        )
                                        currentScreen = "map"
                                    }
                                },
                            )
                        }
                        "config" -> {
                            ConfigScreen(configViewModel) {
                                currentScreen = "main"
                            }
                        }
                        "map" -> {
                            MapRouteScreen(mapRouteViewModel) {
                                currentScreen = "route"
                            }
                        }
                    }
                }
            }
        }
    }

    /** 请求通知权限（如需要）并启动前台保活服务。 */
    private fun ensureKeepAliveRunning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            KeepAliveService.start(this)
        }
    }
}
