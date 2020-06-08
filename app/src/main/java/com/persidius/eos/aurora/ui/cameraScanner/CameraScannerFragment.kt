package com.persidius.eos.aurora.ui.cameraScanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.ui.recipient.RecipientFragment
import java.util.concurrent.Executors

class CameraScannerFragment : Fragment() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView

    companion object {
        /**
         * Pop self from nav stack
         */
        const val ARG_POP_NAV = "popNav"

        private const val REQUEST_CODE_PERMISSIONS = 1
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private var popNav: Boolean = false
    private var debounce = false
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
            updateTransform()
        }

        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()

        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(executor, BarcodeAnalyzer { barcodes -> onBarcode(barcodes) })
        }

        CameraX.bindToLifecycle(this, preview, analyzerUseCase)
    }


    private fun onBarcode(barcodes: List<FirebaseVisionBarcode>) {
        // rtn on debounce
        if(debounce || barcodes.isEmpty()) { return }

        debounce = true
        if(barcodes.size > 1) {
            // show dialogue w/ warning
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Multiple coduri")
            builder.setMessage("Au fost scanate multiple coduri. Pentru a evita ambiguitatea, asigura-te ca exista o singura eticheta in vederea camerei")
            builder.setNeutralButton("Am Ințeles" ) { dialog, which ->
                dialog.dismiss()
                debounce = false
            }
            builder.show()
        }

        if(barcodes.size == 1) {
            // nav to next destination
            val navController = (activity as MainActivity).navController
            val args = Bundle()
            val code = barcodes[0].rawValue?.split(":")?.last()
            args.apply {
                putString(RecipientFragment.ARG_RECIPIENT_ID, code)
            }

            // CameraX.unbindAll()
           if(popNav) {
                navController.popBackStack(R.id.nav_cameraScanner, true)
            }

            navController.navigate(R.id.nav_recipient, args)
        }
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
        popNav = arguments?.getBoolean(ARG_POP_NAV, false) ?: false
        debounce = false
        val root = inflater.inflate(R.layout.fragment_camera_scanner, container, false)
        viewFinder = root.findViewById(R.id.viewFinder)

        if(allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        viewFinder.addOnLayoutChangeListener {_, _, _, _, _, _, _, _, _ -> updateTransform() }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        CameraX.unbindAll()
        Log.d("CAMERA", "VIEW DESTROYED")
    }
}