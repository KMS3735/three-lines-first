package com.kms3735.threelinesfirst.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.kms3735.threelinesfirst.R;
import com.kms3735.threelinesfirst.ocr.OcrHelper;

import java.io.File;

/** ② 촬영 안내: 가이드 틀 위에서 촬영하고, 기기 안에서 글자를 읽어 결과를 보여 준다. */
public class CaptureActivity extends AppCompatActivity {

    private static final String TAG = "CaptureActivity";

    private PreviewView preview;
    private View cameraGroup, resultGroup;
    private TextView txtResultTitle, txtResult;
    private Button btnShoot;

    private ImageCapture imageCapture;
    private final OcrHelper ocr = new OcrHelper();
    private File photo;

    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_needed, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_capture);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        preview = findViewById(R.id.preview);
        cameraGroup = findViewById(R.id.cameraGroup);
        resultGroup = findViewById(R.id.resultGroup);
        txtResultTitle = findViewById(R.id.txtResultTitle);
        txtResult = findViewById(R.id.txtResult);
        btnShoot = findViewById(R.id.btnShoot);

        btnShoot.setOnClickListener(v -> takePhoto());
        findViewById(R.id.btnRetake).setOnClickListener(v -> showCamera());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview previewUseCase = new Preview.Builder().build();
                previewUseCase.setSurfaceProvider(preview.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase, imageCapture);
            } catch (Exception e) {
                Log.e(TAG, "카메라 시작 실패", e);
                Toast.makeText(this, R.string.camera_error, Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        btnShoot.setEnabled(false);
        btnShoot.setText(R.string.reading);

        // 원문 사진은 앱 내부 저장소에만 둔다
        File dir = new File(getFilesDir(), "photos");
        dir.mkdirs();
        photo = new File(dir, "notice_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(photo).build();
        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        recognize();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Log.e(TAG, "촬영 실패", e);
                        Toast.makeText(CaptureActivity.this, R.string.capture_error, Toast.LENGTH_LONG).show();
                        resetShootButton();
                    }
                });
    }

    private void recognize() {
        ocr.recognize(this, photo, new OcrHelper.Callback() {
            @Override
            public void onSuccess(String text) {
                Log.d(TAG, "인식 글자 수: " + text.length());
                showResult(text);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "글자 인식 실패", e);
                showResult("");
            }
        });
    }

    private void showResult(String text) {
        boolean enough = OcrHelper.isEnough(text);
        txtResultTitle.setText(enough ? R.string.result_ok : R.string.result_too_little);
        txtResult.setText(text.isEmpty() ? getString(R.string.result_empty) : text);
        cameraGroup.setVisibility(View.GONE);
        resultGroup.setVisibility(View.VISIBLE);
        resetShootButton();
        // 글자가 너무 적으면 요약으로 넘어가지 않는다 (요약 연결은 3단계)
    }

    private void showCamera() {
        if (photo != null) photo.delete();
        resultGroup.setVisibility(View.GONE);
        cameraGroup.setVisibility(View.VISIBLE);
    }

    private void resetShootButton() {
        btnShoot.setEnabled(true);
        btnShoot.setText(R.string.shoot);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ocr.close();
    }
}
