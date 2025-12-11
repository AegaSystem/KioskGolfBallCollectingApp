package com.geniecaddie.datacollection;

/**
 * 카메라 설정 클래스
 * - 18개 카메라 정보 (9홀 x 2티)
 * - 인증 정보
 */
public class CameraConfig {

    // 인증 정보
    public static final String USERNAME = "admin";
    public static final String PASSWORD = "genie0801";

    // 18개 카메라 정보
    public static final CameraInfo[] CAMERAS = {
        // 1홀
        new CameraInfo("Hole1_White", "172.16.1.228", "1001"),
        new CameraInfo("Hole1_Lady", "172.16.1.236", "1003"),

        // 2홀
        new CameraInfo("Hole2_White", "172.16.1.226", "1001"),
        new CameraInfo("Hole2_Lady", "172.16.1.226", "1003"),

        // 3홀
        new CameraInfo("Hole3_White", "172.16.1.234", "1001"),
        new CameraInfo("Hole3_Lady", "172.16.1.231", "1003"),

        // 4홀
        new CameraInfo("Hole4_White", "172.16.1.223", "1001"),
        new CameraInfo("Hole4_Lady", "172.16.1.227", "1003"),

        // 5홀
        new CameraInfo("Hole5_White", "172.16.1.224", "1001"),
        new CameraInfo("Hole5_Lady", "172.16.1.222", "1003"),

        // 6홀
        new CameraInfo("Hole6_White", "172.16.1.238", "1001"),
        new CameraInfo("Hole6_Lady", "172.16.1.238", "1003"),

        // 7홀
        new CameraInfo("Hole7_White", "172.16.1.230", "1001"),
        new CameraInfo("Hole7_Lady", "172.16.1.229", "1003"),

        // 8홀
        new CameraInfo("Hole8_White", "172.16.1.235", "1001"),
        new CameraInfo("Hole8_Lady", "172.16.1.221", "1003"),

        // 9홀
        new CameraInfo("Hole9_White", "172.16.1.237", "1001"),
        new CameraInfo("Hole9_Lady", "172.16.1.232", "1003")
    };

    /**
     * 인덱스로 카메라 정보 가져오기
     */
    public static CameraInfo getCamera(int index) {
        if (index >= 0 && index < CAMERAS.length) {
            return CAMERAS[index];
        }
        return null;
    }

    /**
     * 홀 번호와 티 타입으로 카메라 찾기
     * @param hole 홀 번호 (1~9)
     * @param isWhite true=White, false=Lady
     */
    public static CameraInfo getCameraByHole(int hole, boolean isWhite) {
        if (hole < 1 || hole > 9) {
            return null;
        }

        int index = (hole - 1) * 2 + (isWhite ? 0 : 1);
        return getCamera(index);
    }

    /**
     * 전체 카메라 수
     */
    public static int getCameraCount() {
        return CAMERAS.length;
    }
}
