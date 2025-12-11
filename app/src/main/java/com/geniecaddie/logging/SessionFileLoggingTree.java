package com.geniecaddie.logging;

import android.util.Log;
import timber.log.Timber;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Timber Tree 구현체 - 세션별 파일 로깅
 * 
 * 주요 기능:
 * - 일자별 폴더 분리 (yyyy-MM-dd)
 * - 세션별 파일 생성 (매 앱 실행마다)
 * - 10MB 파일 크기 제한 (초과 시 파트 분할)
 * - 10GB 총 용량 제한
 * - 30일 자동 정리
 * 
 * 저장 위치: /sdcard/Download/DahuaPlaySDKLogs/yyyy-MM-dd/session_HHmmss_uuid.txt
 */
public class SessionFileLoggingTree extends Timber.Tree {
    
    // === 정책 상수 ===
    private static final long MAX_SESSION_FILE_SIZE = 10 * 1024 * 1024;      // 10MB
    private static final long MAX_TOTAL_LOG_SIZE = 10L * 1024 * 1024 * 1024; // 10GB
    private static final long MAX_FILE_AGE_DAYS = 30;                         // 30일
    
    private final File logRootDir;              // /sdcard/Download/DahuaPlaySDKLogs/
    private final File todayLogDir;             // /sdcard/Download/DahuaPlaySDKLogs/2025-10-28/
    private final ExecutorService executor;
    private final SimpleDateFormat dateFormat;        // yyyy-MM-dd (폴더명)
    private final SimpleDateFormat timeFormat;        // HHmmss (파일명)
    private final SimpleDateFormat timestampFormat;   // yyyy-MM-dd HH:mm:ss.SSS (로그 내용)
    
    private File currentLogFile;
    private int partNumber = 1;
    private final String sessionId;
    
    public SessionFileLoggingTree(File logRootDir) {
        this.logRootDir = logRootDir;
        this.executor = Executors.newSingleThreadExecutor();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        this.timeFormat = new SimpleDateFormat("HHmmss", Locale.US);
        this.timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        this.sessionId = UUID.randomUUID().toString().substring(0, 8);
        
        // 오늘 날짜 폴더 생성
        String today = dateFormat.format(new Date());
        this.todayLogDir = new File(logRootDir, today);
        if (!todayLogDir.exists()) {
            todayLogDir.mkdirs();
        }
        
        // 초기화 시 유지보수 작업 (비동기)
        executor.execute(this::performMaintenance);
        
        // 현재 세션 파일 생성
        this.currentLogFile = createNewSessionFile();
    }
    
    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        // VERBOSE 이상 모든 로그 기록
        if (priority < Log.VERBOSE) {
            return;
        }
        
