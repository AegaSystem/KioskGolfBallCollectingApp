package com.company.PlaySDK;

import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;

import com.company.PlaySDK.IPlaySDKCallBack.*;

import java.io.File;
import java.util.Arrays;

import timber.log.Timber;

public class IPlaySDK
{
	static {
		// ABI 정보 로깅
		logAbiInfo();
		
		String []liblist = 
			{
				"gnustl_shared",
				"play",
				"jniplay",
				"hwdec"
				};
		
		Timber.tag("PlaySDK").i("=== 네이티브 라이브러리 로드 시작 ===");
		for(String lib : liblist)
		{
			try
			{
				System.loadLibrary(lib);
				Timber.tag("PlaySDK").d("[성공] %s 로드 완료", lib);
			}
			catch(UnsatisfiedLinkError ulink)
			{
				Timber.tag("PlaySDK").e(ulink, "[실패] %s 로드 실패", lib);
				ulink.printStackTrace();
			}
		}
		Timber.tag("PlaySDK").i("=== 네이티브 라이브러리 로드 완료 ===");
	}
	
	/**
	 * ABI 및 라이브러리 경로 정보 로깅
	 */
	private static void logAbiInfo() {
		Timber.tag("PlaySDK").i("=== ABI 정보 ===");
		
		// 1. 실제 프로세스 모드 먼저 확인 (가장 중요!)
		String osArch = System.getProperty("os.arch", "unknown");
		boolean is32bit = osArch.contains("i686") || osArch.contains("i386") || 
		                  osArch.equals("x86") || osArch.equals("arm");
		boolean is64bit = osArch.contains("x86_64") || osArch.contains("amd64") || 
		                  osArch.contains("aarch64") || osArch.contains("arm64");
		
		Timber.tag("PlaySDK").i("실제 프로세스: %s (os.arch=%s)",
		      (is32bit ? "32bit" : (is64bit ? "64bit" : "알 수 없음")),
		      osArch);
		
		// 2. OS가 지원하는 ABI 목록 (하드웨어 능력, APK 포함 여부와 무관)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			String[] supportedAbis = Build.SUPPORTED_ABIS;
			Timber.tag("PlaySDK").d("[OS 지원 ABI] %s", Arrays.toString(supportedAbis));
			
			String[] supported32 = Build.SUPPORTED_32_BIT_ABIS;
			String[] supported64 = Build.SUPPORTED_64_BIT_ABIS;
			if (supported32.length > 0) {
				Timber.tag("PlaySDK").d("[OS 지원 32bit] %s", Arrays.toString(supported32));
			}
			if (supported64.length > 0) {
				Timber.tag("PlaySDK").d("[OS 지원 64bit] %s", Arrays.toString(supported64));
			}
		} else {
			Timber.tag("PlaySDK").d("[OS 지원] CPU_ABI: %s, CPU_ABI2: %s", Build.CPU_ABI, Build.CPU_ABI2);
		}
		
