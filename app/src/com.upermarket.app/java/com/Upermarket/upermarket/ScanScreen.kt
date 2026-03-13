package com.Upermarket.upermarket

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    cartViewModel: CartViewModel,
    favoritesViewModel: FavoritesViewModel,
    scanHistoryManager: ScanHistoryManager
) {
    val context = LocalContext.current
    var hasCamPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamPermission = it }

    LaunchedEffect(Unit) {
        if (!hasCamPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasCamPermission) {
        ScannerContent(cartViewModel, favoritesViewModel, scanHistoryManager)
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("Autoriser la caméra")
            }
        }
    }
}

@Composable
fun ScannerContent(
    cartViewModel: CartViewModel,
    favoritesViewModel: FavoritesViewModel,
    scanHistoryManager: ScanHistoryManager
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val api = remember { OpenFoodFactsApi.create() }
    
    var scannedProduct by remember { mutableStateOf<Product?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (scannedProduct == null && !isProcessing) {
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                barcodes.firstOrNull()?.rawValue?.let { code ->
                                                    isProcessing = true
                                                    scope.launch {
                                                        try {
                                                            val response = api.getProductByBarcode(code)
                                                            response.product?.let { product ->
                                                                scannedProduct = product
                                                                scanHistoryManager.addToHistory(product)
                                                            }
                                                        } finally { isProcessing = false }
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    } else { imageProxy.close() }
                                } else { imageProxy.close() }
                            }
                        }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                    } catch (e: Exception) { Log.e("Scan", "Error", e) }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Viseur
        Box(Modifier.fillMaxWidth(0.7f).height(200.dp).align(Alignment.Center).background(Color.White.copy(0.1f), RoundedCornerShape(24.dp)))

        if (scannedProduct != null) {
            ProductDetailSheet(
                product = scannedProduct!!,
                isFavorite = favoritesViewModel.isFavorite(scannedProduct!!),
                onToggleFavorite = { favoritesViewModel.toggleFavorite(scannedProduct!!) },
                onAddToCart = { price ->
                    cartViewModel.addToCart(scannedProduct!!, price)
                    scannedProduct = null
                },
                onDismiss = { scannedProduct = null }
            )
        }
    }
}
