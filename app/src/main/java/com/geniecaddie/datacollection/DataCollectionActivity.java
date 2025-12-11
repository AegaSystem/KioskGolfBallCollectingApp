package com.geniecaddie.datacollection;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.geniecaddie.KioskGolfBallCollectingApp.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

/**
 * 데이터 수집 Activity
 * - 18개 카메라 (9홀 x White/Lady) 전환
 * - 실시간 영상 표시
 * - 스냅샷 캡처
 */
public class DataCollectionActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "DataCollection";

    // NetSDK 라이브러리 로드 (Activity 시작 시 필수)
    static {
        try {
            Log.d("DataCollectionActivity", "=== 라이브러리 로드 시작 ===");

            // C++ 런타임 라이브러리
            System.loadLibrary("c++_shared");
            Log.d("DataCollectionActivity", "✅ c++_shared 로드 성공");

            // NetSDK 관련 라이브러리들
            System.loadLibrary("netsdk");
            Log.d("DataCollectionActivity", "✅ netsdk 로드 성공");

            System.loadLibrary("jninetsdk");
            Log.d("DataCollectionActivity", "✅ jninetsdk 로드 성공");

            // 암호화 라이브러리
            System.loadLibrary("ssl");
            Log.d("DataCollectionActivity", "✅ ssl 로드 성공");

            System.loadLibrary("crypto");
            Log.d("DataCollectionActivity", "✅ crypto 로드 성공");

            // PlaySDK 라이브러리
            System.loadLibrary("play");
            Log.d("DataCollectionActivity", "✅ play 로드 성공");

            System.loadLibrary("jniplay");
            Log.d("DataCollectionActivity", "✅ jniplay 로드 성공");

            Log.d("DataCollectionActivity", "=== 모든 라이브러리 로드 완료 ===");
        } catch (UnsatisfiedLinkError e) {
            Log.e("DataCollectionActivity", "❌ 라이브러리 로드 실패: " + e.getMessage());
            Log.e("DataCollectionActivity", "라이브러리 파일 존재 여부와 ABI 호환성을 확인하세요");
        } catch (Exception e) {
            Log.e("DataCollectionActivity", "❌ 라이브러리 로드 중 예외: " + e.getMessage());
        }
    }


    // UI 컴포넌트
    private SurfaceView surfaceView;
    private Button btnSnapshot;
    private Button[] holeButtons;

    // 카메라 연결 매니저
    private CameraConnectionManager connectionManager;
    private boolean isSurfaceReady = false;
    private int captureCount = 0;

    /**
     * NetSDK 초기화 상태 확인
     */
    private void checkNetSDKStatus() {
        Timber.tag(TAG).d("NetSDK 초기화 상태 확인");

        try {
            // NetSDK가 초기화되었는지 확인 (간단한 함수 호출로 테스트)
            int errorCode = com.company.NetSDK.INetSDK.GetLastError();
            Timber.tag(TAG).d("NetSDK GetLastError 성공: %d", errorCode);

            // 초기화 상태 로깅
            if (errorCode == 0) {
                Timber.tag(TAG).i("NetSDK 정상 동작 중");
            } else {
                Timber.tag(TAG).w("NetSDK 초기화 상태: %d", errorCode);
            }
        } catch (UnsatisfiedLinkError e) {
            Timber.tag(TAG).e("NetSDK 라이브러리 로드 실패: %s", e.getMessage());
        } catch (Exception e) {
            Timber.tag(TAG).e("NetSDK 상태 확인 중 예외: %s", e.getMessage());
        }
    }

    /**
     * 권한 확인
     */
    private void checkPermissions() {
        Timber.tag(TAG).d("권한 확인 시작");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ : 미디어 권한 확인
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                Timber.tag(TAG).w("READ_MEDIA_IMAGES 권한 없음");
            }
        } else {
            // Android 12 이하 : 외부 저장소 권한 확인
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Timber.tag(TAG).w("외부 저장소 권한 없음");
            }
        }

        // 인터넷 권한 확인
        if (checkSelfPermission(android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            Timber.tag(TAG).w("인터넷 권한 없음");
        }

        Timber.tag(TAG).d("권한 확인 완료");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Timber.tag(TAG).i("=== DataCollectionActivity 시작 ===");

            setContentView(R.layout.activity_data_collection);

            // 권한 확인
            checkPermissions();

            // View 초기화
            initViews();

        // NetSDK 초기화 상태 확인
        checkNetSDKStatus();

        // ConnectionManager 초기화
        connectionManager = new CameraConnectionManager(this);

            // 버튼 리스너 설정
            setupListeners();

            Timber.tag(TAG).i("=== DataCollectionActivity 초기화 완료 ===");
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "DataCollectionActivity 초기화 실패");
            Toast.makeText(this, "앱 초기화 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * View 초기화
     */
    private void initViews() {
        surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);

        btnSnapshot = findViewById(R.id.btnSnapshot);

        // 18개 홀 버튼 배열
        holeButtons = new Button[]{
            findViewById(R.id.btn1HoleWhite), findViewById(R.id.btn1HoleLady),
            findViewById(R.id.btn2HoleWhite), findViewById(R.id.btn2HoleLady),
            findViewById(R.id.btn3HoleWhite), findViewById(R.id.btn3HoleLady),
            findViewById(R.id.btn4HoleWhite), findViewById(R.id.btn4HoleLady),
            findViewById(R.id.btn5HoleWhite), findViewById(R.id.btn5HoleLady),
            findViewById(R.id.btn6HoleWhite), findViewById(R.id.btn6HoleLady),
            findViewById(R.id.btn7HoleWhite), findViewById(R.id.btn7HoleLady),
            findViewById(R.id.btn8HoleWhite), findViewById(R.id.btn8HoleLady),
            findViewById(R.id.btn9HoleWhite), findViewById(R.id.btn9HoleLady)
        };
    }

    /**
     * 리스너 설정
     */
    private void setupListeners() {
        // 스냅샷 버튼
        btnSnapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureSnapshot();
            }
        });

        // 18개 홀 버튼 (카메라 전환)
        for (int i = 0; i < holeButtons.length; i++) {
            final int cameraIndex = i;
            holeButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchCamera(cameraIndex);
                }
            });
        }
    }

    /**
     * 카메라 전환
     */
    private void switchCamera(final int cameraIndex) {
        if (!isSurfaceReady) {
            Toast.makeText(this, R.string.toast_surface_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }

        final CameraInfo camera = CameraConfig.getCamera(cameraIndex);
        if (camera == null) {
            Timber.tag(TAG).e("카메라 정보 없음 - Index: %d", cameraIndex);
            return;
        }

        // 버튼 비활성화
        setHoleButtonsEnabled(false);
        btnSnapshot.setEnabled(false);

        // 백그라운드 스레드에서 연결
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean success = connectionManager.connectAndPlay(camera, surfaceView);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (success) {
                            btnSnapshot.setEnabled(true);
                            highlightCurrentCamera(cameraIndex);
                        } else {
                            Timber.tag(TAG).w("카메라 연결 실패: %s", camera.getName());
                        }

                        setHoleButtonsEnabled(true);
                    }
                });
            }
        }).start();
    }

    /**
     * 스냅샷 캡처
     */
    private void captureSnapshot() {
        if (!connectionManager.isConnected()) {
            Toast.makeText(this, "카메라가 연결되지 않았습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        final String filePath = generateFilePath();

        btnSnapshot.setEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean success = connectionManager.captureFrame(filePath);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (success) {
                            captureCount++;
                            String fileName = new File(filePath).getName();
                            Timber.tag(TAG).i("캡처 성공 (총 %d개) - %s", captureCount, fileName);
                        } else {
                            Timber.tag(TAG).w("캡처 실패");
                        }

                        btnSnapshot.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    /**
     * 파일 경로 생성
     */
    private String generateFilePath() {
        // Download/GolfCameraData 폴더
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File captureDir = new File(downloadDir, "GolfCameraData");

        if (!captureDir.exists()) {
            boolean created = captureDir.mkdirs();
            if (!created) {
                Timber.tag(TAG).e("폴더 생성 실패: %s", captureDir.getAbsolutePath());
            }
        }

        // 파일명: yyyyMMdd_HHmmss_카메라이름.jpg
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        CameraInfo camera = connectionManager.getCurrentCamera();
        String cameraName = camera != null ? camera.getName().replace(" ", "_") : "unknown";

        String fileName = String.format("%s_%s.jpg", timestamp, cameraName);

        return new File(captureDir, fileName).getAbsolutePath();
    }

    /**
     * 홀 버튼 활성화/비활성화
     */
    private void setHoleButtonsEnabled(boolean enabled) {
        for (Button button : holeButtons) {
            button.setEnabled(enabled);
        }
    }

    /**
     * 현재 선택된 카메라 버튼 강조
     */
    private void highlightCurrentCamera(int cameraIndex) {
        // 모든 버튼 기본 색상으로
        for (int i = 0; i < holeButtons.length; i++) {
            holeButtons[i].setBackgroundResource(android.R.drawable.btn_default);
        }

        // 현재 버튼 강조
        if (cameraIndex >= 0 && cameraIndex < holeButtons.length) {
            holeButtons[cameraIndex].setBackgroundResource(android.R.drawable.btn_default_small);
        }
    }

    // ========== SurfaceHolder.Callback ==========

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            isSurfaceReady = true;
            Timber.tag(TAG).d("Surface 생성됨");
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Surface 생성 중 에러");
            isSurfaceReady = false;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            Timber.tag(TAG).d("Surface 변경: %dx%d", width, height);
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Surface 변경 중 에러");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            isSurfaceReady = false;
            Timber.tag(TAG).d("Surface 파괴됨 - 스트리밍 중지");

            // Surface가 파괴될 때 즉시 스트리밍 중지 (BufferQueue 에러 방지)
            if (connectionManager != null && connectionManager.isConnected()) {
                connectionManager.disconnect();
            }
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Surface 파괴 중 에러");
        }
    }

    // ========== Activity 생명주기 ==========

    @Override
    protected void onPause() {
        super.onPause();
        Timber.tag(TAG).i("=== Activity Pause - 스트리밍 중지 ===");

        // 백그라운드로 갈 때 스트리밍 중지 (배터리 절약 및 BufferQueue 에러 방지)
        if (connectionManager != null && connectionManager.isConnected()) {
            connectionManager.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Timber.tag(TAG).i("=== Activity 종료 ===");

        if (connectionManager != null) {
            connectionManager.release();
        }
    }
}
