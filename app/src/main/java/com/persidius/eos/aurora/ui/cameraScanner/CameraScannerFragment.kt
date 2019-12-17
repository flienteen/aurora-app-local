package com.persidius.eos.aurora.ui.cameraScanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.view.TextureViewMeteringPointFactory
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.persidius.eos.aurora.R
import java.util.concurrent.Executors

private const val REQUEST_CODE_PERMISSIONS = 1
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class CameraScannerFragment : Fragment() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView

    private fun startCamera() {
        val targetHeight = 1280
        val targetWidth = (viewFinder.width.toFloat() / viewFinder.height.toFloat() * targetHeight).toInt()
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(targetWidth,targetHeight))
        }.build()

        val preview = Preview(previewConfig)

        preview.setOnPreviewOutputUpdateListener {

            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            viewFinder.setOnTouchListener { v, e ->  onTouchToFocus(v, e) }
            updateTransform()
        }

        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()

        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(executor, BarcodeAnalyzer())
        }

        CameraX.bindToLifecycle(this, preview, analyzerUseCase)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        viewFinder.setTransform(matrix)
    }

    private fun onTouchToFocus(v: View, e: MotionEvent): Boolean {
        Log.d("camera","Touch focus")
        return when(e.action) {
            MotionEvent.ACTION_UP -> {
                val pointFactory =  TextureViewMeteringPointFactory(viewFinder)
                val point = pointFactory.createPoint(e.x, e.y)

                val action = FocusMeteringAction.Builder.from(point, FocusMeteringAction.MeteringMode.AF_AE_AWB)
                action.setAutoCancelDuration(5, java.util.concurrent.TimeUnit.SECONDS)

                CameraX.getCameraControl(CameraX.LensFacing.BACK).startFocusAndMetering(action.build())
                true
            }
            else -> false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == REQUEST_CODE_PERMISSIONS) {
            if(allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(context,
                    "Permisiunea de a accesa camera a fost refuzata",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        val baseContext = activity?.baseContext ?: return false
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_camera_scanner, container, false)
        viewFinder = root.findViewById(R.id.viewFinder)

        if(allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(activity!!, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        viewFinder.addOnLayoutChangeListener {_, _, _, _, _, _, _, _, _ -> updateTransform() }

        return root
    }
}


private const val ANALYSIS_INTERVAL = 200L

private class BarcodeAnalyzer : ImageAnalysis.Analyzer {
    private var lastAnalysis = 0L
    private fun degreesToFirebaseRotation(degrees: Int): Int = when(degrees) {
        0 -> FirebaseVisionImageMetadata.ROTATION_0
        90 -> FirebaseVisionImageMetadata.ROTATION_90
        180 -> FirebaseVisionImageMetadata.ROTATION_180
        270 -> FirebaseVisionImageMetadata.ROTATION_270
        else -> throw Exception("Rotation is not 0, 90, 180 or 270")
    }

    override fun analyze(imageProxy: ImageProxy?, degrees: Int) {
        val currentTimestamp = System.currentTimeMillis()

        if(currentTimestamp - lastAnalysis < ANALYSIS_INTERVAL) {
            return
        }

        val mediaImage = imageProxy?.image
        val imageRotation = degreesToFirebaseRotation(degrees)
        if(mediaImage != null) {
            lastAnalysis = currentTimestamp
            val image = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
            val detector = FirebaseVision.getInstance().visionBarcodeDetector

            detector.detectInImage(image)
            .addOnSuccessListener { barcodes ->
                Log.d("barcode","Barcode ${barcodes.size}")
                if(barcodes.size > 0) {
                    for(barcode in barcodes) {
                        Log.d("barcode", "Value: ${barcode.rawValue}, type: ${barcode.format}")
                    }
                }
            }
            .addOnFailureListener {

            }
        }
    }
}