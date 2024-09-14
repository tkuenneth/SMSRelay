package de.thomaskuenneth.smsrelay

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {

    private val flow = MutableStateFlow(false)
    private val permissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_MMS
    )
    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            flow.update { permissions.all { permission -> permission.value } }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissions()
        setContent {
            MaterialTheme {
                val granted by flow.collectAsState()
                Scaffold { innerPadding ->
                    MainScreen(paddingValues = innerPadding, granted = granted)
                }
            }
        }
    }

    private fun checkPermissions() {
        val granted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                this, permission
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
            flow.update { true }
        } else {
            launcher.launch(permissions)
        }
    }
}

@Composable
fun MainScreen(paddingValues: PaddingValues, granted: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(paddingValues = paddingValues), contentAlignment = Alignment.Center
    ) {
        if (!granted) {
            Text(
                text = stringResource(R.string.not_granted),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
