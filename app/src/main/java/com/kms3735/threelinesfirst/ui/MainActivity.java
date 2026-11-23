package com.kms3735.threelinesfirst.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kms3735.threelinesfirst.R;
import com.kms3735.threelinesfirst.data.AppDatabase;
import com.kms3735.threelinesfirst.data.NoticeItem;

import java.util.List;

/** ① 안내문 목록. 최근에 찍은 것부터 Room에서 읽어 보여 준다 */
public class MainActivity extends AppCompatActivity {

    private NoticeAdapter adapter;
    private RecyclerView noticeList;
    private TextView txtEmpty;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    /** 사진 선택기: 사용자가 고른 사진 한 장에만 접근하므로 저장소 권한이 필요 없다 */
    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) return;
                Intent intent = new Intent(this, CaptureActivity.class);
                intent.setData(uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            });

    /** 첫 실행 개인정보 동의. 동의하지 않으면 앱을 닫는다 */
    private final ActivityResultLauncher<Intent> consent =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) {
                    Toast.makeText(this, R.string.consent_needed, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        noticeList = findViewById(R.id.noticeList);
        txtEmpty = findViewById(R.id.txtEmpty);
        adapter = new NoticeAdapter(this, notice ->
                startActivity(new Intent(this, ResultActivity.class)
                        .putExtra(ResultActivity.EXTRA_NOTICE_ID, notice.id)));
        noticeList.setLayoutManager(new LinearLayoutManager(this));
        noticeList.setAdapter(adapter);

        findViewById(R.id.btnCapture).setOnClickListener(v ->
                startActivity(new Intent(this, CaptureActivity.class)));
        findViewById(R.id.btnImport).setOnClickListener(v ->
                pickPhoto.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        if (savedInstanceState == null && !ConsentActivity.isAgreed(this)) {
            consent.launch(new Intent(this, ConsentActivity.class));
        }
    }

    /** 요약 화면에서 체크하거나 지우고 돌아오면 바로 반영되도록 화면에 보일 때마다 다시 읽는다 */
    @Override
    protected void onResume() {
        super.onResume();
        new Thread(() -> {
            List<NoticeItem> items = AppDatabase.get(this).noticeDao().getList();
            uiHandler.post(() -> {
                if (isDestroyed()) return;
                adapter.setItems(items);
                txtEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                noticeList.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
            });
        }).start();
    }
}
