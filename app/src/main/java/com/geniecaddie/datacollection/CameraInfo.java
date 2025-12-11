package com.geniecaddie.datacollection;

/**
 * 카메라 정보 모델 클래스
 */
public class CameraInfo {
    private final String name;         // 예: "1홀 White"
    private final String ip;           // 예: "172.16.1.228"
    private final String port;         // 예: "1001" 또는 "1003"
    private final int channel;         // 채널 번호 (기본 0)

    /**
     * 생성자 1: IP + 포트 (채널 0)
     */
    public CameraInfo(String name, String ip, String port) {
        this(name, ip, port, 0);
    }

    /**
     * 생성자 2: IP + 포트 + 채널
     */
    public CameraInfo(String name, String ip, String port, int channel) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.channel = channel;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public int getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return name + " (" + ip + ":" + port + ", ch:" + channel + ")";
    }
}
