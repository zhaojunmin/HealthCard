package com.healthcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthcard.ui.MainScreen
import com.healthcard.viewmodel.HealthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val viewModel: HealthViewModel = viewModel(factory = HealthViewModel.Factory(application))
                    MainScreen(viewModel)
                }
            }
        }
    }
}
