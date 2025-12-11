package com.geniecaddie;

import android.app.Application;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import com.geniecaddie.logging.LogcatCapture;
import com.geniecaddie.logging.SessionFileLoggingTree;
import timber.log.Timber;
import timber.log.Timber.DebugTree;

// NetSDK imports
import com.company.NetSDK.INetSDK;

import java.io.File;

/**
 * Application 클래스 - 앱 전체 초기화
 *
 * 주요 기능:
 * - DEBUG 빌드에서 로깅 시스템 초기화
 * - Logcat 출력 (Timber.DebugTree)
 * - 파일 출력 (SessionFileLoggingTree)
 * - NetSDK 초기화 (IP 카메라 연결용)
 */
public class DemoApplication extends Application {
    
    // NetSDK 초기화 플래그 (중복 초기화 방지)
    private static boolean isNetSDKInitialized = false;
    
    // Logcat 전체 캡처
    private LogcatCapture logcatCapture;
    
    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // DEBUG 빌드에서만 로깅 활성화
            boolean isDebug = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebug) {
                initializeLogger();

                // 크래시 핸들러 설정 (Timber 초기화 후)
                setupCrashHandler();

                // Logcat 전체 캡처 시작
                logcatCapture = new LogcatCapture();
                logcatCapture.start(this);
            } else {
                Log.i("DemoApp", "RELEASE 빌드 - 로깅 비활성화");
            }

            // NetSDK 초기화 (필수!)
            initializeNetSDK();

        } catch (Exception e) {
            Log.e("DemoApp", "Application 초기화 실패", e);
            // 앱 초기화 실패해도 앱은 계속 실행될 수 있도록 함
        }
    }
    
    /**
     * NetSDK 초기화
     * 주의: 모든 NetSDK 함수 호출 전에 반드시 실행되어야 함
     * 중복 초기화 방지: 이미 초기화된 경우 스킵
     * 라이브러리 로드 실패 시에도 앱 실행은 계속됨
     */
    private void initializeNetSDK() {
        Timber.d("=== NetSDK 초기화 시작 ===");

        // 중복 초기화 방지
        if (isNetSDKInitialized) {
            Timber.i("NetSDK 이미 초기화됨 - 스킵");
            return;
        }

        Timber.d("NetSDK 초기화 시도...");
        Timber.d("Disconnect 콜백 객체: null (콜백 제거)");

        try {
            // NetSDK 초기화 시도
            long startTime = System.currentTimeMillis();
            boolean initResult = INetSDK.Init(null);
            long endTime = System.currentTimeMillis();

            Timber.d("INetSDK.Init() 호출 완료 - 소요시간: %dms", (endTime - startTime));

            if (initResult) {
                isNetSDKInitialized = true;
                Timber.i("✅ NetSDK 초기화 성공");

                // 추가 정보 확인
                try {
                    int lastError = INetSDK.GetLastError();
                    Timber.d("초기화 후 LastError: %d", lastError);
                } catch (Exception e) {
                    Timber.w("LastError 확인 실패: %s", e.getMessage());
                }
            } else {
                Timber.e("❌ NetSDK 초기화 실패!");
                try {
                    int lastError = INetSDK.GetLastError();
                    Timber.e("실패 시 LastError: %d", lastError);
                } catch (Exception e) {
                    Timber.w("실패 시 LastError 확인 실패: %s", e.getMessage());
                }
            }
        } catch (UnsatisfiedLinkError e) {
            Timber.e("❌ NetSDK 라이브러리 로드 실패: %s", e.getMessage());
            Timber.e("라이브러리 파일들이 올바르게 로드되었는지 확인 필요");
        } catch (Exception e) {
            Timber.e(e, "❌ NetSDK 초기화 중 예외 발생");
        }

        Timber.d("=== NetSDK 초기화 종료 ===");
    }
    
    /**
     * 크래시 핸들러 설정
     * - 모든 예상치 못한 크래시를 파일에 기록
     * - 기존 핸들러를 보존하여 정상적인 앱 종료 유지
     */
    private void setupCrashHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = 
            Thread.getDefaultUncaughtExceptionHandler();
        
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    // 크래시 정보를 로그 파일에 기록
                    Timber.tag("CRASH").e("================================================");
                    Timber.tag("CRASH").e("===== UNCAUGHT EXCEPTION DETECTED =====");
                    Timber.tag("CRASH").e("================================================");
                    Timber.tag("CRASH").e("Thread: %s (ID: %d)", thread.getName(), thread.getId());
                    Timber.tag("CRASH").e("Exception Type: %s", throwable.getClass().getName());
                    Timber.tag("CRASH").e("Message: %s", throwable.getMessage());
                    Timber.tag("CRASH").e(throwable, "Stack Trace:");
                    Timber.tag("CRASH").e("================================================");
                    
                    // 로그가 파일에 쓰여질 시간 확보
                    Thread.sleep(500);
                    
                } catch (Exception e) {
                    // 크래시 핸들러에서 예외 발생해도 무시 (무한 루프 방지)
                    Log.e("DemoApp", "크래시 핸들러에서 에러 발생", e);
                }
                
                // 기본 핸들러 호출 (앱 정상 종료)
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                } else {
                    // 기본 핸들러가 없으면 프로세스 종료
                    System.exit(1);
                }
            }
        });
        
        Timber.tag("DemoApp").i("크래시 핸들러 설정 완료");
    }
    
    /**
     * 로깅 시스템 초기화
     * - Logcat Tree: 실시간 디버깅용
     * - File Tree: 사후 분석용 (다운로드 폴더)
     */
    private void initializeLogger() {
        try {
            // 1. Logcat 출력 (Timber.DebugTree)
            Timber.plant(new Timber.DebugTree());
            
            // 2. 다운로드 폴더에 로그 디렉토리 생성
            File downloadDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            );
            File logRootDir = new File(downloadDir, "DahuaPlaySDKLogs");
            
            // 디렉토리 생성
            if (!logRootDir.exists()) {
                if (!logRootDir.mkdirs()) {
                    Log.e("DemoApp", "로그 디렉토리 생성 실패: " + logRootDir.getAbsolutePath());
                    Timber.e("로그 디렉토리 생성 실패 - 파일 로깅 불가");
                    return;
                }
            }
            
            // 3. 파일 로깅 Tree 등록
            SessionFileLoggingTree fileTree = new SessionFileLoggingTree(logRootDir);
            Timber.plant(fileTree);
            
            // 4. 초기화 완료 로그
            Timber.i("=================================================");
            Timber.i("=== Dahua PlaySDK Demo - 로깅 시스템 시작 ===");
            Timber.i("=================================================");
            Timber.i("로그 저장 경로: %s", logRootDir.getAbsolutePath());
            
            // 버전 정보 (PackageInfo에서 가져오기)
            try {
                String packageName = getPackageName();
                android.content.pm.PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
                Timber.i("앱 버전: %s (%d)", packageInfo.versionName, packageInfo.versionCode);
            } catch (Exception e) {
                Timber.w("버전 정보 가져오기 실패");
            }
            
            Timber.i("Android: %s (API %d)", 
                Build.VERSION.RELEASE, 
                Build.VERSION.SDK_INT);
            Timber.i("디바이스: %s %s", 
                Build.MANUFACTURER, 
                Build.MODEL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Timber.i("ABI: %s", Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
                    ? Build.SUPPORTED_ABIS[0] : "unknown");
            }
            Timber.i("=================================================");
            
        } catch (Exception e) {
            Log.e("DemoApp", "로거 초기화 중 예외 발생", e);
        }
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        // Logcat 캡처 중지
        if (logcatCapture != null) {
            logcatCapture.stop();
            logcatCapture = null;
        }
        
        // NetSDK 정리 (초기화된 경우에만)
        if (isNetSDKInitialized) {
            INetSDK.Cleanup();
            isNetSDKInitialized = false;
            Timber.i("NetSDK 정리 완료");
        }
        
        // ExecutorService 정리
        // 주의: onTerminate()는 실제 디바이스에서는 호출되지 않을 수 있음
        // 하지만 에뮬레이터나 테스트 환경에서는 호출될 수 있음
        Timber.i("앱 종료 - 로깅 시스템 정리");
    }
}

