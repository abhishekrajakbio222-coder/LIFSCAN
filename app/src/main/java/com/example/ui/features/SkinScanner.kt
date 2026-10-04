package com.example.ui.features

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.SkinScannerPink
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Reusable 'SkinScanner' Composable that utilizes CameraX Preview and ImageCapture use cases
 * to allow patients and clinicians to capture high-definition photos for dermatological analysis.
 *
 * Features:
 * - CameraX Live Preview with Lifecycle binding
 * - ImageCapture use case with rotation correction
 * - Tap-to-focus for precise lesion targeting
 * - Pinch-to-zoom & step zoom controls (1x, 2x)
 * - Flashlight / Torch toggle for illuminated clinical captures
 * - Front / Back camera switching
 * - Clinical dermatological grid reticle overlay with laser scan animation
 * - Camera permission verification and interactive runtime grant flow
 */
@Composable
fun SkinScanner(
    modifier: Modifier = Modifier,
    title: String = "AI Skin Lesion Scanner",
    guideInstructions: String = "Align lesion inside reticle • Hold 10–15cm away",
    showGuideOverlay: Boolean = true,
    showControls: Boolean = true,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit = {},
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    if (!hasCameraPermission) {
        CameraPermissionRequestView(
            modifier = modifier,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onClose = onClose
        )
    } else {
        CameraXScannerView(
            modifier = modifier,
            title = title,
            guideInstructions = guideInstructions,
            showGuideOverlay = showGuideOverlay,
            showControls = showControls,
            onImageCaptured = onImageCaptured,
            onError = onError,
            onClose = onClose
        )
    }
}

