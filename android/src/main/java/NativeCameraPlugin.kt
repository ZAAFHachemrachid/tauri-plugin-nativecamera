package com.plugin.nativecamera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import app.tauri.PermissionState
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.Permission
import app.tauri.annotation.PermissionCallback
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.io.ByteArrayOutputStream
import android.util.Base64
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix

@InvokeArg
class PreviewOptions {
    var windowed: Boolean = true
    var cameraDirection: String? = "back"
}

@TauriPlugin(
    permissions = [
        Permission(strings = [Manifest.permission.CAMERA], alias = "camera")
    ]
)
class NativeCameraPlugin(private val activity: Activity) : Plugin(activity) {
    private lateinit var webView: WebView
    private var previewView: PreviewView? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    private var requestPermissionResponse: JSObject? = null
    private var windowed = false
    private var webViewBackground: Drawable? = null

    override fun load(webView: WebView) {
        super.load(webView)
        this.webView = webView
    }

    private fun hasCamera(): Boolean {
        return activity.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
    }

    private fun setupCamera(cameraDirection: String, windowed: Boolean, invoke: Invoke) {
        activity.runOnUiThread {
            val pv = PreviewView(activity)
            pv.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            this.previewView = pv

            val parent = webView.parent as ViewGroup
            // Add PreviewView behind WebView (index 0)
            parent.addView(pv, 0)

            this.windowed = windowed
            if (windowed) {
                webView.bringToFront()
                webViewBackground = webView.background
                webView.setBackgroundColor(Color.TRANSPARENT)
            }

            val future = ProcessCameraProvider.getInstance(activity)
            future.addListener(
                {
                    try {
                        val provider = future.get()
                        val lensFacing = if (cameraDirection == "front") CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        bindPreview(provider, lensFacing)
                        this.cameraProvider = provider
                        invoke.resolve()
                    } catch (e: Exception) {
                        invoke.reject("Failed to bind camera: ${e.message}")
                    }
                },
                ContextCompat.getMainExecutor(activity)
            )
            this.cameraProviderFuture = future
        }
    }

    private fun bindPreview(provider: ProcessCameraProvider, cameraDirection: Int) {
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraDirection).build()
        preview.setSurfaceProvider(previewView?.surfaceProvider)

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        provider.unbindAll()
        provider.bindToLifecycle(
            activity as LifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
    }

    private fun dismantleCamera() {
        activity.runOnUiThread {
            if (cameraProvider != null) {
                cameraProvider?.unbindAll()
                val parent = webView.parent as ViewGroup
                parent.removeView(previewView)
                previewView = null
                imageCapture = null
                cameraProvider = null
            }
            if (windowed) {
                if (webViewBackground != null) {
                    webView.background = webViewBackground
                    webViewBackground = null
                } else {
                    webView.setBackgroundColor(Color.WHITE)
                }
            }
        }
    }

    @Command
    fun startPreview(invoke: Invoke) {
        val args = invoke.parseArgs(PreviewOptions::class.java)

        if (hasCamera()) {
            if (getPermissionState("camera") != PermissionState.GRANTED) {
                invoke.reject("No permission to use camera. Request it first.")
            } else {
                webViewBackground = null
                dismantleCamera() // Ensure clean state
                setupCamera(args.cameraDirection ?: "back", args.windowed, invoke)
            }
        } else {
            invoke.reject("No camera available")
        }
    }

    @Command
    fun stopPreview(invoke: Invoke) {
        dismantleCamera()
        invoke.resolve()
    }

    @Command
    fun takePicture(invoke: Invoke) {
        val capture = imageCapture
        if (capture == null) {
            invoke.reject("Camera is not running")
            return
        }

        capture.takePicture(
            ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)

                        // Convert JPEG bytes to Bitmap
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        
                        // Handle rotation
                        val rotation = image.imageInfo.rotationDegrees.toFloat()
                        val finalBitmap = if (rotation != 0f) {
                            val matrix = Matrix()
                            matrix.postRotate(rotation)
                            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        } else {
                            bitmap
                        }

                        val outputStream = ByteArrayOutputStream()
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                        val finalBytes = outputStream.toByteArray()

                        val base64 = Base64.encodeToString(finalBytes, Base64.NO_WRAP)
                        val jsObject = JSObject()
                        jsObject.put("photo", "data:image/jpeg;base64,$base64")

                        image.close()
                        invoke.resolve(jsObject)
                    } catch (e: Exception) {
                        image.close()
                        invoke.reject("Failed to process image: ${e.message}")
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    invoke.reject("Capture failed: ${exception.message}")
                }
            }
        )
    }

    @SuppressLint("ObsoleteSdkInt")
    @PermissionCallback
    fun cameraPermissionCallback(invoke: Invoke) {
        if (requestPermissionResponse == null) return
        val res = requestPermissionResponse!!

        if (getPermissionState("camera") == PermissionState.GRANTED) {
            res.put("camera", PermissionState.GRANTED)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                res.put("camera", PermissionState.DENIED)
            } else {
                res.put("camera", PermissionState.GRANTED)
            }
        }

        invoke.resolve(res)
        this.requestPermissionResponse = null
    }

    @SuppressLint("ObsoleteSdkInt")
    @Command
    override fun requestPermissions(invoke: Invoke) {
        val res = JSObject()
        this.requestPermissionResponse = res
        if (getPermissionState("camera") == PermissionState.GRANTED) {
            res.put("camera", PermissionState.GRANTED)
            invoke.resolve(res)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissionForAlias("camera", invoke, "cameraPermissionCallback")
            } else {
                res.put("camera", PermissionState.GRANTED)
                invoke.resolve(res)
            }
        }
    }

    @Command
    override fun checkPermissions(invoke: Invoke) {
        val res = JSObject()
        if (getPermissionState("camera") == PermissionState.GRANTED) {
            res.put("camera", PermissionState.GRANTED)
        } else {
            res.put("camera", PermissionState.PROMPT)
        }
        invoke.resolve(res)
    }
}
