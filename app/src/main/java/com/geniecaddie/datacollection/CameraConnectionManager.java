package com.geniecaddie.datacollection;

import android.content.Context;
import android.view.SurfaceView;

import com.company.NetSDK.CB_fRealDataCallBackEx;
import com.company.NetSDK.EM_LOCAL_MODE;
import com.company.NetSDK.EM_LOGIN_SPAC_CAP_TYPE;
import com.company.NetSDK.INetSDK;
import com.company.NetSDK.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY;
import com.company.NetSDK.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY;
import com.company.NetSDK.SDK_RealPlayType;
import com.company.PlaySDK.Constants;
import com.company.PlaySDK.IPlaySDK;

import timber.log.Timber;

/**
 * 카메라 연결 매니저
 * - NetSDK: 카메라 로그인 및 실시간 스트림 요청
 * - PlaySDK: 스트림 디코딩 및 렌더링
 */
public class CameraConnectionManager {
    private static final String TAG = "CameraConnection";

    // NetSDK
    private static final int STREAM_BUF_SIZE = 2 * 1024 * 1024; // 2MB
    private static final int RAW_AUDIO_VIDEO_MIX_DATA = 0;

    private Context context;
    private long loginHandle = 0;
    private long realHandle = 0;
    private int playPort = -1;
    private CB_fRealDataCallBackEx realDataCallback;
    private CameraInfo currentCamera;

    public CameraConnectionManager(Context context) {
        this.context = context;

        // PlaySDK 포트는 사용할 때마다 새로 할당 (재사용 금지)
        this.playPort = -1;
        Timber.tag(TAG).i("CameraConnectionManager 초기화 완료 (포트는 연결 시 할당)");
    }

    /**
     * 카메라 연결 및 스트림 시작
     */
    public boolean connectAndPlay(CameraInfo camera, SurfaceView surfaceView) {
        Timber.tag(TAG).i("=== 카메라 연결 시작 ===");
        Timber.tag(TAG).i("카메라: %s", camera.toString());

        // 1. 기존 연결 정리
        disconnect();

        // 2. 카메라 로그인
        if (!login(camera)) {
            Timber.tag(TAG).e("로그인 실패");
            return false;
        }

        // 3. 스트림 열기 (PlaySDK)
        if (!openStream(surfaceView)) {
            Timber.tag(TAG).e("스트림 열기 실패");
            logout();
            return false;
        }

        // 4. 실시간 재생 시작 (NetSDK)
        if (!startRealPlay(camera.getChannel())) {
            Timber.tag(TAG).e("실시간 재생 시작 실패");
            closeStream();
            logout();
            return false;
        }

        currentCamera = camera;
        Timber.tag(TAG).i("=== 카메라 연결 성공 ===");
        return true;
    }

