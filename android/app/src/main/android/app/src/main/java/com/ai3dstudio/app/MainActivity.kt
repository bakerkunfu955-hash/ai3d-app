package com.ai3dstudio.app

import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    
    private var imageUri by mutableStateOf<Uri?>(null)
    
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0A0F24),
                    primary = Color(0xFF00E5FF),
                    secondary = Color(0xFF8A2BE2)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }

    @Composable
    fun MainScreen() {
        var statusText by remember { mutableStateOf("Select an image to begin") }
        var isProcessing by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("AI3D Reality Studio", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))

            imageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Preview",
                    modifier = Modifier.size(200.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text("Gallery")
                }
                
                Button(
                    enabled = imageUri != null && !isProcessing,
                    onClick = {
                        isProcessing = true
                        statusText = "Uploading..."
                    }
                ) {
                    Text("Generate 3D")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(statusText, color = MaterialTheme.colorScheme.primary)
        }
    }
}
