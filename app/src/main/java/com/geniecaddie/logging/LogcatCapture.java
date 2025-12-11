package com.geniecaddie.logging;

import android.content.Context;
import android.os.Environment;
import timber.log.Timber;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Logcat 전체를 파일로 저장하는 클래스
 * 
 * 기능:
 * - 앱 실행 중 모든 logcat 출력을 파일에 실시간 저장
 * - 크래시 발생 시에도 로그가 파일에 남아있음
 * - Java/Native/SDK 로그 모두 캡처
 * 
 * 제한사항:
 * - Android 4.1+ 부터는 본인 앱의 logcat만 읽을 수 있음 (보안 정책)
 * - DEBUG 빌드에서만 사용 권장
 */
public class LogcatCapture {
    
    private static final String TAG = "LogcatCapture";
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    
    private Process logcatProcess;
    private Thread logcatThread;
    private volatile boolean isRunning = false;
    private File currentLogFile;
    
    /**
     * logcat 캡처 시작
     */
    public void start(final Context context) {
        if (isRunning) {
            Timber.tag(TAG).w("이미 실행 중");
            return;
        }
        
        isRunning = true;
        
        logcatThread = new Thread(new Runnable() {
            @Override
            public void run() {
                captureLogcat(context);
            }
        });
        
        logcatThread.setName("LogcatCapture-Thread");
        logcatThread.setPriority(Thread.MIN_PRIORITY); // 낮은 우선순위
        logcatThread.start();
    }
    
    /**
     * logcat 캡처 중지
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        Timber.tag(TAG).i("Logcat 캡처 중지 요청");
        isRunning = false;
        
        // 프로세스 종료
        if (logcatProcess != null) {
            logcatProcess.destroy();
            logcatProcess = null;
        }
        
        // 스레드 종료 대기
        if (logcatThread != null) {
            try {
                logcatThread.join(2000); // 최대 2초 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            logcatThread = null;
        }
        
        Timber.tag(TAG).i("Logcat 캡처 중지 완료");
    }
    
    /**
     * 실제 logcat 캡처 로직
     */
    private void captureLogcat(Context context) {
        BufferedReader reader = null;
        FileOutputStream outputStream = null;
        
        try {
            // 로그 파일 생성
            currentLogFile = createLogFile(context);
            if (currentLogFile == null) {
                Timber.tag(TAG).e("로그 파일 생성 실패");
                return;
            }
            
            Timber.tag(TAG).i("Logcat 캡처 시작: %s", currentLogFile.getName());
            
            // 기존 logcat 버퍼 클리어 (선택사항)
            try {
                Runtime.getRuntime().exec("logcat -c");
                Thread.sleep(100);
            } catch (Exception e) {
                // 클리어 실패해도 계속 진행
            }
            
            // logcat 프로세스 시작
            // -v time: 타임스탬프 포함
            // *:V: 모든 태그의 VERBOSE 레벨 이상 (전체 로그)
            logcatProcess = Runtime.getRuntime().exec(
                new String[]{"logcat", "-v", "threadtime", "*:V"}
            );
            
            reader = new BufferedReader(
                new InputStreamReader(logcatProcess.getInputStream()),
                BUFFER_SIZE
            );
            
            outputStream = new FileOutputStream(currentLogFile);
            
            // 헤더 작성
            String header = String.format(
                "===== Logcat Capture Started =====\n" +
                "Date: %s\n" +
                "File: %s\n" +
                "===================================\n\n",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()),
                currentLogFile.getName()
            );
            outputStream.write(header.getBytes());
            
            String line;
            int lineCount = 0;
            long lastFlush = System.currentTimeMillis();
            
            while (isRunning && (line = reader.readLine()) != null) {
                // 파일에 쓰기
                outputStream.write((line + "\n").getBytes());
                lineCount++;
                
                // 주기적으로 flush (크래시 시에도 최대한 많이 저장)
                long now = System.currentTimeMillis();
                if (now - lastFlush > 1000) { // 1초마다 flush
                    outputStream.flush();
                    lastFlush = now;
                }
                
                // 파일 크기 체크 (50MB 초과 시 새 파일)
                if (lineCount % 1000 == 0) { // 1000줄마다 체크
                    if (currentLogFile.length() > MAX_FILE_SIZE) {
                        Timber.tag(TAG).w("로그 파일 크기 초과, 새 파일 생성");
                        outputStream.flush();
                        outputStream.close();
                        
                        currentLogFile = createLogFile(context);
                        if (currentLogFile != null) {
                            outputStream = new FileOutputStream(currentLogFile);
                            outputStream.write(header.getBytes());
                        } else {
                            break;
                        }
                    }
                }
            }
            
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Logcat 캡처 중 에러");
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "예상치 못한 에러");
        } finally {
            // 정리
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // ignore
            }
            
            if (currentLogFile != null) {
                Timber.tag(TAG).i("Logcat 캡처 종료 - 저장 완료: %s (%.2f MB)", 
                    currentLogFile.getName(),
                    currentLogFile.length() / (1024.0 * 1024.0));
            }
        }
    }
    
    /**
     * 로그 파일 생성
     */
    private File createLogFile(Context context) {
        try {
            // Download/DahuaPlaySDKLogs/logcat/ 폴더
            File downloadDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            );
            File logcatDir = new File(downloadDir, "DahuaPlaySDKLogs/logcat");
            
            if (!logcatDir.exists()) {
                if (!logcatDir.mkdirs()) {
                    Timber.tag(TAG).e("logcat 폴더 생성 실패");
                    return null;
                }
            }
            
            // 파일명: logcat_yyyyMMdd_HHmmss.txt
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String fileName = "logcat_" + timestamp + ".txt";
            
            File logFile = new File(logcatDir, fileName);
            
            // 오래된 파일 정리 (7일 이상)
            cleanupOldLogFiles(logcatDir);
            
            return logFile;
            
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "로그 파일 생성 실패");
            return null;
        }
    }
    
    /**
     * 오래된 로그 파일 정리 (7일 이상)
     */
    private void cleanupOldLogFiles(File logcatDir) {
        try {
            File[] files = logcatDir.listFiles();
            if (files == null) return;
            
            long now = System.currentTimeMillis();
            long sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L);
            
            int deletedCount = 0;
            for (File file : files) {
                if (file.isFile() && file.lastModified() < sevenDaysAgo) {
                    if (file.delete()) {
                        deletedCount++;
                    }
                }
            }
            
            if (deletedCount > 0) {
                Timber.tag(TAG).i("오래된 logcat 파일 %d개 삭제됨", deletedCount);
            }
            
        } catch (Exception e) {
            Timber.tag(TAG).w(e, "logcat 파일 정리 중 에러");
        }
    }
    
    /**
     * 현재 실행 중인지 확인
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * 현재 로그 파일 경로 반환
     */
    public String getCurrentLogFilePath() {
        return currentLogFile != null ? currentLogFile.getAbsolutePath() : null;
    }
}