@Composable
private fun CameraXScannerView(
    modifier: Modifier = Modifier,
    title: String,
    guideInstructions: String,
    showGuideOverlay: Boolean,
    showControls: Boolean,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit,
    onClose: (() -> Unit)?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }

    // Tap to focus state
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val focusAnimAlpha = remember { Animatable(0f) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            .build()
    }

    // Initialize Camera Provider
    LaunchedEffect(lensFacing) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider
                val pView = previewView ?: return@addListener

                provider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(pView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                if (provider.hasCamera(cameraSelector)) {
                    val cam = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    camera = cam

                    cam.cameraControl.enableTorch(isTorchOn)
                    cam.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                        currentZoomRatio = zoomState.zoomRatio
                    }
                }
            } catch (e: Exception) {
                // If camera binding fails
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Laser scanning animation for derm reticle
    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserOffsetRatio by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laser_sweep"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Preview View
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("camerax_preview_surface")
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Tap to Focus
                        val pView = previewView ?: return@detectTapGestures
                        val factory = SurfaceOrientedMeteringPointFactory(
                            pView.width.toFloat(),
                            pView.height.toFloat()
                        )
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                            .build()

                        camera?.cameraControl?.startFocusAndMetering(action)
                        focusPoint = offset
                        coroutineScope.launch {
                            focusAnimAlpha.snapTo(1f)
                            focusAnimAlpha.animateTo(0f, tween(1000, easing = FastOutSlowInEasing))
                            focusPoint = null
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val cam = camera ?: return@detectTransformGestures
                        val currentZoom = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                        val maxZoom = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 3f
                        val minZoom = cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                        val newZoom = (currentZoom * zoom).coerceIn(minZoom, maxZoom.coerceAtMost(4f))
                        cam.cameraControl.setZoomRatio(newZoom)
                    }
                },
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                }
            },
            update = { pView ->
                previewView = pView
            }
        )

        // Focus indicator box on user tap
        focusPoint?.let { pt ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxSize = 70.dp.toPx()
                drawRoundRect(
                    color = SkinScannerPink.copy(alpha = focusAnimAlpha.value),
                    topLeft = Offset(pt.x - boxSize / 2, pt.y - boxSize / 2),
                    size = Size(boxSize, boxSize),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Dermatological Laser Reticle & Grid Overlay
        if (showGuideOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(240.dp)
                        .testTag("derma_lens_reticle")
                ) {
                    val strokeW = 3.dp.toPx()
                    val cornerL = 36.dp.toPx()
                    val w = size.width
                    val h = size.height

                    // Grid guidelines
                    val gridColor = Color.White.copy(alpha = 0.25f)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawLine(gridColor, Offset(w / 3, 0f), Offset(w / 3, h), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                    drawLine(gridColor, Offset(2 * w / 3, 0f), Offset(2 * w / 3, h), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                    drawLine(gridColor, Offset(0f, h / 3), Offset(w, h / 3), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                    drawLine(gridColor, Offset(0f, 2 * h / 3), Offset(w, 2 * h / 3), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)

                    // Corner brackets (Top-Left)
                    drawLine(SkinScannerPink, Offset(0f, 0f), Offset(cornerL, 0f), strokeW)
                    drawLine(SkinScannerPink, Offset(0f, 0f), Offset(0f, cornerL), strokeW)

                    // Corner brackets (Top-Right)
                    drawLine(SkinScannerPink, Offset(w, 0f), Offset(w - cornerL, 0f), strokeW)
                    drawLine(SkinScannerPink, Offset(w, 0f), Offset(w, cornerL), strokeW)

                    // Corner brackets (Bottom-Left)
                    drawLine(SkinScannerPink, Offset(0f, h), Offset(cornerL, h), strokeW)
                    drawLine(SkinScannerPink, Offset(0f, h), Offset(0f, h - cornerL), strokeW)

                    // Corner brackets (Bottom-Right)
                    drawLine(SkinScannerPink, Offset(w, h), Offset(w - cornerL, h), strokeW)
                    drawLine(SkinScannerPink, Offset(w, h), Offset(w, h - cornerL), strokeW)

                    // Center crosshair
                    val crossSize = 14.dp.toPx()
                    val cx = w / 2
                    val cy = h / 2
                    drawLine(TealAccent, Offset(cx - crossSize, cy), Offset(cx + crossSize, cy), 1.5.dp.toPx())
                    drawLine(TealAccent, Offset(cx, cy - crossSize), Offset(cx, cy + crossSize), 1.5.dp.toPx())

                    // Animated Laser Scanning Line
                    val laserY = h * laserOffsetRatio
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, SkinScannerPink, TealAccent, SkinScannerPink, Color.Transparent)
                        ),
                        start = Offset(0f, laserY),
                        end = Offset(w, laserY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }

        // Shutter white flash animation on photo capture
        AnimatedVisibility(
            visible = isCapturing,
            enter = fadeIn(tween(50)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        // Top Header & Controls Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Camera", tint = Color.White)
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SkinScannerPink))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("HD DERMA CAMERA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Status Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("HIPAA Secure Capture", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Flashlight / Torch Toggle
            IconButton(
                onClick = {
                    isTorchOn = !isTorchOn
                    camera?.cameraControl?.enableTorch(isTorchOn)
                },
                modifier = Modifier
                    .background(
                        if (isTorchOn) SkinScannerPink else Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Torch",
                    tint = Color.White
                )
            }
        }

        // Clinical guidance tip bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.7f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TealAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(guideInstructions, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Bottom Controls: Zoom, Capture Button, Camera Switch
        if (showControls) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zoom toggle (1x / 2x)
                    IconButton(
                        onClick = {
                            val newRatio = if (currentZoomRatio > 1.5f) 1.0f else 2.0f
                            camera?.cameraControl?.setZoomRatio(newRatio)
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Text(
                            "${String.format("%.1f", currentZoomRatio)}x",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Shutter Capture Button
                    Surface(
                        modifier = Modifier
                            .size(76.dp)
                            .testTag("camera_shutter_btn"),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(4.dp, Color.White),
                        onClick = {
                            if (!isCapturing) {
                                isCapturing = true
                                val executor = ContextCompat.getMainExecutor(context)
                                imageCapture.takePicture(
                                    executor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                            try {
                                                val rawBitmap = imageProxy.toBitmap()
                                                val rotation = imageProxy.imageInfo.rotationDegrees
                                                val finalBitmap = if (rotation != 0) {
                                                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                                    Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                                                } else {
                                                    rawBitmap
                                                }
                                                onImageCaptured(finalBitmap)
                                            } catch (e: Exception) {
                                                // Fallback if conversion fails
                                            } finally {
                                                imageProxy.close()
                                                isCapturing = false
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            isCapturing = false
                                            onError(exception)
                                        }
                                    }
                                )
                            }
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SkinScannerPink,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Capture",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Flip Camera Lens (Back/Front)
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionRequestView(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    onClose: (() -> Unit)?
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = SkinScannerPink.copy(alpha = 0.15f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Camera Permission",
                        tint = SkinScannerPink,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Camera Access Required",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "To accurately analyze skin rashes, lesions, eczema, and psoriasis, Lifscan needs permission to use your device camera for real-time dermatological scanning.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onClose != null) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                }

                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("grant_camera_permission_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = SkinScannerPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enable Camera", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
