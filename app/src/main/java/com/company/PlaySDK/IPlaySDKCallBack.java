package com.company.PlaySDK;

import android.view.Surface;

public class IPlaySDKCallBack
{

	public static class FRAME_INFO
	{
		public int			nWidth;					//너비, 단위는 픽셀, 오디오 데이터의 경우 0.
		public int			nHeight;				//높이, 오디오 데이터의 경우 0
		public int			nStamp;					//타임 스탬프 정보, 단위는 ms
		public int			nType;					//비디오 프레임 유형, T_AUDIO16, T_RGB32, T_IYUV
		public int			nFrameRate;				//인코딩 중 생성된 이미지 프레임 속도
	};

	public static class FRAME_INFO_EX
	{
		public int				nFrameType;				//프레임 유형
		public int				nFrameSeq;				//프레임 일련 번호
		public int				nStamp;					//프레임 시간, 밀리초
		public int				nWidth;					//너비, 단위는 픽셀, 오디오 데이터의 경우 0.
		public int 				nHeight;				//높이, 오디오 데이터의 경우 0.
		public int				nFrameRate;				//인코딩 중 생성된 이미지 프레임 속도
		public int				nChannels;				//오디오 채널 번호.
		public int				nBitPerSample;			//오디오 샘플링 비트
		public int				nSamplesPerSec;			//오디오 샘플링 주파수
		public int				nYear;					//년
		public int				nMonth;					//월
		public int				nDay;					//일
		public int				nHour;					//시
		public int				nMinute;				//분
		public int				nSecond;				//초
		public int				nReserved[] = new int[64]; //예약됨
	};

	public static class FRAME_DECODE_INFO
	{
		public int				nFrameType;				//프레임 유형, FRAME_INFO_EX nFrameType 텍스트로 정의
		public byte[]			pAudioData;          	//오디오 프레임인 경우 오디오 데이터
		public int				nAudioDataLen;			//오디오 데이터 길이
		
		public byte[]			pVideoDataY;			//YUV 컴포넌트로서
		public byte[]			pVideoDataU;
		public byte[]			pVideoDataV;
		public int				nStride[] = new int[3];		  //YUV 컴포넌트 간 간격
		public int				nWidth[]  = new int[3];	      //YUV 컴포넌트의 너비
		public int				nHeight[] = new int[3];	      //YUV 컴포넌트의 높이
		public int				nReserved[] = new int[64]; 	  //예약됨
	};

	public static class DEMUX_INFO
	{
		public int type;			// 1:비디오, 2:오디오, 3:데이터
		public int subtype;		    // I 프레임, BP 프레임, PCM8, MS-ADPCM 등.
		public int encode;			// MPEG4, H264, STDH264
		public int sequence;		// 프레임 시퀀스

		public int width;			// 너비, 단위는 픽셀, 오디오 데이터의 경우 0.
		public int height;			// 높이, 단위는 픽셀, 오디오 데이터의 경우 0
		public int rate;			// 인코딩 중 생성된 이미지 프레임 속도
	
		public int year;			// 시간 정보
		public int month;
		public int day;
		public int hour;
		public int minute;
		public int secode;
		public int timestamp;       // 타임 스탬프 정보, 단위는 ms
		
		public int channels;		// 오디오 채널 번호.
		public int bitspersample;	// 오디오 샘플링 비트
		public int samplespersecond;// 오디오 샘플링 주파수
	};


