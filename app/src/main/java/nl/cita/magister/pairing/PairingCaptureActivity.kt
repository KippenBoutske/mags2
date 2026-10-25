package nl.cita.magister.pairing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import nl.cita.magister.platform.AndroidHooks

/**
 * Camera lives here, not in the Compose tree. Embedding CameraX / ML Kit /
 * Activity Result in PairingScanScreen crashed on Pixel 10 (SDK 37) during
 * recomposition after Meer was torn down.
 */
class PairingCaptureActivity : ComponentActivity() {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val handled = AtomicBoolean(false)
    private var provider: ProcessCameraProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CAMERA) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera-toestemming is nodig.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun startCamera() {
        val previewView = PreviewView(this)
        setContentView(
            FrameLayout(this).apply {
                addView(
                    previewView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            },
        )
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                runCatching {
                    val camera = future.get()
                    provider = camera
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
                                    AndroidHooks.pairingScanResult?.invoke(value)
                                    finish()
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                    camera.unbindAll()
                    camera.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }.onFailure { error ->
                    Toast.makeText(this, error.message ?: "Camera kon niet starten.", Toast.LENGTH_LONG).show()
                    finish()
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    override fun onDestroy() {
        runCatching { provider?.unbindAll() }
        executor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_CAMERA = 71
    }
}
