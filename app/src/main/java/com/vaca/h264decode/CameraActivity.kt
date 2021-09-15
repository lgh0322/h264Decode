package com.vaca.h264decode

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.hardware.Camera.PreviewCallback
import android.media.MediaCodec

import com.vaca.h264decode.CameraActivity.H264FileTask
import android.os.Bundle
import com.vaca.h264decode.R
import com.vaca.h264decode.CameraActivity
import android.graphics.ImageFormat
import android.graphics.Paint
import android.hardware.Camera
import kotlin.Throws
import android.media.MediaFormat
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaCodecInfo.CodecCapabilities
import android.util.Log
import android.view.*
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.*
import java.nio.ByteBuffer
import java.util.ArrayList

@SuppressLint("NewApi")
class CameraActivity : AppCompatActivity(), SurfaceHolder.Callback, PreviewCallback,
    View.OnClickListener {
    private val bOpening = false
    private var surfaceView: SurfaceView? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var mSurface: Surface? = null
    private var mCamera: Camera? = null
    private var mCameraWidth = 0
    private var mCameraHeight = 0
    private var mSurfaceWidth = 0
    private var mSurfaceHeight = 0
    private var mMediaEncoder: MediaCodec? = null
    private var mMediaDecoder: MediaCodec? = null
    private var paint: Paint? = null
    private var address: InetAddress? = null
    private var socket: DatagramSocket? = null

    private var h264FileTask: H264FileTask? = null
    private var mFrameIndex = 0
    private lateinit var mEncoderH264Buf: ByteArray
    private var mMediaHead: ByteArray? = null
    private val mYuvBuffer = ByteArray(1280 * 720 * 3 / 2)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_camera)
        findViewById<View>(R.id.btnOpen).setOnClickListener(this)
        findViewById<View>(R.id.btnClose).setOnClickListener(this)
        mCameraWidth = 640
        mCameraHeight = 480
        if (!setupView()) {
            Log.e(TAG, "failed to setupView")
            return
        }
        paint = Paint()
        mMediaEncoder = null
        mMediaDecoder = null
        mSurface = null
        mEncoderH264Buf = ByteArray(10240)

    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnOpen -> {

            }
            R.id.btnClose -> {

            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceCreated.")
    }

    override fun surfaceChanged(
        holder: SurfaceHolder, format: Int, width: Int,
        height: Int
    ) {
        Log.i(TAG, "surfaceChanged w:$width h:$height")
        mSurface = surfaceHolder!!.surface
        mSurfaceWidth = width
        mSurfaceHeight = height

        /*
         * get canvas from surface holder with draw something
         * */
//        Surface surface = holder.getSurface();
//        if(!setupDecoder(surface,"video/avc",mCameraWidth,mCameraHeight)){
//			releaseCamera();
//			Log.e(TAG, "failed to setupEncoder");
//			return;
//		}

//      Canvas c = holder.lockCanvas();
//		paint.setColor(Color.WHITE);
//		paint.setStrokeWidth(4);
//		c.drawRect(0, 0, width, height, paint);
//		paint.setColor(Color.BLACK);
//		paint.setTextSize(30);
//		paint.setStrokeWidth(1);        
//		c.drawText("http://blog.csdn.net/", 100, 100, paint);


//        Bitmap bitmap = null;
//        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.waiting);
//        if (bitmap != null) {
//            float left=(c.getWidth()-bitmap.getWidth())/2;
//            float top=(c.getHeight()-bitmap.getHeight())/2;
//            c.drawBitmap(bitmap, left, top, paint);
//        }
//
//        holder.unlockCanvasAndPost(c);
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceDestroyed")
        mSurface = null

    }

    override fun onPreviewFrame(rawData: ByteArray, camera: Camera) {
        val w = camera.parameters.previewSize.width
        val h = camera.parameters.previewSize.height

        if (mMediaEncoder == null) {
            try {
                setupEncoder("video/avc", w, h)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        assert(mSurface != null)
        if (mMediaDecoder == null) {
            try {
                setupDecoder(mSurface, "video/avc", w, h)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        //convert yv12 to i420
        swapYV12toI420(rawData, mYuvBuffer, w, h)
        System.arraycopy(mYuvBuffer, 0, rawData, 0, rawData.size)

        //set h264 buffer to zero.
        for (i in mEncoderH264Buf.indices) mEncoderH264Buf[i] = 0
        val encoderRet = offerEncoder(rawData, mEncoderH264Buf)
        if (encoderRet > 0) {
            Log.d(TAG, "encoder output h264 buffer len:$encoderRet")
            /**
             * send to VLC client by udp://@port;
             */
//            netSendTask!!.pushBuf(mEncoderH264Buf, encoderRet)
            /**
             * push data to decoder
             */
            offerDecoder(mEncoderH264Buf, encoderRet)
        }


        //reset buff to camera.
        camera.addCallbackBuffer(rawData)
    }

    /**
     *
     */
    private fun setupView(): Boolean {
        Log.d(TAG, "fall in setupView")
        if (null != surfaceHolder) {
            surfaceHolder!!.removeCallback(this)
            surfaceView = null
        }
        if (null != surfaceView) {
            surfaceView = null
        }
        surfaceView = findViewById<View>(R.id.surfaceView) as SurfaceView
        surfaceHolder = surfaceView!!.holder
        surfaceHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        surfaceHolder!!.addCallback(this)
        return true
    }


    @Throws(IOException::class)
    private fun setupEncoder(mime: String, width: Int, height: Int): Boolean {
        val colorFormat = selectColorFormat(selectCodec(mime), mime)
        Log.d(TAG, "setupEncoder $mime colorFormat:$colorFormat w:$width h:$height")
        mMediaEncoder = MediaCodec.createEncoderByType(mime)
        val mediaFormat = MediaFormat.createVideoFormat(mime, width, height)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 125000)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
        mMediaEncoder!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mMediaEncoder!!.start()
        return true
    }

    @Throws(IOException::class)
    private fun setupDecoder(surface: Surface?, mime: String, width: Int, height: Int): Boolean {
        Log.d(TAG, "setupDecoder surface:$surface mime:$mime w:$width h:$height")
        val mediaFormat = MediaFormat.createVideoFormat(mime, width, height)
        mMediaDecoder = MediaCodec.createDecoderByType(mime)
        if (mMediaDecoder == null) {
            Log.e("DecodeActivity", "createDecoderByType fail!")
            return false
        }

        mMediaDecoder!!.configure(mediaFormat, surface, null, 0)
        mMediaDecoder!!.start()
        return true
    }

    private fun offerEncoder(input: ByteArray, output: ByteArray): Int {
        var pos = 0
        try {
            val inputBuffers = mMediaEncoder!!.inputBuffers
            val outputBuffers = mMediaEncoder!!.outputBuffers
            val inputBufferIndex = mMediaEncoder!!.dequeueInputBuffer(-1)
            if (inputBufferIndex >= 0) {
                val inputBuffer = inputBuffers[inputBufferIndex]
                Log.d(
                    TAG,
                    "offerEncoder InputBufSize: " + inputBuffer.capacity() + " inputSize: " + input.size + " bytes"
                )
                inputBuffer.clear()
                inputBuffer.put(input)
                mMediaEncoder!!.queueInputBuffer(inputBufferIndex, 0, input.size, 0, 0)
            }
            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = mMediaEncoder!!.dequeueOutputBuffer(bufferInfo, 0)
            while (outputBufferIndex >= 0) {
                val outputBuffer = outputBuffers[outputBufferIndex]
                val data = ByteArray(bufferInfo.size)
                outputBuffer[data]
                Log.d(
                    TAG,
                    "offerEncoder InputBufSize:" + outputBuffer.capacity() + " outputSize:" + data.size + " bytes written"
                )
                if (mMediaHead != null) {
                    System.arraycopy(data, 0, output, pos, data.size)
                    pos += data.size
                } else {
                    Log.d(TAG, "offer Encoder save sps head,len:" + data.size)
                    val spsPpsBuffer = ByteBuffer.wrap(data)
                    if (spsPpsBuffer.int == 0x00000001) {
                        mMediaHead = ByteArray(data.size)
                        System.arraycopy(data, 0, mMediaHead, 0, data.size)
                    } else {
                        Log.e(TAG, "not found media head.")
                        return -1
                    }
                }
                mMediaEncoder!!.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = mMediaEncoder!!.dequeueOutputBuffer(bufferInfo, 0)
            }
            if (output[4] == 0x65.toByte()) //key frame   ���������ɹؼ�֡ʱֻ�� 00 00 00 01 65 û��pps sps�� Ҫ����
            {
                System.arraycopy(output, 0, input, 0, pos)
                System.arraycopy(mMediaHead, 0, output, 0, mMediaHead!!.size)
                System.arraycopy(input, 0, output, mMediaHead!!.size, pos)
                pos += mMediaHead!!.size
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return pos
    }

    private fun offerDecoder(input: ByteArray, length: Int) {
        try {
            val inputBuffers = mMediaDecoder!!.inputBuffers
            val inputBufferIndex = mMediaDecoder!!.dequeueInputBuffer(-1)
            if (inputBufferIndex >= 0) {
                val inputBuffer = inputBuffers[inputBufferIndex]
                val timestamp = (mFrameIndex++ * 1000000 / FRAME_RATE).toLong()
                Log.d(TAG, "offerDecoder timestamp: $timestamp inputSize: $length bytes")
                inputBuffer.clear()
                inputBuffer.put(input, 0, length)
                mMediaDecoder!!.queueInputBuffer(inputBufferIndex, 0, length, timestamp, 0)
            }
            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = mMediaDecoder!!.dequeueOutputBuffer(bufferInfo, 0)
            while (outputBufferIndex >= 0) {
                Log.d(TAG, "offerDecoder OutputBufSize:" + bufferInfo.size + " bytes written")

                //If a valid surface was specified when configuring the codec, 
                //passing true renders this output buffer to the surface.  
                mMediaDecoder!!.releaseOutputBuffer(outputBufferIndex, true)
                outputBufferIndex = mMediaDecoder!!.dequeueOutputBuffer(bufferInfo, 0)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun startPlayH264File() {
        assert(mSurface != null)
        if (mMediaDecoder == null) {
            if (!setupDecoder(mSurface, "video/avc", mSurfaceWidth, mSurfaceHeight)) {
                Log.e(TAG, "failed to setupDecoder")
                return
            }
        }
        h264FileTask = H264FileTask()
        h264FileTask!!.start()
    }

    private fun swapYV12toI420(
        yv12bytes: ByteArray,
        i420bytes: ByteArray,
        width: Int,
        height: Int
    ) {
        System.arraycopy(yv12bytes, 0, i420bytes, 0, width * height)
        System.arraycopy(
            yv12bytes,
            width * height + width * height / 4,
            i420bytes,
            width * height,
            width * height / 4
        )
        System.arraycopy(
            yv12bytes,
            width * height,
            i420bytes,
            width * height + width * height / 4,
            width * height / 4
        )
    }

    private fun readH264FromFile() {
        val file = File(H264FILE)
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "failed to open h264 file.")
            return
        }
        try {
            var len = 0
            val fis = FileInputStream(file)
            val buf = ByteArray(1024)
            while (fis.read(buf).also { len = it } > 0) {
                offerDecoder(buf, len)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return
    }

    internal inner class H264FileTask : Thread() {
        override fun run() {
            Log.d(TAG, "fall in H264File Read thread")
            readH264FromFile()
        }
    }


    companion object {
        private const val TAG = "StudyCamera"
        private const val FRAME_RATE = 15
        private const val REMOTE_HOST = "192.168.1.105"
        private const val REMOTE_HOST_PORT: Short = 5000
        private const val H264FILE = "/sdcard/test.h264"


        private fun selectCodec(mimeType: String): MediaCodecInfo? {
            val numCodecs = MediaCodecList.getCodecCount()
            for (i in 0 until numCodecs) {
                val codecInfo = MediaCodecList.getCodecInfoAt(i)
                if (!codecInfo.isEncoder) {
                    continue
                }
                val types = codecInfo.supportedTypes
                for (j in types.indices) {
                    if (types[j].equals(mimeType, ignoreCase = true)) {
                        return codecInfo
                    }
                }
            }
            return null
        }

        /**
         * Returns a color format that is supported by the codec and by this test code.  If no
         * match is found, this throws a test failure -- the set of formats known to the test
         * should be expanded for new platforms.
         */
        private fun selectColorFormat(codecInfo: MediaCodecInfo?, mimeType: String): Int {
            val capabilities = codecInfo!!.getCapabilitiesForType(mimeType)
            for (i in capabilities.colorFormats.indices) {
                val colorFormat = capabilities.colorFormats[i]
                if (isRecognizedFormat(colorFormat)) {
                    return colorFormat
                }
            }
            Log.e(TAG, "couldn't find a good color format for " + codecInfo.name + " / " + mimeType)
            return 0 // not reached
        }

        /**
         * Returns true if this is a color format that this test code understands (i.e. we know how
         * to read and generate frames in this format).
         */
        private fun isRecognizedFormat(colorFormat: Int): Boolean {
            return when (colorFormat) {
                CodecCapabilities.COLOR_FormatYUV420Planar, CodecCapabilities.COLOR_FormatYUV420PackedPlanar, CodecCapabilities.COLOR_FormatYUV420SemiPlanar, CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar, CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar -> true
                else -> false
            }
        }
    }
}