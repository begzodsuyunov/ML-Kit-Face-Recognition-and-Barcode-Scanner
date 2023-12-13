package com.example.mlkitfacedetection.qrscanner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.mlkitfacedetection.CameraXViewModel
import com.example.mlkitfacedetection.databinding.ActivityScannerBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding

    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private var urlDialog: AlertDialog? = null

    private lateinit var imageAnalysis: ImageAnalysis

    private val cameraXViewModel = viewModels<CameraXViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
//        cameraProviderFuture = ProcessCameraProvider.getInstance(this)


        cameraXViewModel.value.processCameraProvider.observe(this) { provider ->
            processCameraProvider = provider
            bindCameraPreview()
            bindInputAnalyser()
        }

    }

    private fun bindInputAnalyser() {
        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        val cameraExecutor = Executors.newSingleThreadExecutor()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(barcodeScanner, imageProxy)
        }

        processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    showBarcodeInfo(barcodes.first())
                }
            }.addOnFailureListener {
                Log.e(TAG, it.message ?: it.toString())
            }.addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun showBarcodeInfo(barcode: Barcode) {
        when (barcode.valueType) {
            Barcode.TYPE_URL -> {
                binding.tvQrType.text = "URL"
                binding.tvQrContent.text = barcode.url?.url
                showUrlDialog(barcode.url?.url ?: "")



            }

            Barcode.TYPE_CONTACT_INFO -> {
                binding.tvQrType.text = "Contact"
                binding.tvQrContent.text = barcode.contactInfo.toString()

            }

            else -> {
                binding.tvQrType.text = "Other"
                binding.tvQrContent.text = barcode.rawValue.toString()
            }

        }

    }

    private fun bindCameraPreview() {
        cameraPreview = Preview.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        cameraPreview.setSurfaceProvider(binding.previewView.surfaceProvider)

        processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)
    }

    private fun showUrlDialog(url: String) {
        if (urlDialog == null || !urlDialog!!.isShowing) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Open URL")
                .setMessage("Do you want to open the following URL?\n\n$url")
                .setPositiveButton("Open") { _, _ ->
                    openUrl(url)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }

            urlDialog = builder.create()
            urlDialog?.show()
        }
    }
    private fun openUrl(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening URL: $url", e)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        urlDialog?.dismiss()

    }
    companion object {

        private val TAG = ScannerActivity::class.simpleName
        fun startScanner(context: Context) {
            Intent(context, ScannerActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }
}