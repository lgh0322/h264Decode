package com.vaca.h264decode

import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import com.vaca.h264decode.databinding.FragmentServerBinding
import com.vaca.h264decode.server.Server
import com.vaca.h264decode.server.VideoDecoder
import kotlinx.coroutines.*
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.lang.Runnable
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

class ServerFragment : Fragment() {
    lateinit var binding: FragmentServerBinding


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding= FragmentServerBinding.inflate(inflater,container,false)

        val mSurfaceView =binding.sur
        val mServer = Server.getInstance();
        val mVideoDecoder =  VideoDecoder(mSurfaceView.getHolder().getSurface(), mServer);
        mVideoDecoder.start()


        return binding.root
    }



}