    /**
     * 카메라 로그인 (NetSDK)
     */
    private boolean login(CameraInfo camera) {
        try {
            NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY stuIn = new NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY();

            // IP 설정
            byte[] ipBytes = camera.getIp().getBytes();
            System.arraycopy(ipBytes, 0, stuIn.szIP, 0, Math.min(ipBytes.length, stuIn.szIP.length));

            // 포트 설정
            stuIn.nPort = Integer.parseInt(camera.getPort());

            // 사용자명 설정
            byte[] userBytes = CameraConfig.USERNAME.getBytes();
            System.arraycopy(userBytes, 0, stuIn.szUserName, 0, Math.min(userBytes.length, stuIn.szUserName.length));

            // 비밀번호 설정
            byte[] pwdBytes = CameraConfig.PASSWORD.getBytes();
            System.arraycopy(pwdBytes, 0, stuIn.szPassword, 0, Math.min(pwdBytes.length, stuIn.szPassword.length));

            // TCP 모드
            stuIn.emSpecCap = EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;

            NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY stuOut = new NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();
            loginHandle = INetSDK.LoginWithHighLevelSecurity(stuIn, stuOut);

            if (loginHandle == 0) {
                int errorCode = INetSDK.GetLastError();
                Timber.tag(TAG).e("NetSDK 로그인 실패 - IP: %s, Port: %s, ErrorCode: %d",
                    camera.getIp(), camera.getPort(), errorCode);
                return false;
            }

            // 최적화 모드 설정
            int nPlayValue = 0x01 | 0x02;
            boolean ret = INetSDK.SetLocalMode(loginHandle, EM_LOCAL_MODE.EM_LOCAL_PLAY_FLAG_MODE, nPlayValue);
            if (!ret) {
                Timber.tag(TAG).w("SetLocalMode 설정 실패 (비필수)");
            }

            Timber.tag(TAG).d("NetSDK 로그인 성공 - Handle: %d", loginHandle);
            return true;
        } catch (UnsatisfiedLinkError e) {
            Timber.tag(TAG).e(e, "NetSDK 라이브러리 로드 실패 - 카메라 연결 불가");
            return false;
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "NetSDK 로그인 중 예외 발생");
            return false;
        }
    }

    /**
     * PlaySDK 스트림 열기
     */
    private boolean openStream(SurfaceView surfaceView) {
        // ✨ 새 포트 할당 (재사용 금지 - 메모리 충돌 방지)
        playPort = IPlaySDK.PLAYGetFreePort();

        if (playPort < 0) {
            Timber.tag(TAG).e("PlayPort 할당 실패");
            return false;
        }

        Timber.tag(TAG).d("새 PlayPort 할당: %d", playPort);

        // 소프트웨어 디코딩 스레드 설정
        int ret = IPlaySDK.PLAYSetDecodeThreadNum(playPort, 4);
        if (ret == 0) {
            Timber.tag(TAG).w("디코딩 스레드 설정 실패 (비필수)");
        }

        // 스트림 열기
        ret = IPlaySDK.PLAYOpenStream(playPort, null, 0, STREAM_BUF_SIZE);
        if (ret == 0) {
            Timber.tag(TAG).e("PLAYOpenStream 실패 - Port: %d", playPort);
            IPlaySDK.PLAYReleasePort(playPort);
            playPort = -1;
            return false;
        }

        // Surface로 재생 시작
        ret = IPlaySDK.PLAYPlay(playPort, surfaceView.getHolder().getSurface());
        if (ret == 0) {
            Timber.tag(TAG).e("PLAYPlay 실패 - Port: %d", playPort);
            IPlaySDK.PLAYCloseStream(playPort);
            IPlaySDK.PLAYReleasePort(playPort);
            playPort = -1;
            return false;
        }

        Timber.tag(TAG).d("PlaySDK 스트림 열기 성공 - Port: %d", playPort);
        return true;
    }

    /**
     * NetSDK 실시간 재생 시작
     */
    private boolean startRealPlay(int channel) {
        try {
            // 실시간 스트림 요청
            realHandle = INetSDK.RealPlayEx(
                loginHandle,
                channel,
                SDK_RealPlayType.SDK_RType_Realplay_0  // Main Stream
            );

            if (realHandle == 0) {
                int errorCode = INetSDK.GetLastError();
                Timber.tag(TAG).e("RealPlayEx 실패 - Channel: %d, ErrorCode: %d", channel, errorCode);
                return false;
            }

            // 실시간 데이터 콜백 설정
            realDataCallback = new CB_fRealDataCallBackEx() {
                @Override
                public void invoke(long rHandle, int dataType, byte[] buffer, int bufSize, int param) {
                    // 원시 오디오/비디오 혼합 데이터
                    if (dataType == RAW_AUDIO_VIDEO_MIX_DATA) {
                        // PlaySDK로 데이터 전달하여 디코딩
                        IPlaySDK.PLAYInputData(playPort, buffer, buffer.length);
                    }
                }
            };

            INetSDK.SetRealDataCallBackEx(realHandle, realDataCallback, 1);

            Timber.tag(TAG).d("NetSDK 실시간 재생 시작 - Handle: %d, Channel: %d", realHandle, channel);
            return true;
        } catch (UnsatisfiedLinkError e) {
            Timber.tag(TAG).e(e, "NetSDK RealPlayEx 라이브러리 오류");
            return false;
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "NetSDK 실시간 재생 시작 중 예외 발생");
            return false;
        }
    }

    /**
     * 현재 프레임 캡처 (JPG)
     */
    public boolean captureFrame(String filePath) {
        if (playPort < 0) {
            Timber.tag(TAG).e("PlayPort가 유효하지 않음");
            return false;
        }

        if (!isConnected()) {
            Timber.tag(TAG).e("카메라가 연결되지 않음");
            return false;
        }

        // JPG 형식으로 캡처
        int ret = IPlaySDK.PLAYCatchPicEx(
            playPort,
            filePath,
            Constants.PicFormat_JPEG
        );

        if (ret == 0) {
            Timber.tag(TAG).e("캡처 실패 - Port: %d, Path: %s", playPort, filePath);
            return false;
        }

        Timber.tag(TAG).i("캡처 성공 - Path: %s", filePath);
        return true;
    }

    /**
     * 연결 종료
     */
    public void disconnect() {
        Timber.tag(TAG).i("=== 연결 종료 시작 ===");

        try {
            // 실시간 재생 중지
            if (realHandle != 0) {
                INetSDK.StopRealPlayEx(realHandle);
                realHandle = 0;
                Timber.tag(TAG).d("NetSDK 실시간 재생 중지");
            }
        } catch (UnsatisfiedLinkError e) {
            Timber.tag(TAG).w("NetSDK StopRealPlayEx 라이브러리 오류 (무시)");
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "NetSDK 실시간 재생 중지 중 예외 발생");
        }

        // 스트림 닫기
        closeStream();

        // 로그아웃
        logout();

        currentCamera = null;
        Timber.tag(TAG).i("=== 연결 종료 완료 ===");
    }

    /**
     * PlaySDK 스트림 닫기
     *
     * ✨ 포트 재사용 금지 방식:
     * 1. 현재 포트를 즉시 무효화 (-1)하여 재사용 차단
     * 2. 이전 포트는 백그라운드에서 천천히 정리
     * 3. GPU 명령 큐가 완전히 비워질 시간 확보 (1초)
     * 4. 새 연결은 다른 포트를 사용하므로 메모리 충돌 없음
     */
    private void closeStream() {
        if (playPort >= 0) {
            final int closingPort = playPort;

            // ✨ 즉시 포트 무효화 (재사용 차단)
            playPort = -1;

            Timber.tag(TAG).d("포트 %d 비동기 닫기 시작", closingPort);

            // 1. 렌더링 중지
            IPlaySDK.PLAYStop(closingPort);

            // 2. Flush 시도
            int flushResult = IPlaySDK.PLAYFlush(closingPort);
            Timber.tag(TAG).d("포트 %d Flush 결과: %d", closingPort, flushResult);

            // 3. 백그라운드에서 안전하게 리소스 해제
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // GPU 명령 큐가 완전히 비워질 때까지 대기
                        // Mali GPU는 비동기 처리이므로 충분한 시간 필요
                        Thread.sleep(1000);

                        // 버퍼 상태 최종 확인
                        int remainBuffer = IPlaySDK.PLAYGetSourceBufferRemain(closingPort);
                        Timber.tag(TAG).d("포트 %d 최종 버퍼: %d bytes", closingPort, remainBuffer);

                        // 스트림 닫기
                        IPlaySDK.PLAYCloseStream(closingPort);

                        // 포트 해제
                        IPlaySDK.PLAYReleasePort(closingPort);

                        Timber.tag(TAG).i("포트 %d 비동기 닫기 완료 ✅", closingPort);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Timber.tag(TAG).w("포트 %d 닫기 중 인터럽트", closingPort);
                    } catch (Exception e) {
                        Timber.tag(TAG).e(e, "포트 %d 닫기 중 예외", closingPort);
                    }
                }
            }).start();
        }
    }

    /**
     * NetSDK 로그아웃
     */
    private void logout() {
        if (loginHandle != 0) {
            try {
                INetSDK.Logout(loginHandle);
                Timber.tag(TAG).d("NetSDK 로그아웃 완료");
            } catch (UnsatisfiedLinkError e) {
                Timber.tag(TAG).w("NetSDK Logout 라이브러리 오류 (무시)");
            } catch (Exception e) {
                Timber.tag(TAG).e(e, "NetSDK 로그아웃 중 예외 발생");
            } finally {
                loginHandle = 0;
            }
        }
    }

    /**
     * 리소스 해제 (Activity onDestroy에서 호출)
     *
     * 현재 활성화된 포트만 동기적으로 정리
     * 비동기로 닫히고 있는 포트들은 백그라운드에서 계속 정리됨
     */
    public void release() {
        Timber.tag(TAG).i("=== 리소스 해제 ===");

        disconnect();

        // 비동기 닫기로 인해 playPort는 이미 -1일 수 있음
        // 그 경우 백그라운드 스레드에서 정리 중
        if (playPort >= 0) {
            Timber.tag(TAG).w("활성 포트 %d가 남아있음 - 동기 정리", playPort);

            IPlaySDK.PLAYStop(playPort);
            IPlaySDK.PLAYCloseStream(playPort);
            IPlaySDK.PLAYReleasePort(playPort);
            playPort = -1;

            Timber.tag(TAG).d("PlayPort 동기 해제 완료");
        } else {
            Timber.tag(TAG).d("활성 포트 없음 (비동기 정리 중)");
        }
    }

    /**
     * 현재 연결된 카메라 정보
     */
    public CameraInfo getCurrentCamera() {
        return currentCamera;
    }

    /**
     * 연결 상태 확인
     */
    public boolean isConnected() {
        return loginHandle != 0 && realHandle != 0;
    }
}
