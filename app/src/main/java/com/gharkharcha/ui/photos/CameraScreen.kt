package com.gharkharcha.ui.photos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gharkharcha.R
import com.gharkharcha.ui.theme.Dimens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import java.io.File
import java.util.UUID

/** Key under which the captured file path is handed back to the previous screen. */
const val CAMERA_RESULT_KEY = "camera_result"

/**
 * Full-screen back-camera capture. Asks for CAMERA permission only here. Calls [onCaptured]
 * with the absolute path of a JPEG in the cache (the caller compresses and attaches it).
 */
@Composable
fun CameraScreen(onBack: () -> Unit, onCaptured: (String) -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        askedOnce = true
    }
    LaunchedEffect(Unit) { if (!granted && !askedOnce) permission.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            CameraPreview(onCaptured)
        } else {
            PermissionPrompt(
                showSettings = askedOnce,
                onGrant = { permission.launch(Manifest.permission.CAMERA) },
                onSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp),
        ) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back), tint = Color.White)
        }
    }
}

@Composable
private fun CameraPreview(onCaptured: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var unavailable by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }

    LaunchedEffect(lifecycleOwner) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        val preview = Preview.Builder().build().apply { setSurfaceProvider { surfaceRequest = it } }
        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            awaitCancellation()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            unavailable = true
        } finally {
            provider.unbindAll()
        }
    }

    if (unavailable) {
        Text(
            stringResource(R.string.camera_unavailable),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxSize().padding(48.dp),
        )
        return
    }
    surfaceRequest?.let { CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize()) }

    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
            if (failed) Text(stringResource(R.string.camera_failed), color = Color.White)
            val label = stringResource(R.string.camera_capture)
            Box(
                Modifier
                    .size(76.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(8.dp)
                    .background(if (capturing) Color.White.copy(alpha = 0.5f) else Color.White, CircleShape)
                    .clickable(enabled = !capturing, role = Role.Button, onClickLabel = label) {
                        capturing = true
                        failed = false
                        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                        val file = File(dir, "${UUID.randomUUID()}.jpg")
                        imageCapture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    onCaptured(file.absolutePath)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    capturing = false
                                    failed = true
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (capturing) CircularProgressIndicator(Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PermissionPrompt(showSettings: Boolean, onGrant: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(R.drawable.ic_camera), contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        Text(stringResource(R.string.camera_permission_title), style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(
            stringResource(R.string.camera_permission_message),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        Button(onClick = onGrant) { Text(stringResource(R.string.camera_permission_grant)) }
        if (showSettings) {
            OutlinedButton(onClick = onSettings) { Text(stringResource(R.string.camera_permission_settings), color = Color.White) }
        }
    }
}
