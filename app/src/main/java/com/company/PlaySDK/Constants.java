package com.company.PlaySDK;

public class Constants
{
	/************************************************************************/
	/* Macro Definition			                                                    */
	/************************************************************************/
	public static final int  FUNC_MAX_PORT   = 501;		//max play channel amount

	public static final int MIN_WAVE_COEF    = -100;	//used by PLAY_AdjustWaveAudio
	public static final int MAX_WAVE_COEF    = 100;		//used by PLAY_AdjustWaveAudio

	public static final int TIMER_1	         = 1;	    //Default timer. Accurate time. Support max 16 in one progress
	public static final int TIMER_2			 = 2;	    //Thread timer. No amount limit.

	public static final int BUF_VIDEO_SRC	 = 1;		//Video source buffer
	public static final int BUF_AUDIO_SRC	 = 2;		//Audio source buffer
	public static final int BUF_VIDEO_RENDER = 3;		//Video data buffer after decoding
	public static final int BUF_AUDIO_RENDER = 4;		//Audio data buffer after decoding

	public static final int MAX_DISPLAY_WND			= 		4;  //Support max 4 zones in one time

	public static final int DISPLAY_NORMAL			= 		1;	// In normal resolution
	public static final int DISPLAY_QUARTER			= 		2;	//In 1/4 resolution

	public static final int MAX_DIS_FRAMES			= 		50;	//Max decode buffer amount
	public static final int MIN_DIS_FRAMES			= 		6;	//Min decode buffer amount

	public static final int BY_FRAMENUM				=		1;	//In accordance with frame number
	public static final int BY_FRAMETIME			=		2;	//In accordance with time

	public static final int SOURCE_BUF_MAX			=		1024*100000;	//Max original buffer
	public static final int SOURCE_BUF_MIN			=		1024*50;		//Min original buffer

	public static final int STREAME_REALTIME		=		0;		//The real-time has the highest priority
	public static final int STREAME_FILE			=		1;		//The fluency has the highest priority

	public static final int T_AUDIO16				=		101;		//16bits audio data type
	public static final int T_AUDIO8				=		100;		//8bits audio data type

	public static final int T_UYVY					=		1;		//YUV data, type is UYVY, not supported now.
	public static final int T_IYUV					=		3;		//YUV data, type is IYUV(I420)
	public static final int T_RGB32					=		7;		//RGB32 type

	public static final int SUPPORT_DDRAW			=		1;		//Support DIRECTDRAW. If not, player can not work
	public static final int SUPPORT_BLT				=		2;		//Display card support BLT operation. If not, player cannot work
	public static final int SUPPORT_BLTFOURCC		=		4;		//Display card BLT supports color switch
	public static final int SUPPORT_BLTSHRINKX		=		8;		//Display car BLT supports X axis zoom out
	public static final int SUPPORT_BLTSHRINKY		=		16;		//Display card BLT supports Y axis zoom out
	public static final int SUPPORT_BLTSTRETCHX		=		32;		//Display card BLT supports X axis zoom in
	public static final int SUPPORT_BLTSTRETCHY		=		64;		//Display card BLT supports Y axis zoom in
	public static final int SUPPORT_SSE				=		128;	//CPU support SSE command,Intel Pentium3 or higher support SSE command
	public static final int SUPPORT_MMX				=		256;	//CPU support MMX instruction set

	public static final int PLAY_CMD_GetTime		=		1;		//time info
	public static final int PLAY_CMD_GetFileRate	=		2;		//frame rate info
	public static final int PLAY_CMD_GetMediaInfo	=		3;		//media info
	public static final int PLAY_CMD_GetRenderNum	=		4;		//current frame num for rendering
	public static final int PLAY_CMD_GetRenderTime	=		5;		//current time for rendering
	public static final int PLAY_CMD_GetSrcTime		=		6;      //original time

	public static final int AVI_MEDIACHANGE_FRAMERATE	=	1;		//frame rate changed
	public static final int AVI_MEDIACHANGE_RESOLUTION	=	2;		//frame resolution changed

	public static final int WATERMARK_DATA_TEXT			=	0;		//text
	public static final int WATERMARK_DATA_JPEG_BMP		=	1;		//JPEG or BMP
	public static final int WATERMARK_DATA_FRAMEDATA	=	3;		//frame data