	/************************************************************************/
	/* 콜백 인터페이스	                                                   */
	/************************************************************************/
	/** 
	 * 설명: 오디오 비디오(stride 포함) 디코딩 또는 렌더 데이터 콜백, PLAYSetDecodeCallBack, PLAYSetVisibleDecodeCallBack에서 사용.
	 * @param nPort 포트 번호.
	 * @param pFrameDecodeInfo 다음과 같이 정의된 클래스:\n
	 *		  public int			nFrameType;	    프레임 유형, FRAME_INFO_EX nFrameType 텍스트로 정의. \n
	 *		  public byte[]			pAudioData;     오디오 프레임인 경우 오디오 데이터. \n
	 *	      public int			nAudioDataLen;  오디오 데이터 길이. \n
	 *	      public byte[]			pVideoDataY;	Y 컴포넌트. \n
	 *	      public byte[]			pVideoDataU;    U 컴포넌트. \n
	 *	      public byte[]			pVideoDataV;    V 컴포넌트. \n
	 *	      public int			nStride[] = new int[3];	  YUV 컴포넌트 간 간격. \n
	 *	      public int			nWidth[]  = new int[3];	  YUV 컴포넌트의 너비. \n
	 *	      public int			nHeight[] = new int[3];	  YUV 컴포넌트의 높이. \n
	 *	      public int			nReserved[] = new int[64]; 예약됨.\n
	 * @param pFrameInfo 다음과 같이 정의된 클래스: \n
	 * 		  public int				nFrameType;		프레임 유형. \n
	 *		  public int				nFrameSeq;		프레임 일련 번호. \n
	 *		  public int				nStamp;			프레임 시간, 밀리초. \n
	 *		  public int				nWidth;			너비, 단위는 픽셀, 오디오 데이터의 경우 0.\n
	 *		  public int 				nHeight;		높이, 오디오 데이터의 경우 0.\n
	 *		  public int				nFrameRate;		인코딩 중 생성된 이미지 프레임 속도.\n
	 *		  public int				nChannels;		오디오 채널 번호.\n
	 *		  public int				nBitPerSample;	오디오 샘플링 비트.\n
	 *		  public int				nSamplesPerSec;	오디오 샘플링 주파수.\n
	 *		  public int				nYear;			시간 정보. \n
	 *		  public int				nMonth;			\n
	 *		  public int				nDay;			\n
	 *		  public int				nHour;			\n
	 *		  public int				nMinute;		\n
	 *		  public int				nSecond;		\n
	 * 		  public int				nReserved[] = new int[64]; 예약됨
	 * @param pUserData 예약됨.
	 */
	public interface fCBDecode {
		public void invoke(int nPort, FRAME_DECODE_INFO pFrameDecodeInfo, FRAME_INFO_EX pFrameInfo,	long pUserData);
	}

	/** 
	 * 설명: 비디오 스냅 콜백, PLAYSetDisplayCallBack에서 사용.
	 * @param nPort 포트 번호.
	 * @param pBuf 버퍼 포인터.
	 * @param nSize 버퍼 길이.
	 * @param nWidth 사진 너비.
	 * @param nHeight 사진 높이.
	 * @param nStamp 사진 타임스탬프.
	 * @param nType 데이터 유형, 자세한 내용은 Constants 참조. T_AUDIO16 T_RGB32 T_IYUV.
	 * @param pUserData 예약됨.
	 */
	public interface fDisplayCBFun {
		public void invoke(int nPort,byte[] pBuf,int nSize,int nWidth,int nHeight,int nStamp,int nType, long pUserData);
	}

	/** 
	 * 설명: 현재 surface 디바이스 컨텍스트를 가져옵니다, PLAYRigisterDrawFun에서 사용.
	 * @param nPort 포트 번호.
	 * @param hDc hdc.
	 * @param pUserData 예약됨.
	 */
	public interface fDrawCBFun {
		public void invoke(int nPort,int regionnum, long eglContext, long pUserData);
	}
	
	/** 
	 * 설명: 파일 종료 완료 콜백, PLAYSetFileEndCallBack에서 사용.
	 * @param nPort 포트 번호.
	 * @param pUserData 예약됨.
	 */
	public interface fpEncChangeCBFunEx {
		public void invoke(int nPort, int width, int height, long pUserData);
	}
	
	/** 
	 * 설명: 인코딩 변경 콜백 Ex, PLAYSetFileEndCallBackEx에서 사용.
	 * @param nPort 포트 번호.
	 * @param pUserData 예약됨.
	 */
	public interface fpFileEndCBFun {
		public void invoke(int nPort, long pUserData);
	}

	/** 
	 * 설명: 파일 참조 완료 콜백, PLAYSetFileRefCallBackEx에서 사용.
	 * @param nPort 포트 번호.
	 * @param bIndexCreated 1은 인덱스 생성 성공을 의미.
	 * @param pUserData 예약됨.
	 */
	public interface fpFileRefDoneCBFunEx {
		public void invoke(int nPort, int bIndexCreated, long pUserData);
	}

	/** 
	 * 설명: 워터마크 콜백, PLAYSetWaterMarkCallBackEx에서 사용.
	 * @param nPort 포트 번호.
	 * @param buf 버퍼 포인터.
	 * @param lTimeStamp 프레임 타임스탬프. 
	 * @param lInfoType 정보 유형
	 * @param len 길이
	 * @param reallen 실제 길이
	 * @param lCheckResult 검사 결과
	 * @param pUserData 예약됨.
	 */
	public interface GetWaterMarkInfoCallbackFuncEx {
		public void invoke(int nPort, byte[] buf, int lTimeStamp, int lInfoType, int len, int reallen, int lCheckResult, long pUserData);
	}

