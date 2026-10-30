package com.kms3735.threelinesfirst.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.kms3735.threelinesfirst.privacy.PrivacyMasker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * ② 촬영 안내: 가이드 틀 위에서 촬영하고, 기기 안에서 글자를 읽어 결과를 보여 준다.
 * 사진 불러오기(Intent data)나 다른 앱의 공유(ACTION_SEND)로 들어오면 카메라를 건너뛴다.
 */
public class CaptureActivity extends AppCompatActivity {

    private static final String TAG = "CaptureActivity";

    private PreviewView preview;
    private View cameraGroup, resultGroup;
    private TextView txtResultTitle, txtResult;
    private Button btnShoot, btnRetake;

    /** 카메라로 시작했는지. 사진·공유로 들어왔으면 다시 찍기 대신 돌아가기 */
    private boolean fromCamera = true;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

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

    /** 공유로 바로 들어온 경우에도 동의를 먼저 받는다. 동의하면 받은 내용을 그대로 이어서 처리 */
    private final ActivityResultLauncher<Intent> consent =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    start();
                } else {
                    Toast.makeText(this, R.string.consent_needed, Toast.LENGTH_LONG).show();
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

        btnRetake = findViewById(R.id.btnRetake);

        btnShoot.setOnClickListener(v -> takePhoto());
        btnRetake.setOnClickListener(v -> {
            if (fromCamera) showCamera();
            else finish();
        });

        if (savedInstanceState != null) return;
        if (ConsentActivity.isAgreed(this)) {
            start();
        } else {
            consent.launch(new Intent(this, ConsentActivity.class));
        }
    }

    private void start() {
        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            receiveShare(intent);
            return;
        }
        if (intent.getData() != null) {
            importImage(intent.getData());
            return;
        }

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

    /** 다른 앱에서 공유받은 이미지 또는 글 */
    private void receiveShare(Intent intent) {
        String type = intent.getType();
        if (type != null && type.startsWith("image/")) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                importImage(uri);
                return;
            }
        }
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        fromCamera = false;
        if (text == null || text.trim().isEmpty()) {
            Toast.makeText(this, R.string.share_empty, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // 이미 글자로 된 안내문은 글자 인식 없이 그대로 쓴다
        showResult(text.trim(), R.string.result_shared_text);
    }

    /** 불러오거나 공유받은 사진을 내부 저장소로 복사한 뒤 글자를 읽는다 */
    private void importImage(Uri uri) {
        fromCamera = false;
        showLoading();
        photo = newPhotoFile();
        new Thread(() -> {
            boolean copied = copy(uri, photo);
            uiHandler.post(() -> {
                if (copied) {
                    recognize();
                } else {
                    Toast.makeText(this, R.string.import_error, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        }).start();
    }

    private boolean copy(Uri uri, File dest) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(dest)) {
            if (in == null) return false;
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return true;
        } catch (IOException | SecurityException e) {
            Log.e(TAG, "사진 복사 실패", e);
            return false;
        }
    }

    private File newPhotoFile() {
        // 원문 사진은 앱 내부 저장소에만 둔다
        File dir = new File(getFilesDir(), "photos");
        dir.mkdirs();
        return new File(dir, "notice_" + System.currentTimeMillis() + ".jpg");
    }

    private void showLoading() {
        cameraGroup.setVisibility(View.GONE);
        resultGroup.setVisibility(View.VISIBLE);
        txtResultTitle.setText(R.string.reading);
        txtResult.setText("");
        btnRetake.setVisibility(View.INVISIBLE);
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        btnShoot.setEnabled(false);
        btnShoot.setText(R.string.reading);

        photo = newPhotoFile();

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
        showResult(text, OcrHelper.isEnough(text) ? R.string.result_ok : R.string.result_too_little);
    }

    private void showResult(String text, int titleRes) {
        // 화면에도 AI로 보낼 글(개인정보를 가린 글)을 그대로 보여 준다. 가리기 전 원문은 저장하지 않는다
        PrivacyMasker.Result masked = PrivacyMasker.mask(text);
        String note = masked.count > 0
                ? getString(R.string.masked_count, masked.count)
                : getString(R.string.masked_none);
        txtResultTitle.setText(text.isEmpty() ? getString(titleRes) : getString(titleRes) + "\n" + note);
        txtResult.setText(text.isEmpty() ? getString(R.string.result_empty) : masked.text);
        cameraGroup.setVisibility(View.GONE);
        resultGroup.setVisibility(View.VISIBLE);
        btnRetake.setText(fromCamera ? R.string.retake : R.string.go_back);
        btnRetake.setVisibility(View.VISIBLE);
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