	public static final int	PLAY_NOERROR				=	0;		//No error
	public static final int PLAY_PARA_OVER				=	1;		//Illegal input parameter
	public static final int PLAY_ORDER_ERROR			=	2;		//Callabck sequence is not right
	public static final int PLAY_TIMER_ERROR			=	3;		// Failed to set multiple-media setup
	public static final int PLAY_DEC_VIDEO_ERROR		=	4;		//Failed to decode video
	public static final int PLAY_DEC_AUDIO_ERROR		=	5;		//Failed to decode audio
	public static final int PLAY_ALLOC_MEMORY_ERROR		=	6;		//Failed to allocate meomory
	public static final int PLAY_OPEN_FILE_ERROR		=	7;		//file operation failed
	public static final int PLAY_CREATE_OBJ_ERROR		=	8;		//Failed to create thread events
	public static final int PLAY_CREATE_DDRAW_ERROR		=	9;		//Failed to create directDraw
	public static final int PLAY_CREATE_OFFSCREEN_ERROR = 10	;	//Failed to create back-end buffer
	public static final int PLAY_BUF_OVER				=	11;		//Buffer is full. Input stream failed
	public static final int PLAY_CREATE_SOUND_ERROR		=	12;		//Failed to create audio device
	public static final int PLAY_SET_VOLUME_ERROR		=	13;		//Failed to set volume
	public static final int PLAY_SUPPORT_FILE_ONLY		=	14;		//Only valid when play file
	public static final int PLAY_SUPPORT_STREAM_ONLY	=	15;		//Only valid when play stream
	public static final int PLAY_SYS_NOT_SUPPORT		=	16;		//System does not support. Decoder can work in Pentium 3 or higher
	public static final int PLAY_FILEHEADER_UNKNOWN		=	17;		//No file head
	public static final int PLAY_VERSION_INCORRECT		=	18;		//Decoder and encoder version are not compatilble
	public static final int PLAY_INIT_DECODER_ERROR		=	19;		//Failed to initialize decoder
	public static final int PLAY_CHECK_FILE_ERROR		=	20;		//File is too short or bit stream can not be recognized
	public static final int PLAY_INIT_TIMER_ERROR		=	21;		//Failed to initialize MMTimer
	public static final int PLAY_BLT_ERROR				=	22;	    //Bit copy failed
	public static final int PLAY_UPDATE_ERROR			=	23;	    //Failed to display overlay
	public static final int PLAY_MEMORY_TOOSMALL		=	24;		//memory too small
	
	public static final int FRAME_TYPE_VIDEO = 0;				//Video Frame
	public static final int FRAME_TYPE_AUDIO = 1;				//Audio Frame
	
	public static final int PicFormat_BMP 		= 0;		//BMP 32bit
	public static final int PicFormat_JPEG 		= 1;		//JPEG
	public static final int PicFormat_JPEG_70   = 2;		//70% JPEG
	public static final int PicFormat_JPEG_50   = 3;		//50% JPEG
	public static final int PicFormat_JPEG_30   = 4;		//30% JPEG
	public static final int PicFormat_JPEG_10   = 5;		//10% JPEG
	public static final int PicFormat_BMP24     = 6;        //BMP24
	
	//SetEngine
	public static final int DECODE_NOTSET 	= 0;
	public static final int DECODE_SW 		= 1;
	public static final int DECODE_HW 		= 2;
	public static final int DECODE_HW_FAST 	= 3;
	public static final int DECODE_HW_TEXTURE	= 4;
	public static final int RENDER_NOTSET 	= 0;

	// AEC Param
	public static final int AEC_SW			= 0;
	public static final int AEC_HW			= 1;

	//SetAVSyncType
	public static final int AV_SYNC_VIDEO_MASTER 	= 0;
	public static final int AV_SYNC_TIME_STAMP 		= 1;

	//IVSDRAWER TYPE
	public static final int IVSDRAWER_TRACK = 1;  //IVS Track
	public static final int IVSDRAWER_ALARM = 2;  //IVS ALARM
	public static final int IVSDRAWER_RULE = 3;   //IVS Rule
	public static final int IVSDRAWER_TRACKEX2 = 14; //IVS TrackEx
}
