package com.persidius.eos.aurora.ui.cameraScanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import io.sentry.Sentry


class BarcodeAnalyzer(private val onBarcode: (List<FirebaseVisionBarcode>) -> Unit) : ImageAnalysis.Analyzer {
    companion object {
        private const val ANALYSIS_INTERVAL = 200L
    }
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

      try {
        val mediaImage = imageProxy?.image
        val imageRotation = degreesToFirebaseRotation(degrees)
        if (mediaImage != null) {
          lastAnalysis = currentTimestamp
          val image = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
          val detector = FirebaseVision.getInstance().visionBarcodeDetector

          detector.detectInImage(image)
            .addOnSuccessListener { barcodes -> onBarcode(barcodes) }
            .addOnFailureListener {
                Sentry.capture(it)
            }
        }
      } catch(e: Exception) {
        // some error
        return
      }
    }
}