@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)

package nl.cita.magister.ui.login

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import nl.cita.magister.pairing.LanAddresses
import nl.cita.magister.pairing.PairingOffer

@Composable
actual fun PairingScanScreen(
    onOffer: (PairingOffer) -> Unit,
    onClose: () -> Unit,
    busy: Boolean,
    error: String?,
) {
    val context = LocalContext.current
    var permission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permission = it
    }
    LaunchedEffect(Unit) {
        if (!permission) ask.launch(Manifest.permission.CAMERA)
    }
    var pending by remember { mutableStateOf<PairingOffer?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Koppel desktop") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sluiten")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Richt de camera op de QR-code op je pc. Jullie moeten op hetzelfde wifi zitten.",
                style = MaterialTheme.typography.bodyMedium,
            )
            when {
                !permission -> Text("Camera-toestemming is nodig om de QR-code te scannen.")
                busy -> Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text("Sessie wordt gestuurd…", Modifier.padding(top = 12.dp))
                }
                else -> QrCamera(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    onRaw = { raw ->
                        if (pending != null) return@QrCamera
                        val offer = PairingOffer.parse(raw)
                        when {
                            offer == null -> scanError = "Geen Mags-koppelcode."
                            offer.isExpired() -> scanError = "Deze QR-code is verlopen."
                            !LanAddresses.isPrivateLanIp(offer.ip) ->
                                scanError = "Alleen een privé-LAN-adres is toegestaan."
                            else -> {
                                scanError = null
                                pending = offer
                            }
                        }
                    },
                )
            }
            val shown = error ?: scanError
            if (shown != null) {
                Text(shown, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    val offer = pending
    if (offer != null && !busy) {
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Desktop koppelen?") },
            text = {
                Text(
                    "Sessie naar ${offer.ip}:${offer.port} sturen. " +
                        "Als Magister refresh-tokens roteert, kan één van de twee apparaten " +
                        "later opnieuw moeten inloggen.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chosen = offer
                        pending = null
                        onOffer(chosen)
                    },
                ) { Text("Koppelen") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Annuleren") }
            },
        )
    }
}

@Composable
private fun QrCamera(
    modifier: Modifier,
    onRaw: (String) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
    }
    val handled = remember { AtomicBoolean(false) }
    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener(
                {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor) { imageProxy ->
                        val media = imageProxy.image
                        if (media == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val image = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { codes ->
                                val value = codes.firstOrNull()?.rawValue
                                if (!value.isNullOrBlank() && handled.compareAndSet(false, true)) {
                                    onRaw(value)
                                    handled.set(false)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                },
                ContextCompat.getMainExecutor(ctx),
            )
            previewView
        },
    )
}
