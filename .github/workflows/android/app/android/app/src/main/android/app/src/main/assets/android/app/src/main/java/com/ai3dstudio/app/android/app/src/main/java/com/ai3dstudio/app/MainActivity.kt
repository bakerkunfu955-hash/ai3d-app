package com.ai3dstudio.app

import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

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
        val coroutineScope = rememberCoroutineScope()
        var statusText by remember { mutableStateOf("Select an image to begin") }
        var progress by remember { mutableStateOf(0) }
        var modelUrl by remember { mutableStateOf<String?>(null) }
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
                        coroutineScope.launch {
                            try {
                                val file = uriToFile(imageUri!!)
                                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
                                
                                val response = RetrofitClient.api.uploadImage(body)
                                pollStatus(response.taskId) { url ->
                                    modelUrl = url
                                    isProcessing = false
                                }
                            } catch (e: Exception) {
                                statusText = "Error: ${e.message}"
                                isProcessing = false
                            }
                        }
                    }
                ) {
                    Text("Generate 3D")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(statusText, color = MaterialTheme.colorScheme.primary)
            if (isProcessing) {
                LinearProgressIndicator(progress = progress / 100f, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }

            modelUrl?.let { url ->
                Spacer(modifier = Modifier.height(16.dp))
                ModelViewer(url)
            }
        }
        
        suspend fun pollStatus(taskId: String, onComplete: (String) -> Unit) {
            while (true) {
                try {
                    val statusResponse = RetrofitClient.api.checkStatus(taskId)
                    progress = statusResponse.progress
                    statusText = "Status: ${statusResponse.status} ($progress%)"
                    
                    if (statusResponse.status == "SUCCEEDED" && statusResponse.modelUrl != null) {
                        onComplete(statusResponse.modelUrl)
                        break
                    } else if (statusResponse.status == "FAILED") {
                        statusText = "Generation Failed: ${statusResponse.error}"
                        isProcessing = false
                        break
                    }
                } catch (e: Exception) {
                    statusText = "Polling Error. Retrying..."
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    @Composable
    fun ModelViewer(url: String) {
        AndroidView(factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadUrl("file:///android_asset/viewer.html")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, webUrl: String?) {
                        evaluateJavascript("loadModel('$url');", null)
                    }
                }
            }
        }, modifier = Modifier.fillMaxSize())
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "temp_image.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        return file
    }
}