	/** 
	 * 설명: 오디오 비디오(stride 없음) 렌더 데이터 콜백, PLAYSetDecCallBackEx 또는 PLAYSetVisibleDecCallBack에서 사용.
	 * @param nPort 포트 번호.
	 * @param pBuf  버퍼 포인터.
	 * @param nSize 버퍼 길이.
	 * @param FrameInfo 다음과 같이 정의된 클래스:\n
	 * 				public int			nWidth;	    너비, 단위는 픽셀, 오디오 데이터의 경우 0.\n
	 *				public int			nHeight;    높이, 오디오 데이터의 경우 0. \n
	 *				public int			nStamp;	    타임 스탬프 정보, 단위는 ms. \n
	 *				public int			nType;		비디오 프레임 유형, T_AUDIO16, T_RGB32, T_IYUV. \n
	 *				public int			nFrameRate;	인코딩 중 생성된 이미지 프레임 속도 
	 * @param pUserData 예약됨.
	 */
	public interface fVisibleDecCBFun {
		public void invoke(int nPort,byte[] pBuf,int nSize,FRAME_INFO FrameInfo, long pUserData);
	}

	/** 
	 * 설명: 오디오 녹음 콜백, PLAYOpenAudioRecord에서 사용.
	 * @param pDataBuffer 버퍼 포인터.
	 * @param nBufferLen  버퍼 길이.
	 * @param pUserData 예약됨.
	 */
	public interface pCallFunction {
		public void invoke(byte[] pDataBuffer,int nBufferLen, long pUserData);
	}

	/** 
	 * 설명: 소스 데이터 콜백, PLAYSetDemuxCallBack에서 사용.
	 * @param nPort 포트 번호.
	 * @param pFrameData 프레임 포인터, 헤드 포함.
	 * @param datalen    프레임 길이.
	 * @param pFrameBodyData 스트림 포인터.
	 * @param bodylen		 스트림 길이.
	 * @param DemuxInfo 다음과 같이 정의된 클래스:\n
	 *					public int type;			1:비디오, 2:오디오, 3:데이터 \n
	 *					public int subtype;		    I 프레임, BP 프레임, PCM8, MS-ADPCM 등.\n
	 *					public int encode;			MPEG4, H264, STDH264. \n
	 *					public int sequence;		프레임 시퀀스. \n
	 *					public int width;			너비, 단위는 픽셀, 오디오 데이터의 경우 0.\n
	 *					public int height;			높이, 단위는 픽셀, 오디오 데이터의 경우 0. \n
     *					public int rate;			인코딩 중 생성된 이미지 프레임 속도. \n					
	 * 					public int year;			시간 정보. \n
	 *					public int month; \n
	 *					public int day;   \n
 	 *					public int hour;  \n
	 *					public int minute;\n
	 *					public int secode;\n  
	 *					public int timestamp;       타임 스탬프 정보, 단위는 ms. \n		
	 *					public int channels;		오디오 채널 번호.\n
	 *					public int bitspersample;	오디오 샘플링 비트.\n
	 *					public int samplespersecond;오디오 샘플링 주파수.
	 * @param pUserData 예약됨.
	 */
	public interface fDemuxCBFun {
		public void invoke(int nPort, byte[] pFrameData, int datalen, byte[] pFrameBodyData, int bodylen, DEMUX_INFO DemuxInfo, long pUserData);
	}

	/** 
	 * 설명: 로컬 파일 시작 시간, 종료 시간 콜백, PLAYSetFileTimeDoneCallBack에서 사용.
	 * @param nPort 포트 번호.
	 * @param nStarTime 파일 시작 시간.
	 * @param nEndTime  파일 종료 시간.
	 * @param pUserData 예약됨.
	 */
	public interface fFileTimeDoneCBFun {
		public void invoke(int nPort, int nStarTime, int nEndTime, long pUserData);
	}
	
	/** 
	 * 설명: 녹화 오류 콜백, PLAYStartDataRecord에서 사용.
	 * @param nPort 포트 번호.
	 * @param pUserData 예약됨.
	 */
	public interface fRecordErrorCallBack {
		public void invoke(int nPort, long pUserData);
	}

	public interface fGLESTextureCallback {
		public Surface invoke(int nTextureID, long pUserData);
	}
}
