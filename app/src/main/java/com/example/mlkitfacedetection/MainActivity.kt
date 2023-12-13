package com.example.mlkitfacedetection

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.example.mlkitfacedetection.databinding.ActivityMainBinding
import com.example.mlkitfacedetection.facedetection.FaceDetectionActivity
import com.example.mlkitfacedetection.qrscanner.ScannerActivity
import com.example.mlkitfacedetection.qrscanner.ScannerActivity.Companion.startScanner
import com.google.mlkit.vision.barcode.common.Barcode

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val cameraPermission = android.Manifest.permission.CAMERA

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startScannerActivity()
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonOpenScanner.setOnClickListener {
            requestCameraAndStartScanner()
        }

        binding.buttonOpenFaceDetection.setOnClickListener {
            startActivity(FaceDetectionActivity::class.java)
        }
    }
    private fun startActivity(targetActivityClass: Class<*>) {
        if (isPermissionGranted(cameraPermission)) {
            val intent = Intent(this, targetActivityClass)
            startActivity(intent)
        } else {
            requestCameraPermission()
        }
    }
    private fun requestCameraAndStartScanner() {
        if (isPermissionGranted(cameraPermission)) {
            startScannerActivity()

        } else {
            requestCameraPermission()
        }

    }
    private fun startScannerActivity() {
        ScannerActivity.startScanner(this)
    }

    private fun requestCameraPermission() {
        when {
            shouldShowRequestPermissionRationale(cameraPermission) -> {
                cameraPermissionRequest {
                    openPermissionSetting()
                }
            }

            else -> {
                requestPermissionLauncher.launch(cameraPermission)
            }

        }
    }



}