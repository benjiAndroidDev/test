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
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Rounded.FlashOn, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                Spacer(Modifier.height(16.dp))
                Text("L'accès à la caméra est nécessaire pour scanner les produits.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Autoriser la caméra")
                }
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
    val haptic = LocalHapticFeedback.current
    
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13, 
            Barcode.FORMAT_EAN_8, 
            Barcode.FORMAT_UPC_A, 
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39
        )
        .build()
    val scanner = remember { BarcodeScanning.getClient(options) }
    val api = remember { OpenFoodFactsApi.create() }
    
    var scannedProduct by remember { mutableStateOf<Product?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (scannedProduct == null && !isProcessing && errorMessage == null) {
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                barcodes.firstOrNull()?.rawValue?.let { code ->
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    isProcessing = true
                                                    scope.launch {
                                                        try {
                                                            Log.d("Scan", "Code détecté: $code")
                                                            val response = api.getProductByBarcode(code)
                                                            if (response.product != null) {
                                                                scannedProduct = response.product
                                                                scanHistoryManager.addToHistory(response.product)
                                                            } else {
                                                                errorMessage = "Produit non référencé ($code)"
                                                                delay(3000)
                                                                errorMessage = null
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("Scan", "API Error", e)
                                                            errorMessage = "Erreur réseau ou serveur"
                                                            delay(3000)
                                                            errorMessage = null
                                                        } finally {
                                                            isProcessing = false
                                                        }
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
                        val cam = cameraProvider.bindToLifecycle(
                            lifecycleOwner, 
                            CameraSelector.DEFAULT_BACK_CAMERA, 
                            preview, 
                            imageAnalysis
                        )
                        cameraControl = cam.cameraControl
                        // Auto-focus continu
                        cam.cameraControl.setLinearZoom(0f)
                    } catch (e: Exception) {
                        Log.e("Scan", "Binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Overlay style Yuka
        YukaOverlay(isProcessing)

        // Flash Button
        IconButton(
            onClick = { 
                isTorchOn = !isTorchOn
                cameraControl?.enableTorch(isTorchOn)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp)
                .background(Color.Black.copy(0.4f), CircleShape)
        ) {
            Icon(
                if (isTorchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                contentDescription = "Flash",
                tint = Color.White
            )
        }

        // Center Loading
        if (isProcessing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00C853), strokeWidth = 6.dp, modifier = Modifier.size(64.dp))
            }
        }

        // Error message popup
        if (errorMessage != null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp, start = 32.dp, end = 32.dp),
                color = Color.Black.copy(0.8f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    errorMessage!!,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

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

@Composable
fun YukaOverlay(isProcessing: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val boxWidth = width * 0.85f
        val boxHeight = boxWidth * 0.5f
        val left = (width - boxWidth) / 2
        val top = (height - boxHeight) / 2
        
        val rect = Rect(left, top, left + boxWidth, top + boxHeight)

        // Background assombri avec trou au milieu
        drawPath(
            Path().apply {
                addRect(Rect(0f, 0f, width, height))
                addRoundRect(RoundRect(rect, CornerRadius(24.dp.toPx())))
                fillType = PathFillType.EvenOdd
            },
            color = Color.Black.copy(alpha = 0.5f)
        )

        // Coins blancs
        val lineLength = 40.dp.toPx()
        val strokeWidth = 5.dp.toPx()
        val color = if (isProcessing) Color(0xFF00C853) else Color.White

        // Top Left
        drawLine(color, Offset(left, top + lineLength), Offset(left, top), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(left, top), Offset(left + lineLength, top), strokeWidth, StrokeCap.Round)
        
        // Top Right
        drawLine(color, Offset(left + boxWidth - lineLength, top), Offset(left + boxWidth, top), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(left + boxWidth, top), Offset(left + boxWidth, top + lineLength), strokeWidth, StrokeCap.Round)
        
        // Bottom Left
        drawLine(color, Offset(left, top + boxHeight - lineLength), Offset(left, top + boxHeight), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(left, top + boxHeight), Offset(left + lineLength, top + boxHeight), strokeWidth, StrokeCap.Round)
        
        // Bottom Right
        drawLine(color, Offset(left + boxWidth - lineLength, top + boxHeight), Offset(left + boxWidth, top + boxHeight), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth, top + boxHeight - lineLength), strokeWidth, StrokeCap.Round)

        // Ligne de scan
        if (!isProcessing) {
            drawLine(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF00C853).copy(0.6f), Color.Transparent)),
                start = Offset(left + 15.dp.toPx(), top + (boxHeight * scanLineY)),
                end = Offset(left + boxWidth - 15.dp.toPx(), top + (boxHeight * scanLineY)),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}