		// 3. 실제 사용되는 ABI 추정
		String actualAbi;
		if (is32bit) {
			// 32bit 프로세스 = 32bit ABI 중 하나 사용 중
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				String[] supported32 = Build.SUPPORTED_32_BIT_ABIS;
				actualAbi = supported32.length > 0 ? supported32[0] : "x86";
			} else {
				actualAbi = Build.CPU_ABI;
			}
			Timber.tag("PlaySDK").i("[실제 사용 ABI] %s (APK 포함 32bit 라이브러리)", actualAbi);
		} else if (is64bit) {
			// 64bit 프로세스 = 64bit ABI 중 하나 사용 중
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				String[] supported64 = Build.SUPPORTED_64_BIT_ABIS;
				actualAbi = supported64.length > 0 ? supported64[0] : "x86_64";
			} else {
				actualAbi = "x86_64";
			}
			Timber.tag("PlaySDK").i("[실제 사용 ABI] %s (APK 포함 64bit 라이브러리)", actualAbi);
		} else {
			Timber.tag("PlaySDK").w("[실제 사용 ABI] 알 수 없음");
		}
		
		Timber.tag("PlaySDK").d("참고: APK 포함 ABI는 build.gradle abiFilters로 결정");
	}
	
	public static class CUSTOMRECT
	{
		public int left;
		public int top;
		public int right;
		public int bottom;
	};
	
	public static class MEDIA_INFO
	{
		public int			lWidth;					
		public int			lHeight;				
		public int			lFrameRate;				
		public int			lChannel;				
		public int			lBitPerSample;			
		public int			lSamplesPerSec;			
	};

	public static class IVSDRAWER_TrackEx2Config
	{
		public int         objtype_enable;                // show object type, 0:NO 1:YES
		public int         attribute88_enable;            // show 0x88 attribute, 0:NO 1:YES
		public int         objid_enable;                  // show object ID, 0:NO 1:YES
		public int         age_enable;					  // show age,0:NO 1:YES
	};
	
	/**
	 * 설명: 파일 열기 인터페이스.
	 * @param nPort 포트 번호
	 * @param sFileName 파일 이름, 파일 크기 범위는 4K에서 4G까지.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYOpenFile(int nPort,String sFileName);
	
	/**
	 * 설명: 재생된 파일 닫기, PLAYStop 이후에 호출.
	 * @param nPort 포트 번호
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYCloseFile(int nPort);
	
	/**
	 * 설명: 스트림 열기 인터페이스 (파일 열기와 동일)
	 * @param port 포트 번호
	 * @param pFileHeadBuf 현재 사용 불가. null로 설정하세요.
	 * @param nSize	현재 사용 불가. 0으로 설정하세요.
	 * @param nBufPoolSize 저장 데이터 스트림 버퍼 크기 설정. 값 범위는 [SOURCE_BUF_MIN,SOURCE_BUF_MAX]. 일반적으로 900*1024. 
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYOpenStream(int port,  byte[] pFileHeadBuf, int nSize, int nBufPoolSize);
	
	/** 설명: 데이터 스트림 닫기 (파일 닫기와 동일), PLAY_Stop 이후에 호출.
	 *  @param port 포트 번호
	 *  @return true 성공, false 실패.
	 */
	public native static int PLAYCloseStream(int port);

	/**
	 * 설명: 내부 함수, 사용자는 신경 쓸 필요 없음.
	 * @param port 포트 번호
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYPlay(int port, Surface surface);

	
	/**
	 * 설명: 내부 함수, 사용자는 신경 쓸 필요 없음.
	 * @param port 포트 번호
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYStop(int port);

	/**
	 * 설명: 카드에서 가져온 스트림 데이터를 입력합니다. 스트림을 활성화한 다음 PLAY_Play를 호출하여 데이터를 입력합니다.
	 * @param port 포트 번호
	 * @param buffer 버퍼 주소
	 * @param nSize 버퍼 크기
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYInputData(int port, byte[] buffer, int nSize);
	
	/**
	 * 설명: 일시정지/재개
	 * @param nPort 포트 번호
	 * @param nPause TRUE 일시정지, FALSE 재개
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYPause(int nPort,short nPause);
	
	/**
	 * 설명: 빠른 재생. 속도가 한 단계씩 증가하며 한 번의 콜백에서 최대 4배속 지원. 현재 위치에서 정상 재생을 재개하려면 PLAY_Play를 호출할 수 있습니다.
	 * @param nPort 포트 번호
	 * @return true 성공, false 실패.
	 */
	public native static int  PLAYFast(int nPort);


	/**
	 * 설명: 느린 재생. 위와 동일합니다. 매번 속도가 한 단계씩 감소합니다. 
	 *				최대 4번 콜백 지원. 정상 재생을 재개하려면 PLAY_Play를 호출하세요.
	 * @param nPort 포트 번호
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSlow(int nPort);
	
	/**
	 * 설명: 프레임별 재생. 정상 재생을 재개하려면 PLAY_Play를 호출하세요.
	 * @param nPort 포트 번호
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYOneByOne(int nPort);

	/**
	 * 설명: 파일 재생 포인터의 상대 위치 설정 (단위: %).
	 * @param nPort 포트 번호
	 * @param fRelativePos 값 범위는 [0, 100%]
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetPlayPos(int nPort,float fRelativePos);
	
	/**
	 * 설명: 재생 방향 설정.
	 * @param nPort 포트 번호
	 * @param emDirection 재생 방향: 0-정방향, 1-역방향
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetPlayDirection(int nPort, short emDirection);

	/**
	 * 설명: 파일 플레이어 포인터의 상대 위치를 가져옵니다.
	 * @param nPort 포트 번호
	 * @return float, 값 범위는 [0, 100%]
	 */
	public native static float PLAYGetPlayPos(int nPort);


	/**
	 * 설명: 볼륨 설정.
	 * @param nPort 포트 번호
	 * @param nVolume 볼륨 값. 값 범위는 [0, 0XFFFF].
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetVolume(int nPort,int nVolume);

	/**
	* 설명: 볼륨 가져오기.
	* @param nPort 포트 번호
	* @return 볼륨 값.
	*/
	public native static int PLAYGetVolume(int nPort);

	/**
	 * 설명: 오디오 비활성화.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYStopSound();

	/**
	 * 설명: 오디오 활성화. 한 번에 1채널 오디오만 가능합니다. 시스템은 현재 오디오가 활성화되면 
	 * 				이전 오디오를 자동으로 비활성화합니다. 
	 * 				기본적으로 오디오는 비활성화되어 있습니다.
	 * @param nPort 포트 번호
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYPlaySound(int nPort);
	
	/**
	 * 설명: 파일 총 시간 길이를 가져옵니다. 단위는 초.
	 * @param nPort 포트 번호
	 * @return int, 총 파일 시간
	 */
	public native static int PLAYGetFileTime(int nPort);

	/**
	 * 설명: 파일 재생 시간을 가져옵니다. 단위는 초.
	 * @param nPort 포트 번호
	 * @return int, 파일의 현재 재생 시간
	 */
	public native static int PLAYGetPlayedTime(int nPort);

	/**
	 * 설명: 디코딩된 비디오 프레임 수를 가져옵니다.
	 * @param nPort 포트 번호
	 * @return int, 디코딩된 비디오 프레임 수.
	 */
	public native static int PLAYGetPlayedFrames(int nPort);

	
	/**
	 * 설명: 콜백 함수를 설정합니다. 플레이어에서 표시되는 섹션을 대체합니다. 표시되는 비디오를 제어할 수 있습니다.
	 *              PLAY_Play 전에 이 함수를 호출하세요. 이 함수는 PLAY_Stop을 호출할 때 null이 됩니다. 
	 *				다음에 PLAY_Play를 호출할 때 재설정해야 합니다. 디코딩 섹션은 속도를 제어하지 않습니다. 
	 *				콜백 함수에서 반환하면 디코더는 다음 데이터를 디코딩합니다. 이 함수를 사용하기 전에 주의하세요; 
	 *				비디오 표시 및 오디오 재생을 완전히 이해해야 합니다. 그렇지 않으면 이 함수를 함부로 사용하지 마세요!
	 * @param nPort 포트 번호
	 * @param cbDec IPlaySDKCallBack 참조, 콜백 함수 포인터. null일 수 없으며, 매개변수 정의는 다음과 같습니다:
	 *				nPort, 포트 번호
	 *				pFrameDecodeInfo, 디코딩 후 오디오 및 비디오 데이터
	 *				pFrameInfo, 이미지 및 오디오 정보. 다음 정보를 참조하세요
	 *				pUserData, 예약됨
	 * @param  pUser 사용하지 않음
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetDecodeCallBack(int nPort, fCBDecode cbDec, long pUser);
	
	/**
	 * 	설명: 비디오 데이터 콜백을 설정합니다. 스냅샷에 사용할 수 있습니다. DisplayCBFun의 콜백 함수
	 *				 포인터를 null로 설정하여 콜백을 중지할 수 있습니다. 콜백 함수는 한 번 설정되면 
	 *			     프로그램이 종료될 때까지 항상 유효합니다. 언제든지 이 함수를 호출할 수 있습니다.
	 * @param nPort 포트 번호
	 * @param DisplayCBFun IPlaySDKCallBack 참조, 비디오 데이터 콜백 함수. null일 수 있으며, 매개변수 정의는 다음과 같습니다:
	 *					   nPort, 포트 번호
	 *				       pBuf, 비디오 데이터 버퍼
	 *				       nSize, 비디오 데이터 크기
	 *				       nWidth, 이미지 너비. 단위는 픽셀
	 *				       nHeight, 이미지 높이
	 *				       nStamp, 타임 스탬프 정보. 단위는 ms
	 *				       nType, 데이터 유형. T_RGB32, T_UYVY. 매크로 정의를 참조하세요
	 *				       pUserData, 예약됨
	 * @param pUserData 사용하지 않음
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetDisplayCallBack(int nPort, fDisplayCBFun DisplayCBFun, long pUserData);
	
	/**
	 * 	설명: 스냅샷 이미지를 BMP 파일로 저장합니다. 이 스위치는 CPU 리소스를 차지합니다. 이미지를 저장할 필요가 없으면 
	 *				 이 함수를 호출하지 마세요.
	 * @param pBuf 이미지 데이터 버퍼
	 * @param nSize 이미지 데이터 크기
	 * @param nWidth 이미지 너비. 단위는 픽셀.
	 * @param nHeight 이미지 높이. 단위는 픽셀.
	 * @param nType 데이터 유형. T_RGB32, T_UYVY. 매크로 정의를 참조하세요.
	 * @param sFileName 파일 이름. 파일 확장자는 BMP.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYConvertToBmpFile(byte[] pBuf,int nSize,int nWidth,int nHeight,int nType, String sFileName);
	
	/**
	 * 설명: YUV 이미지 데이터를 jpeg 형식으로 변환합니다.
	 * @param pYUVBuf 이미지 데이터 버퍼
	 * @param nWidth 이미지 너비
	 * @param nHeight 이미지 높이
	 * @param YUVtype YUV 데이터 유형. T_YV12, T_UYVY 등.
	 * @param quality 압축 품질, (0, 100]
	 * @param sFileName 파일 이름. 파일 확장자는 jpg.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYConvertToJpegFile(byte[] pYUVBuf, int nWidth, int nHeight, int YUVtype, int quality, String sFileName);


	/**
	 * 설명: 총 프레임 수를 가져옵니다.
	 * @param nPort 포트 번호
	 * @return int, 총 프레임 수
	 */
	public native static int PLAYGetFileTotalFrames(int nPort);
	
	/**
	 * 설명: 현재 비트 스트림의 인코딩 프레임 속도를 가져옵니다.
	 * @param nPort 포트 번호
	 * @return int, 현재 비트 스트림을 인코딩할 때의 프레임 속도 값.
	 */
	public native static int PLAYGetCurrentFrameRate(int nPort);
	
	/**
	 * 설명: 파일 재생 시간을 가져옵니다. 단위는 ms
	 * @param nPort 포트 번호
	 * @return int, 파일의 현재 재생 시간
	 */
	public native static int PLAYGetPlayedTimeEx(int nPort);
	
	/**
	 * 설명: 시간에 따라 파일 재생 위치를 설정합니다. 이 인터페이스는 
	 *				PLAY_SetPlayPos보다 약간 더 오래 걸립니다. 하지만 시간을 사용하여 진행 표시줄을 제어하면 (PLAY_GetPlayedTime(Ex)와 함께 작동)
	 *				진행 표시줄이 부드럽게 움직입니다.
	 * @param nPort 포트 번호
	 * @param nTime 파일 재생 위치를 지정된 시간으로 설정합니다. 단위는 ms
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetPlayedTimeEx(int nPort,int nTime);
		
	/**
	 *  설명: 현재 재생 프레임 일련 번호를 가져옵니다. PLAY_GetPlayedFrames는 총 프레임 수입니다. 
	 *			    재생 위치가 동일하면 이 두 함수의 반환 값은 매우 가까워야 합니다, 
	 *			    데이터 손실이 발생하지 않는 한.
	 * @param nPort 포트 번호
	 * @return int, 현재 재생 프레임 번호.
	 */
	public native static int PLAYGetCurrentFrameNum(int nPort);

	/**
	 * 설명: 스트림 재생 모드를 설정합니다. 재생하기 전에 설정해야 합니다
	 * @param port 포트 번호
	 * @param mode STREAME_REALTIME 실시간 모드 (기본값); STREAME_FILE 파일 모드. 실시간 모드는 네트워크 실시간 모드에 적합합니다.
	 *			   디코더 카드가 즉시 디코딩을 시작합니다
	 *			   파일 모드는 파일 데이터를 스트림으로 입력하기에 적합합니다
	 *			   PLAY_InputData()가 FALSE를 반환하면 대기한 다음 다시 입력해야 합니다.
	 */
	public native static void PLAYSetStreamOpenMode(int port, int mode);
	
	/**
	 * 설명: 플레이어 SDK 주 버전, 부 버전 및 팩 번호를 가져옵니다.
	 * @return 상위 16비트는 현재 주 버전을 나타냅니다. 9-16은 부 버전을 나타냅니다. 1-8비트는 서브 팩 번호를 나타냅니다.
	 *		   예를 들어: 반환 값이 0x00030107이면 주 버전은 3, 부 버전은 1, 팩 번호는 7입니다.
	 */
	public native static int PLAYGetSdkVersion();
	
	/**
	 * 설명: 현재 오류 코드를 가져옵니다. 함수 호출에 실패하면 이 함수를 호출하여 오류 코드를 가져올 수 있습니다.
	 * @param nPort 포트 번호
	 * @return int, 오류 유형 참조
	 */
	public native static int PLAYGetLastError(int nPort);

	/**
	 * 설명: 표시를 새로 고칩니다. 플레이어가 일시 정지 모드일 때 새로 고치면 창 비디오가 
	 *			    사라집니다. 이 인터페이스를 호출하여 비디오를 다시 가져올 수 있습니다. 일시 정지 및 프레임별 
	 *			    재생 모드에서 유효합니다. 시스템은 다른 상황에서 직접 반환합니다.
	 * @param nPort 포트 번호
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYRefreshPlay(int nPort);

	/**
	 *  설명: 비트 스트림에서 원본 이미지 크기를 가져온 다음 그에 따라 표시 창을 조정합니다. 
	 *			     디스플레이 카드가 줌 기능을 지원할 필요가 없습니다. 하드웨어 줌을 
	 *			     지원하지 않는 디스플레이 카드에 매우 유용합니다.
	 * @param nPort 포트 번호
	 * @param pWidth  원본 이미지의 너비. PAL 형식 CIF 해상도에서는 352
	 * @param pHeight 원본 이미지의 높이. PAL 형식 CIF 해상도에서 값은 288
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYGetPictureSize(int nPort,int[] pWidth, int[] pHeight);
	
	/**
	 * 설명: 비디오 품질을 설정합니다. 높은 품질일 때 비디오가 선명하지만 CPU 소비가 
	 *			    높습니다. 시스템이 다중 채널 재생 모드일 때는 CPU 소비를 낮추기 위해 
	 *			    품질을 약간 낮출 수 있습니다. 큰 영역에서 한 창을 보려면 높은
	 *			    품질로 설정하여 우수한 비디오 효과를 얻을 수 있습니다.
	 * @param nPort 포트 번호
	 * @param bHighQuality 1일 때 비디오 품질이 최상입니다. 0일 때 비디오 품질이 가장 낮습니다(기본값).
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetPicQuality(int nPort,int bHighQuality);

	/**
	 * 설명: 공유 방식으로 오디오를 재생합니다. 다른 채널의 오디오를 비활성화하지 않고 현재 채널 오디오를 재생합니다.
	 * @param nPort 포트 번호
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYPlaySoundShare(int nPort);

	/**
	 * 설명: 공유 방식으로 오디오를 비활성화합니다. PLAY_PlaySound와 PLAY_StopSound는 
	 *			    독점 방식으로 오디오를 활성화합니다. 한 프로세스에서 모든 채널은
	 *			    동일한 방식으로 오디오를 활성화하거나 비활성화해야 합니다.
	 * @param nPort 포트 번호
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYStopSoundShare(int nPort);

	/**
	 * 설명: 스트림 모드 유형을 가져옵니다.
	 * @param nPort 포트 번호
	 * @return int, STREAME_REALTIME 또는 STREAME_FILE.
	 */
	public native static int PLAYGetStreamOpenMode(int nPort);
	
	/**
	 * 설명: 스트림 재생 모드에서 소스 버퍼에 남아 있는 데이터를 지웁니다.
	 * @param nPort 포트 번호
	 * @return int, true--성공 false--실패.
	 */
	public native static int PLAYResetSourceBuffer(int nPort);
	
	/**
	 * 설명: 프레임별 역재생. 각 콜백은 한 프레임을 반환합니다.
	 *			    파일 인덱스를 생성한 후 이 함수를 호출할 수 있습니다
	 * @param nPort 포트 번호
	 * @return int, true--성공 false--실패.
	 */
	public native static int PLAYOneByOneBack( int nPort);
	
	/**
	 * 설명: 현재 위치의 프레임 번호를 지정합니다. 
	 *				프레임 번호에 따라 재생 위치를 찾습니다. 파일 인덱스를 생성한 후 함수를 다시 호출해야 합니다.
	 * @param nPort 포트 번호
	 * @param nFrameNum 프레임 번호
	 * @return int, true--성공 false--실패.
	 */
	public native static int PLAYSetCurrentFrameNum( int nPort,int nFrameNum);

	/**
	 * 설명: 디코딩할 때 콜백 스트림 유형을 설정합니다.
	 * @param nPort 포트 번호
	 * @param nStream 1 비디오 스트림; 2 오디오 스트림; 3 복합 스트림.
	 * @return int, true--성공 false--실패.
	 */
	public native static int PLAYSetDecCBStream( int nPort,int nStream);
	
	/**
	 * 설명: 표시 영역을 설정하거나 추가합니다. 부분 확대를 지원합니다.
	 * @param nPort 포트 번호
	 * @param nRegionNum 표시 영역 일련 번호. 0~(MAX_DISPLAY_WND-1). nRegionNum이 0이면 메인 표시 창을 의미합니다.
	 * @param pSrcRect 부분 표시 영역
	 * @param hDestWnd 표시 창 핸들
	 * @param bEnable 표시 영역 열기(설정) 또는 닫기
	 * @return int, true--성공 false--실패.
	 */
	public native static int PLAYSetDisplayRegion( int nPort,int nRegionNum, CUSTOMRECT pSrcRect, Surface surface, int bEnable);

	/**
	 * 설명: 현재 surface 디바이스 컨텍스트를 가져오기 위해 콜백 함수를 등록합니다. 
	 *			창의 클라이언트 영역 DC에서처럼 DC에 그릴(쓸) 수 있습니다. 하지만 이 DC는 
	 *			창의 클라이언트 영역 DC가 아니라 DirectDraw의 Off-Screen DC입니다. 참고: 이 인터페이스는 
	 *			오버레이 surface를 사용하는 경우 null입니다. 창에 그릴 수 있습니다. 투명 색상이 아니면
	 *			표시됩니다.
	 * @param nPort 포트 번호
	 * @param DrawFun IPlaySDKCallBack 참조, 콜백 함수 핸들
	 *			      nPort, 포트 번호
	 *			      hDc, OffScreen, 표시된 창 DC를 조작하는 것처럼 조작할 수 있습니다
	 *			      pUserData, 사용자 정의 매개변수
	 * @param pUserData 사용하지 않음.
	 * @return int, true--성공 false--실패.
	 */
	public native static int PLAYRigisterDrawFun(int nPort, int regionnum, fDrawCBFun DrawCBFun, long pUserData);

	/**
	 * 설명: 플레이어 버퍼를 지웁니다.
	 * @param nPort 포트 번호
	 * @param nBufType 버퍼 유형, 매크로 정의 참조: BUF_VIDEO_SRC 1 BUF_AUDIO_SRC 2 BUF_VIDEO_RENDER 3 BUF_AUDIO_RENDER 4 
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYResetBuffer(int nPort,int nBufType);

	/**
	 * 설명: 플레이어 버퍼 크기(프레임 수 또는 바이트)를 가져옵니다. 이 인터페이스를 사용하여 
	 *				버퍼의 데이터를 가져와 네트워크 지연 시간을 추정할 수 있습니다.
	 * @param nPort 포트 번호
	 * @param nBufType BUF_VIDEO_SRC 1 BUF_AUDIO_SRC 2 BUF_VIDEO_RENDER 3 BUF_AUDIO_RENDER 4 
	 * @return int, 다양한 매개변수에 따라 다른 버퍼 값을 반환합니다. 소스 버퍼는 
	 *			   바이트를 반환하고 버퍼는 디코딩 후 프레임 수를 반환합니다
	 */
	public native static int PLAYGetBufferValue(int nPort,int nBufType);

	/**
	 * 설명: WAVE를 조정하여 볼륨을 조정합니다. 이 함수와 PLAY_SetVolume의 차이점은: 
	 *				이 함수는 오디오 데이터를 조정하며 현재 채널에만 적용됩니다. 
	 *				하지만 PLAY_SetVolume은 오디오 카드 볼륨을 조정하며 전체 시스템에 적용됩니다.
	 *				이 함수는 아직 구현되지 않았습니다.
	 * @param nPort 포트 번호
	 * @param nCoefficient 수정 매개변수. 값 범위: [MIN_WAVE_COEF, MAX_WAVE_COEF]. 0은 수정 없음을 의미합니다.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYAdjustWaveAudio(int nPort,int nCoefficient);

	/**
	 * 설명: 비디오 매개변수를 설정합니다. 설정되면 즉시 활성화됩니다.
	 * @param nPort 포트 번호
	 * @param nRegionNum 표시 영역. PLAY_SetDisplayRegion을 참조하세요. 표시 영역이 하나만 있으면 0으로 설정하세요(일반적으로 0)
	 * @param nBrightness 밝기. 기본값은 64. 값 범위는 0에서 128
	 * @param nContrast 대비. 기본값은 64. 값 범위는 0에서 128
	 * @param nSaturation 채도. 기본값은 64. 값 범위는 0에서 128
	 * @param nHue 색조. 기본값은 64. 값 범위는 0에서 128
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetColor(int nPort, int nRegionNum, int nBrightness, int nContrast, int nSaturation, int nHue);

	/**
	 * 설명: 해당 색상 값을 가져옵니다. 매개변수는 PLAY_SetColor와 동일합니다.
	 * @param nPort 포트 번호
	 * @param nRegionNum 표시 영역. PLAY_SetDisplayRegion을 참조하세요. 표시 영역이 하나만 있으면 0으로 설정하세요(일반적으로 0)
	 * @param Brightness 밝기. 기본값은 64. 값 범위는 0에서 128
	 * @param Contrast 대비. 기본값은 64. 값 범위는 0에서 128
	 * @param Saturation 채도. 기본값은 64. 값 범위는 0에서 128
	 * @param Hue 색조. 기본값은 64. 값 범위는 0에서 128
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYGetColor(int nPort, int nRegionNum, Integer Brightness, Integer Contrast, Integer Saturation, Integer Hue);

	/**
	 * 설명: 스냅샷, 지정된 파일에 사진 데이터를 씁니다. PLAY_SetDisplayCallBack 비디오 데이터를 디코딩할 때 
	 *			이 함수를 호출하면 비디오 데이터를 처리할 수 있습니다(예: 스냅샷). 계속 디코딩 데이터가 있으면 
	 *			이 콜백 함수를 계속 호출합니다. 하지만 PLAY_CatchPic은 한 번에 하나의 이미지를 스냅샷하며 
	 *			일시 정지 또는 프레임별 재생 모드에서 스냅샷을 실현할 수 있습니다. 스냅샷을 원하면
	 *			(한 번에 하나의 이미지), PLAY_CatchPic을 호출하세요. PLAY_SetDisplayCallBack을 호출하여 
	 *			일정 기간 동안 비디오 데이터를 가져올 수 있습니다.
	 * @param nPort 포트 번호
	 * @param sFileName 파일 이름
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYCatchPic(int nPort,String sFileName) ;

	/**
	 * 설명: 스냅샷. 사진 형식을 bmp 또는 jpeg로 지정할 수 있습니다.
	 * @param nPort 포트 번호
	 * @param sFileName 파일 이름
	 * @param ePicfomat 사진 형식 유형, 정의는 Constants 참조.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYCatchPicEx(int nPort,String sFileName,int ePicfomat);
	
	/**
	 * 설명: 파일 종료 콜백을 설정합니다.
	 * @param nPort 포트 번호
	 * @param pFileEndCBFun 콜백 함수 포인터
	 * @param pUserData 사용하지 않음
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetFileEndCallBack(int nPort, fpFileEndCBFun pFileEndCBFun,long pUserData);

	public native static int PLAYSetEncTypeChangeCallBackEx(int nPort, fpEncChangeCBFunEx pEncChangeCBFunEx, long pUserData);
	/**
	 * 설명: 스트림 데이터 녹화를 시작합니다. 스트림 모드에만 적용됩니다. PLAY_Play 이후에 호출하세요.
	 * @param nPort 포트 번호
	 * @param sFileName 녹화 파일 이름. 이름에 기존 폴더가 없으면 새 폴더를 생성합니다
	 * @param idataType 0 원시 비디오 스트림; 1 avi로 변환; 2 asf로 변환
	 * @param fRecordListener 녹화 콜백.
	 * @param pUserData 사용하지 않음. 
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYStartDataRecord(int nPort, String sFileName, int idataType, fRecordErrorCallBack fRecordListener, long pUserData);

	/**
	 * 설명:  Android 플랫폼에서 SurfaceTexture를 생성하기 위한 openGLES Texture 콜백을 설정합니다 
	 * @param nPort 포트 번호
	 * @param fGLESTextureFun 콜백 함수 포인터
	 * @param nRegionNum 표시 영역 번호, 기본값으로 0을 전달
	 * @param pUserData 사용자 객체 포인터
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetGLESTextureCallBack(int nPort, fGLESTextureCallback fGLESTextureFun, int nRegionNum, long pUserData);
	/**
	 * 설명:  외부에서 호출하는 렌더링 메서드
	 * @param nPort 포트 번호
	 * @param nX  뷰포트의 왼쪽
	 * @param nY  뷰포트의 아래쪽
	 * @param nWidth 뷰포트의 너비
	 * @param nHeight 뷰포트의 높이
	 * @param nRegionNum 표시 영역 번호, 기본값으로 0을 전달
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYOutsideRender(int nPort, int nX, int nY, int nWidth, int nHeight, int nRegionNum);
	/**
	 * 설명:  네이티브 스트림을 저장합니다.
	 * @param nPort 포트 번호
	 * @param pBuf 스트림 버퍼
	 * @param nSize 버퍼 크기
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYWriteData(int nPort, byte[] pBuf,int nSize);

	/**
	 * 설명: 스트림 데이터 녹화를 중지합니다.
	 * @param nPort 포트 번호
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYStopDataRecord(int nPort);

	/**
	 * 설명: 재생 속도를 변경합니다.
	 * @param nPort 포트 번호
	 * @param fCoff 재생 속도, 범위[1/64~64.0], 1보다 작으면 느리게 재생, 1보다 크면 빠르게 재생.
	 *				재생 속도가 충분히 높으면 프레임을 건너뛸 수 있습니다.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetPlaySpeed(int nPort, float fCoff);
	
	/**
	 * 설명: 정보 상태 검색 함수. 현재 시간 및 프레임 속도 정보를 검색할 수 있습니다.
	 * @param nPort 포트 번호
	 * @param cmdType 상태 검색 유형을 지정합니다.
	 * 				  PLAY_CMD_GetTime			시간 정보를 가져옵니다. 단위는 ms
	 *			 	  PLAY_CMD_GetFileRate		프레임 속도 정보를 가져옵니다
	 *			 	  PLAY_CMD_GetMediaInfo		미디어 정보를 가져옵니다, 구조체는 MEDIA_INFO
	 * @param buf  정보 버퍼
	 * @param buflen 버퍼 길이
	 * @param returnlen 유효한 데이터 길이
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYQueryInfoTime(int nPort);
	
	/**
	 * 설명: 정보를 쿼리합니다.
	 * @param nPort 포트 번호
	 * @param cmdType 상태 검색 유형을 지정합니다.
	 * 				  PLAY_CMD_GetTime			시간 정보를 가져옵니다. 단위는 ms
	 *			 	  PLAY_CMD_GetFileRate		프레임 속도 정보를 가져옵니다
	 *			 	  PLAY_CMD_GetMediaInfo		미디어 정보를 가져옵니다, 구조체는 MEDIA_INFO
	 * @param buf  정보 버퍼
	 * @param buflen 버퍼 길이
	 * @param returnlen 유효한 데이터 길이
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYQueryInfo(int nPort , int cmdType, byte[] buf, int buflen, Integer returnlen);

	/**
	 * 설명: 오디오 에코 제거를 위한 파일 디버깅 활성화 여부를 설정합니다. 기본적으로 꺼져 있으며 Android 플랫폼에서 유효합니다.
	 * @param bWriteData True는 쓰기 파일 열기, False는 쓰기 파일 닫기를 의미합니다.
	 * @param sConfigFile 매개변수 디버깅을 위한 소프트웨어 오디오 에코 제거 구성 파일 경로.
	 * @return true 성공, false 실패.
	 */
    public native static int PLAYSetAecDebug(int bWriteData, String sConfigFile);

	/**
	 * 설명: 하드웨어 오디오 에코 제거 활성화를 설정합니다. Android 플랫폼에서 사용 가능합니다.
	 * @param bEnable  하드웨어 오디오 에코 제거 활성화 여부. True는 "켜짐", FALSE는 "꺼짐", 기본값은 "꺼짐"입니다.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYAecHardwareEnable(int bEnable);
	
	/**
	 * 설명: 오디오 샘플링 작업을 활성화합니다.
	 * @param pProc IPlaySDKCallBack 참조, 오디오 샘플링 데이터 콜백 포인터, 매개변수 정의는 다음과 같습니다:
	 * 			    pDataBuffer, 데이터 콜백 포인터
	 *				DataLength, 콜백 데이터 길이
	 *				pUserData, 사용자 정의 매개변수
	 * @param nBitsPerSample 각 샘플링의 비트를 나타냅니다
	 * @param nSamplesPerSec 샘플링 속도
	 * @param nLength 데이터 버퍼 길이
	 * @param nReserved 사용하지 않음
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYOpenAudioRecord(pCallFunction pProc, int nBitsPerSample, int nSamplesPerSec, int nLength, long nReserved);
	
	/**
	 * 설명: 오디오 샘플링 기능을 비활성화합니다.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYCloseAudioRecord();
	
	/**
	 * 설명: 파일을 열고 채널 번호를 자동으로 할당합니다.
	 * @param  sFileName 파일 이름, 파일 크기 범위는 4K에서 4G
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYCreateFile(String sFileName);

	/**
	 * 설명: 파일을 닫고 자동으로 할당된 채널 번호를 해제합니다.
	 * @param  nPort 포트 번호
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYDestroyFile(int nPort);

	/**
	 * 설명: 스트림 인터페이스를 열고 채널 번호를 자동으로 할당합니다.
	 * @param nBufPoolSize 저장 데이터 스트림 버퍼 크기를 설정합니다. 값 범위는 [SOURCE_BUF_MIN,SOURCE_BUF_MAX]. 일반적으로 900*1024. 
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYCreateStream(int nBufPoolSize);
	
	/**
	 * 설명: 데이터 비트를 닫고 자동으로 할당된 포트 번호를 해제합니다.
	 * @param nPort 포트 번호 
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYDestroyStream(int nPort);

	/**
	 * 설명: 사용 가능한 포트를 가져옵니다. 
	 * @return 포트 번호. -1은 유효하지 않은 포트를 의미합니다.
	 */
	public native static int PLAYGetFreePort();
	
	/**
	 * 설명: PLAY_GetFreePort로 얻은 포트를 해제합니다.
	 * @param nPort 포트 번호
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYReleasePort(int lPort);

	/**
	 * 설명: 수직 동기화 방법, 오프스크린 모드만 지원
	 *			    PLAY_Play 이후에 호출됩니다. 동적 이미지를 표시할 때 이 방법이 유용할 수 있습니다.
	 * @param nPort 포트 번호
	 * @param bEnable TRUE 수직 동기화 활성화; FALSE 수직 동기화 비활성화.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYVerticalSyncEnable(int nPort, int bEnable);

	/**
	 * 설명: 스냅샷. BMP 사진 데이터 버퍼를 반환합니다.
	 * @param nPort 포트 번호
	 * @param pBmpBuf 사진 버퍼. 사용자가 할당, 권장 크기:
	 *				sizeof(BITMAPFILEHEADER) +
	 *				sizeof(BITMAPINFOHEADER) + w * h * 4
	 *				여기서 w는 사진 너비, h는 사진 높이
	 * @param dwBufSize 버퍼 크기
	 * @param pBmpSize BMP 사진 데이터 크기
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYGetPicBMP(int nPort, byte[] pBmpBuf, int dwBufSize, Integer pBmpSize);

	/**
	 * 설명: BMP 이미지 스냅샷.
	 * @param nPort 포트 번호
	 * @param pBmpBuf BMP 데이터를 저장할 버퍼 주소, 사용자가 할당, bmp 이미지 크기보다 커야 함,
	 *				  권장 크기: size of(BITMAPFILEHEADER)+sizeof(BITMAPINFOHEADER)+w*h*4,
	 *				  w와 h는 이미지 너비와 높이.
	 * @param dwBufSize 버퍼 영역 크기
	 * @param pBmpSize 실제 bmp의 이미지 크기 
	 * @param nWidth 지정된 bmp 너비
	 * @param nHeight 지정된 bmp 높이
	 * @param nRgbType 지정된 RGB 형식 0��RGB32;1��RGB24;
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYGetPicBMPEx(int nPort, byte[] pBmpBuf, int dwBufSize, Integer pBmpSize, int nWidth, int nHeight, int nRgbType);

	/**
	 * 설명: 스냅샷. JPEG 사진 데이터 버퍼를 반환합니다.
	 * @param nPort 포트 번호
	 * @param pJpegBuf 사진 버퍼. 사용자가 할당, 권장 크기: w * h * 3/2 (w는 사진 너비, h는 사진 높이)
	 * @param dwBufSize 버퍼 크기
	 * @param pJpegSize JPEG 사진 데이터 크기
	 * @param quality jpeg 압축 품질, 값은 (0, 100]
	 * @return true 성공, false 실패.
	 */ 
	public native static int PLAYGetPicJPEG(int nPort, byte[] pJpegBuf, int dwBufSize, Integer pJpegSize, int quality);

	/**
	 * 설명: 디코딩 호출, PLAY_SetDecCallBackEx와 거의 동일하지만, 디코딩 호출은
	 *				비디오를 동시에 표시할 수 있습니다. 표시 지연을 피하기 위해 호출 함수에서 장시간 로직 처리는 권장하지 않습니다.
	 * @param nPort 포트 번호
	 * @param cbDec IPlaySDKCallBack 참조, 호출 함수 표시기, null일 수 없으며, 매개변수 정의는 다음과 같습니다:
	 *				nPort, 포트 번호
	 *				pFrameDecodeInfo, 디코딩 후 A/V 데이터, FRAME_DECODE_INFO 구조체 참조
	 *				pFrameInfo, 이미지 및 오디오, FRAME_INFO_EX 구조체 참조
	 *				pUserData, 사용자 정의 매개변수
	 * @param pUserData 사용하지 않음
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetVisibleDecodeCallBack(int nPort, fCBDecode cbDec, long pUser);

	/**
	 * 설명: 스냅. 형식, 높이 및 너비를 설정할 수 있습니다. 
	 * @param nPort 포트 번호
	 * @param sFileName 파일 이름 
	 * @param lTargetWidth 사진 너비
	 * @param lTargetHeight 사진 높이
	 * @param ePicfomat 사진 형식 유형, 정의는 Constants 참조.
	 * @return true 성공, false 실패.
	 */ 
	public native static int PLAYCatchResizePic(int nPort, String sFileName, int lTargetWidth, int lTargetHeight, int ePicfomat);

	/**
	 * 설명: 비디오 실시간 비트 전송률을 가져옵니다.  
	 * @param nPort 포트 번호
	 * @param pBitRate 출력 매개변수, 비디오 비트 전송률을 반환 
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYGetRealFrameBitRate(int nPort, Integer pBitRate);
	
	/**
	 * 설명: 콜백 함수 포인터를 설정합니다. 콜백을 설정할 때 콜백합니다. 빠르고 
	 *			  정확하게 찾기 위해 시스템은 파일을 열 때 파일 인덱스를 생성합니다. 이 기간은 약간 
	 *			  길어질 수 있습니다. 속도는 약 40M/s이며, 주로 HDD에서 데이터를 읽는 것이 느리기 때문입니다. 인덱스 
	 *			  설정은 백그라운드에서 작동합니다. 이 인덱스를 사용해야 하는 함수는 
	 *			  이 프로세스가 끝날 때까지 기다려야 하지만, 다른 인터페이스는 영향을 받지 않을 수 있습니다.
	 * @param nPort 포트 번호
	 * @param pFileRefDoneEx IPlaySDKCallBack 참조, 콜백 함수 포인터, 매개변수 정의는 다음과 같습니다:
	 *						 nPort, 포트 번호
	 *						 bIndexCreated, 인덱스 생성 기호. TRUE=인덱스 생성 성공. FALSE=인덱스 생성 실패.
	 *						 pUserData, 사용자 정의 매개변수
	 * @param pUserData 사용하지 않음
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetFileRefCallBackEx(int nPort, fpFileRefDoneCBFunEx pFileRefDoneCBFunEx, long pUserData);

	/**
	 * 설명: 워터마크 데이터 콜백을 설정합니다.
	 * @param nPort 포트 번호
	 * @param pFunc IPlaySDKCallBack 참조, 워터마크 정보를 가져오는 콜백 함수, 매개변수 정의는 다음과 같습니다:
	 *			    nPort, 포트 번호
	 *				buf, 워터마크 데이터 버퍼
	 *				lTimeStamp, 워터마크 타임 스탬프 
	 *				lInfoType, 다른 워터마크 구분, 세 가지 유형이 있습니다: WATERMARK_DATA_TEXT,
	 *					WATERMARK_DATA_JPEG_BMP, WATERMARK_DATA_FRAMEDATA
	 *			    len, 버퍼 길이
	 *			    reallen, 버퍼 실제 길이
	 *			    lCheckResult, 1 오류 없음; 2 프레임 워터마크; 3 프레임 데이터 오류; 4 프레임 시퀀스 오류
	 *				pUserData, 사용자 정의 매개변수
	 * @param pUserData 사용하지 않음
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetWaterMarkCallBackEx(int nPort, GetWaterMarkInfoCallbackFuncEx pFunc, long pUserData);


	/**
	 * 설명: 오디오 녹음 데이터 확대/축소 비율을 설정합니다.
	 * @param pfRatio 확대/축소 비율. 0에서 1=오디오 확대. 1=원본 오디오. 1 이상=오디오 축소.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetAudioRecScaling(float fRatio);

	/**
	 * 설명: 오디오 녹음 데이터 확대/축소 비율을 가져옵니다.
	 * @param pfRatio 확대/축소 비율. 0에서 1=오디오 확대. 1=원본 오디오. 1 이상=오디오 축소.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYGetAudioRecScaling(Double pfRatio);
	
	/**
	 * 설명: 실시간 재생 지연 시간을 설정합니다.
	 * @param nPort 포트 번호
	 * @param nDelay 지연 시간
	 * @param nThreshold 임계값 시간
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetDelayTime(int nPort, int nDelay, int nThreshold);
	
	/**
	 * 설명: 플레이어에서 표시를 대체하도록 디코딩 호출을 설정합니다. 사용자가 제어하며,
	 *				PLAY_Play 전에 호출하는 함수이며, PLAY_Stop 시 자동으로 무효화됩니다. 다음 PLAY_Play 전에
	 *				다시 설정해야 합니다. 디코딩 부분은 속도를 제어하지 않으며, 사용자는 호출 함수에서 반환해야 하고, 디코더는
	 *				다음 데이터를 디코딩합니다. 표시 없이 디코딩만 가능합니다.
	 * @param nPort 포트 번호
	 * @param DecCBFun IPlaySDKCallBack 참조, 호출 함수 표시기, null일 수 없으며, 매개변수 정의는 다음과 같습니다:
	 *				nPort, 포트 번호
	 *				pBuf, 디코딩 후 A/V 데이터
	 *				nSize, 디코딩 후 A/V 데이터의 pBuf 길이
	 *				FrameInfo, 이미지 및 오디오, FRAME_INFO 구조체 참조
	 *				pUserData, 사용자 정의 매개변수
	 * @param pUserData 사용하지 않음.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetDecCallBackEx(int nPort, fVisibleDecCBFun DecCBFun, long pUserData);

	/**
	 * 설명: 디코딩 호출, PLAY_SetDecCallBackEx와 거의 동일하지만, 디코딩 호출은
	 *				비디오를 동시에 표시할 수 있습니다. 표시 지연을 피하기 위해 호출 함수에서 장시간 로직 처리는 권장하지 않습니다.
	 * @param nPort 포트 번호
	 * @param DecCBFun IPlaySDKCallBack 참조, 호출 함수 표시기, null일 수 없으며, 매개변수 정의는 다음과 같습니다:
	 *				nPort, 포트 번호
	 *				pBuf, 디코딩 후 A/V 데이터
	 *				nSize, 디코딩 후 A/V 데이터의 pBuf 길이
	 *				FrameInfo, 이미지 및 오디오, FRAME_INFO 구조체 참조
	 *				pUserData, 사용자 정의 매개변수
	 * @param pUserData 사용하지 않음.
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetVisibleDecCallBack(int nPort, fVisibleDecCBFun DecCBFun, long pUserData);

	/**
	* 설명: 사진 크기 조정
	* @param nPort 포트 번호
	* @param scale 크기 조정 비율. 범위 [1.0, 8.0] 권장, 보통은 1.0.
	* @param nRegionNum 표시 영역 일련 번호. 0~(MAX_DISPLAY_WND-1). nRegionNum이 0이면 메인 표시 창을 의미합니다.
	* @return true 성공, false 실패.
	*/
	public native static int PLAYScale(int nPort, float scale, int nRegionNum);

	/**
	* 설명: 사진 이동, PLAYGetTranslateX 및 PLAYGetTranslateY 이후에 호출됩니다.
	* @param nPort 포트 번호
	* @param x x 좌표의 절대값
	* @param y y 좌표의 절대값
	* @param nRegionNum 표시 영역 일련 번호. 0~(MAX_DISPLAY_WND-1). nRegionNum이 0이면 메인 표시 창을 의미합니다.
	* @return true 성공, false 실패.
	*/
	public native static int PLAYTranslate(int nPort, float x, float y, int nRegionNum);

	/**
	* 설명: 행렬 초기화, 이동 및 크기 조정 작업을 재설정합니다.
	* @param nPort 포트 번호
	* @param nRegionNum 표시 영역 일련 번호. 0~(MAX_DISPLAY_WND-1). nRegionNum이 0이면 메인 표시 창을 의미합니다.
	* @return true 성공, false 실패.
	*/
	public native static int PLAYSetIdentity(int nPort, int nRegionNum);

	/**
	* 설명: 현재 크기 조정 비율을 가져옵니다.
	* @param nPort 포트 번호
	* @param nRegionNum 표시 영역 일련 번호. 0~(MAX_DISPLAY_WND-1). nRegionNum이 0이면 메인 표시 창을 의미합니다.
	* @return 크기 조정 비율.
	*/
	public native static float PLAYGetScale(int nPort, int nRegionNum);
	
	public native static int PLAYGetSourceBufferRemain(int nPort);

	/**
	* 설명: 이동의 현재 x 좌표를 가져옵니다.
	* @param nPort 포트 번호
	* @param nRegionNum 표시 영역 일련 번호. 0~(MAX_DISPLAY_WND-1). nRegionNum이 0이면 메인 표시 창을 의미합니다.
	* @return x 좌표
	*/
	public native static float PLAYGetTranslateX(int nPort, int nRegionNum);

	/**
	* 설명: 이동의 현재 y 좌표를 가져옵니다.
	* @param nPort 포트 번호
	* @param nRegionNum 표시 영역 일련 번호. 0~(MAX_DISPLAY_WND-1). nRegionNum이 0이면 메인 표시 창을 의미합니다.
	* @return y 좌표
	*/
	public native static float PLAYGetTranslateY(int nPort, int nRegionNum);

	/**
	 * 설명: 소스 데이터 콜백 함수를 설정합니다.
	 * @param nPort 포트 번호
	 * @param cbFun IPlaySDKCallBack 참조, 호출 함수 표시기, null일 수 없으며, 매개변수 정의는 다음과 같습니다:
	 *				nPort, 포트 번호
	 *				pFrameData, 프레임 데이터, 개인 헤드 포함
	 *				datalen, 프레임 데이터 길이
	 *				pFrameBodyData, 본문 데이터, 개인 헤드 제외
	 *				bodylen, 본문 길이	
	 *				DemuxInfo, 데이터 프레임 정보, DEMUX_INFO 구조체 참조
	 *				pUserData, 사용자 정의 매개변수
	 * @param  pUserData 사용하지 않음.
	 * @return true 성공, false 실패.
	*/
	public native static int PLAYSetDemuxCallBack(int nPort, fDemuxCBFun cbFun, long pUserData);

	/**
	 * 설명: 로컬 파일 시작 시간 및 종료 시간 콜백 함수를 설정합니다.
	 * @param nPort 포트 번호
	 * @param FileTimeCBFun IPlaySDKCallBack 참조, 호출 함수 표시기, null일 수 없으며, 매개변수 정의는 다음과 같습니다:
	 *				nPort, 포트 번호
	 *				nStarTime, 로컬 파일 시작 시간, 1970/1/1.0.0.0부터 총 초.
	 *				nEndTime, 로컬 파일 종료 시간, 1970/1/1.0.0.0부터 총 초.
	 *				pUserData, 사용자 정의 매개변수
	 * @param  pUserData 사용하지 않음.
	 * @return true 성공, false 실패.
	*/
	public native static int PLAYSetFileTimeDoneCallBack(int nPort, fFileTimeDoneCBFun FileTimeCBFun, long pUserData);

	/**
	 * 설명: 재생 전략 설정, 실시간 전용
	 * @param nPort 포트 번호
	 * @param nStartTime 내부 버퍼가 nStartTime보다 크면 처음 재생 시작
	 * @param nSlowTime 내부 버퍼가 nSlowTime보다 작으면 낮은 속도로 재생 시작
	 * @param nFastTime 내부 버퍼가 nFastTime보다 크면 더 빠른 속도로 재생 시작
	 * @param nFailedTime 내부 버퍼가 nFailedTime보다 크면 InputData 인터페이스가 실패
	 * @return true 성공, false 실패.
	*/
	public native static int PLAYSetPlayMethod(int nPort, int nStartTime, int nSlowTime, int nFastTime, int nFailedTime);

	/**
	 * 설명: 화면 지우기, PLAYStop 전에 호출합니다.
	 * @param nPort 포트 번호
	 * @param red rgba 값.
	 * @param nRegionNum 기본값 0.
	 * @return true 성공, false 실패.
	*/
	public native static int PLAYCleanScreen(int nPort, float red, float green, float blue, float alpha, int nRegionNum);


	/**
	 * 설명: aes 암호화 키를 설정합니다.
	 * @param nPort 포트 번호
	 * @param szKey 키(문자열).
	 * @param nKeylen szKey의 길이.
	 * @return true 성공, false 실패.
	*/
	public native static int PLAYSetSecurityKey(int nPort, byte[] szKey, int nKeylen);
	
	/**
	 * 설명: Play_Play 전에 호출
	 * @param nPort 포트 번호
	 * @param nThreadNumber 스레드 번호 (1 - 8)
	 * @return true 성공, false 실패.
	*/
	public native static int PLAYSetDecodeThreadNum(int nPort, int nThreadNumber);

	/**
	 * 설명: 실시간 스트림에 사용, PLAYInputData 전에 호출
	 * @param nPort 포트 번호
	 * @param nCacheMode 캐시 모드 (0: 끄기  1: 적응형  2: 실시간 우선 3: 유창성 우선)
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetCacheMode(int nPort, int nCacheMode);
	
	/**
	 * 설명: Play_Play 전에 호출
	 * @param nPort 포트 번호
	 * @param DecodeType 디코딩 타입
	 * @param RenderType 렌더링 타입
	 * @return true 성공, false 실패.
	*/
	public native static int PLAYSetEngine(int nPort, int DecodeType, int RenderType);
	
	/**
	 * 설명: Play_Play 전에 호출
	 * @param nPort 포트 번호
	 * @param nAVSyncType AV 동기화 타입
	 * @return true 성공, false 실패.
	*/
	public native static int PLAYSetAVSyncType(int nPort, int nAVSyncType);
	
	/**
	 * 설명: Play_Play 전에 호출
	 * @param nPort 포트 번호
	 * @param rotateAngle 0: 0�� 1:90�� 2:180��  3:270��
	 * @return true 성공, false 실패.
	*/
	public native static int PLAYSetRotateAngle(int nPort, int rotateAngle);
	
	/**
	 * 설명: 로그 출력 스위치
	 * @param enable 1은 켜기, 0은 끄기를 의미
	 * @return true 성공, false 실패.
	*/
	public native static void PLAYSetPrintLogSwitch(int enable);

    /**
     * 설명: 지능형 정보 스위치
     * @param nPort 포트 번호
     * @param enable 1은 켜기, 0은 끄기를 의미
     * @return true 성공, false 실패.
    */
    public native static void PLAYRenderPrivateData(int nPort, int enable);
    
    /**
     * 설명: 마지막 프레임 플러시
     * @param nPort 포트 번호
     * @return true 성공, false 실패.
    */
    public native static int PLAYFlush(int nPort);
	
	/**
     * 설명: 변경된 뷰의 너비와 높이를 설정합니다
     * @param nPort 포트 번호
	 * @param nWidth 변경된 뷰의 너비
	 * @param nHeight 변경된 뷰의 높이
	 * @param nRegionNum 표시 영역 일련 번호. 0~(MAX_DISPLAY_WND-1). nRegionNum이 0이면 메인 표시 창을 의미합니다.
     * @return true 성공, false 실패.
    */
	public native static int PLAYViewResolutionChanged(int nPort, int nWidth, int nHeight, int nRegionNum);
	
	/**
     * 설명: 오디오 재생 모드를 설정합니다
     * @param nPort 포트 번호
	 * @param nMode 재생 모드
     * @return true 성공, false 실패.
    */
	public native static int PLAYSetAudioPlaybackMode(int nPort, int nMode);
	
	/**
	 * 고화질 이미지 내부 조정 전략 활성화 여부.
	 *
	 * @param[in] nPort 포트 번호
	 * @param[in] nType 프레임 드롭 유형
	 * @return true 성공, false 실패.
	*/
	public native static int PLAYEnableLargePicAdjustment(int nPort, int nType);


	/**
	 * 지능형 유형 그리기 활성화 설정
	 * @param nPort 포트 번호
	 * @param nIvsType 지능형 유형
	 * @param bEnable 활성화 여부
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetIvsEnable(int nPort, int nIvsType, int bEnable);

	/**
	 * 트랙 텍스 그리기 활성화 설정
	 * @param nPort 포트 번호
	 * @param trackex2config 트랙 텍스 유형
	 * @return true 성공, false 실패.
	 */
	public native static int PLAYSetIVSTrackEx2Config(int nPort, IVSDRAWER_TrackEx2Config trackex2config);

	/************************************************************************/
	/* 이전 자바 인터페이스 구현                                         */
	/************************************************************************/
	public static int PLAYSurfaceChange(int port, SurfaceView surfaceView)
	{
		return PLAYSetDisplayRegion(port, 0, null, surfaceView != null ? surfaceView.getHolder().getSurface() : (Surface)null, 1);
	}

	public static int PLAYSetDisplayRegion( int nPort,int nRegionNum, CUSTOMRECT pSrcRect, SurfaceView surfaceView, int bEnable)
	{
		return PLAYSetDisplayRegion(nPort, nRegionNum, pSrcRect, surfaceView != null ? surfaceView.getHolder().getSurface() : (Surface)null, bEnable);
	}
	
	public static int PLAYPlay(int port, SurfaceView surfaceView)
	{
		return PLAYPlay(port, surfaceView != null ? surfaceView.getHolder().getSurface() : (Surface)null);
	}
	
	public static int InitSurface(int port, SurfaceView surfaceView)
	{
		return PLAYSetDisplayRegion(port, 0, null, surfaceView != null ? surfaceView.getHolder().getSurface() : (Surface)null, 1);
	}
	
	public static int UinitSurface(int port)
	{
		return PLAYSetDisplayRegion(port, 0, null, (Surface)null, 0);
	}
}
