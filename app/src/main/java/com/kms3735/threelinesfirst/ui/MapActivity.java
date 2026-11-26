package com.kms3735.threelinesfirst.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kms3735.threelinesfirst.R;
import com.kms3735.threelinesfirst.util.KeepWords;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * 장소 지도: 안내문에 적힌 기관을 지도에 표시한다.
 * 같은 이름의 기관이 여러 곳이면 내 위치에서 가장 가까운 곳을 고른다.
 * 위치는 지도 표시에만 쓰고 AI로 보내거나 저장하지 않는다. 위치 권한을 거절해도 장소는 보여 준다.
 */
public class MapActivity extends AppCompatActivity {

    public static final String EXTRA_PLACE = "place";
    /** 안내문의 문의 전화. 있으면 "가기 전에 전화로 묻기" 버튼을 보여 준다 */
    public static final String EXTRA_PHONE = "phone";
    private static final String TAG = "MapActivity";
    private static final float ZOOM = 16f;

    private String place;
    private GoogleMap map;
    private TextView txtStatus;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> showPlace(hasLocationPermission()));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map);
        KeepWords.applyTo(findViewById(R.id.main));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        place = getIntent().getStringExtra(EXTRA_PLACE);
        if (place == null) place = "";
        ((TextView) findViewById(R.id.txtPlace)).setText(place);
        txtStatus = findViewById(R.id.txtStatus);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        String phone = getIntent().getStringExtra(EXTRA_PHONE);
        if (phone != null && !phone.isEmpty()) {
            View call = findViewById(R.id.btnCall);
            call.setVisibility(View.VISIBLE);
            call.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.no_app, Toast.LENGTH_LONG).show();
                }
            });
        }

        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        fragment.getMapAsync(googleMap -> {
            map = googleMap;
            map.getUiSettings().setZoomControlsEnabled(true);
            // 아래 둥근 판이 지도 아래쪽을 살짝 덮으므로 확대 버튼이 가리지 않게 띄운다
            map.setPadding(0, 0, 0, Math.round(24 * getResources().getDisplayMetrics().density));
            if (hasLocationPermission()) {
                showPlace(true);
            } else {
                locationPermission.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            }
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission") // 호출 전에 hasLocationPermission()으로 확인함
    private void showPlace(boolean canLocate) {
        if (!canLocate) {
            findPlace(null);
            return;
        }
        map.setMyLocationEnabled(true);
        LocationServices.getFusedLocationProviderClient(this).getLastLocation()
                .addOnCompleteListener(task -> findPlace(task.isSuccessful() ? task.getResult() : null));
    }

    /** 장소 이름을 좌표로 바꾸는 일은 네트워크를 쓰므로 백그라운드 스레드에서 한다 */
    private void findPlace(Location here) {
        txtStatus.setText(R.string.map_searching);
        new Thread(() -> {
            Address found = null;
            if (Geocoder.isPresent() && !place.isEmpty()) {
                try {
                    @SuppressWarnings("deprecation") // 비동기 버전은 API 33부터라 minSdk 26에서는 이 메서드를 쓴다
                    List<Address> list = new Geocoder(this, Locale.KOREA).getFromLocationName(place, 5);
                    found = nearest(list, here);
                } catch (IOException e) {
                    Log.w(TAG, "장소 검색 실패", e);
                }
            }
            Address result = found;
            uiHandler.post(() -> {
                if (!isDestroyed()) onFound(result, here);
            });
        }).start();
    }

    private static Address nearest(List<Address> list, Location here) {
        if (list == null || list.isEmpty()) return null;
        if (here == null) return list.get(0);
        Address best = null;
        float bestDistance = Float.MAX_VALUE;
        float[] d = new float[1];
        for (Address a : list) {
            Location.distanceBetween(here.getLatitude(), here.getLongitude(), a.getLatitude(), a.getLongitude(), d);
            if (d[0] < bestDistance) {
                bestDistance = d[0];
                best = a;
            }
        }
        return best;
    }

    private void onFound(Address address, Location here) {
        if (address == null) {
            txtStatus.setText(getString(R.string.map_not_found, place));
            if (here != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(here.getLatitude(), here.getLongitude()), ZOOM));
            }
            return;
        }
        LatLng at = new LatLng(address.getLatitude(), address.getLongitude());
        String line = address.getMaxAddressLineIndex() >= 0 ? address.getAddressLine(0) : "";
        map.addMarker(new MarkerOptions().position(at).title(place).snippet(line)).showInfoWindow();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(at, ZOOM));
        txtStatus.setText(line.isEmpty() ? getString(R.string.map_found) : KeepWords.of(line));
    }
}