        executor.execute(() -> {
            try {
                writeLogEntry(priority, tag, message, t);
            } catch (Exception e) {
                Log.e("SessionFileLoggingTree", "로그 쓰기 실패", e);
            }
        });
    }
    
    /**
     * 로그 엔트리를 파일에 기록
     */
    private void writeLogEntry(int priority, String tag, String message, Throwable t) {
        // 파일 크기 체크 및 파트 분할
        if (currentLogFile.length() > MAX_SESSION_FILE_SIZE) {
            partNumber++;
            currentLogFile = createNewSessionFile();
            
            Log.i("SessionFileLoggingTree", 
                String.format("파일 크기 초과 - 파트 %d 생성: %s", 
                partNumber, currentLogFile.getName()));
        }
        
        try (FileWriter writer = new FileWriter(currentLogFile, true)) {
            String timestamp = timestampFormat.format(new Date());
            String priorityStr = getPriorityString(priority);
            String pid = String.valueOf(android.os.Process.myPid());
            String tid = String.valueOf(android.os.Process.myTid());
            
            // Logcat 호환 형식: [날짜 시간] [PID-TID]/[태그] [레벨]: [메시지]
            String logLine = String.format(Locale.US,
                "%s %5s-%5s/%s %s: %s\n",
                timestamp, pid, tid, tag, priorityStr, message
            );
            
            writer.write(logLine);
            
            // 예외 스택 트레이스
            if (t != null) {
                writer.write(Log.getStackTraceString(t) + "\n");
            }
            
        } catch (IOException e) {
            Log.e("SessionFileLoggingTree", "파일 쓰기 실패: " + currentLogFile, e);
        }
    }
    
    /**
     * 새 세션 로그 파일 생성
     */
    private File createNewSessionFile() {
        String time = timeFormat.format(new Date());
        String filename;
        
        if (partNumber == 1) {
            filename = String.format("session_%s_%s.txt", time, sessionId);
        } else {
            filename = String.format("session_%s_%s_part%d.txt", time, sessionId, partNumber);
        }
        
        File file = new File(todayLogDir, filename);
        
        try {
            if (file.createNewFile()) {
                Log.i("SessionFileLoggingTree", "새 로그 파일 생성: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e("SessionFileLoggingTree", "로그 파일 생성 실패", e);
        }
        
        return file;
    }
    
    /**
     * 로그 디렉토리 유지보수
     * - 일자별 폴더 단위로 관리
     * - 30일 초과 폴더 삭제
     * - 총 용량 10GB 초과 시 가장 오래된 폴더부터 삭제
     */
    private void performMaintenance() {
        File[] dateDirs = logRootDir.listFiles(new java.io.FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        
        if (dateDirs == null || dateDirs.length == 0) {
            Log.d("SessionFileLoggingTree", "유지보수할 폴더 없음");
            return;
        }
        
        // 날짜순 정렬 (오래된 순)
        Arrays.sort(dateDirs, (d1, d2) -> d1.getName().compareTo(d2.getName()));
        
        long now = System.currentTimeMillis();
        long maxAge = TimeUnit.DAYS.toMillis(MAX_FILE_AGE_DAYS);
        long totalSize = 0;
        int deletedByAge = 0;
        
        // 1단계: 30일 초과 폴더 삭제
        for (File dateDir : dateDirs) {
            if (now - dateDir.lastModified() > maxAge) {
                long dirSize = getDirectorySize(dateDir);
                if (deleteDirectory(dateDir)) {
                    deletedByAge++;
                    Log.i("SessionFileLoggingTree", 
                        String.format("30일 초과 폴더 삭제: %s (%.2f MB)", 
                        dateDir.getName(), dirSize / (1024.0 * 1024.0)));
                }
            } else {
                totalSize += getDirectorySize(dateDir);
            }
        }
        
        // 2단계: 총 용량 체크 (10GB 초과)
        int deletedBySize = 0;
        if (totalSize > MAX_TOTAL_LOG_SIZE) {
            // 재스캔 (삭제된 폴더 제외)
            dateDirs = logRootDir.listFiles(File::isDirectory);
            if (dateDirs != null) {
                Arrays.sort(dateDirs, (d1, d2) -> d1.getName().compareTo(d2.getName()));
                
                long sizeToFree = totalSize - MAX_TOTAL_LOG_SIZE;
                long freedSize = 0;
                
                for (File dateDir : dateDirs) {
                    if (freedSize >= sizeToFree) {
                        break;
                    }
                    
                    // 오늘 폴더는 삭제하지 않음
                    if (dateDir.equals(todayLogDir)) {
                        continue;
                    }
                    
                    long dirSize = getDirectorySize(dateDir);
                    if (deleteDirectory(dateDir)) {
                        freedSize += dirSize;
                        deletedBySize++;
                        totalSize -= dirSize;
                        Log.i("SessionFileLoggingTree", 
                            String.format("용량 초과로 폴더 삭제: %s (%.2f MB)", 
                            dateDir.getName(), dirSize / (1024.0 * 1024.0)));
                    }
                }
            }
        }
        
        Log.i("SessionFileLoggingTree", 
            String.format("로그 유지보수 완료 - 현재: %.2f GB / %.2f GB, 삭제: %d개(기간) + %d개(용량)", 
            totalSize / (1024.0 * 1024.0 * 1024.0),
            MAX_TOTAL_LOG_SIZE / (1024.0 * 1024.0 * 1024.0),
            deletedByAge,
            deletedBySize));
    }
    
    /**
     * 디렉토리 크기 계산 (재귀)
     */
    private long getDirectorySize(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += getDirectorySize(file);
                }
            }
        }
        return size;
    }
    
    /**
     * 디렉토리 재귀 삭제
     */
    private boolean deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return directory.delete();
    }
    
    /**
     * 로그 레벨을 문자열로 변환
     */
    private String getPriorityString(int priority) {
        switch (priority) {
            case Log.VERBOSE: return "V";
            case Log.DEBUG:   return "D";
            case Log.INFO:    return "I";
            case Log.WARN:    return "W";
            case Log.ERROR:   return "E";
            case Log.ASSERT:  return "A";
            default:          return "?";
        }
    }
    
    /**
     * 앱 종료 시 정리
     * ExecutorService를 안전하게 종료
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

