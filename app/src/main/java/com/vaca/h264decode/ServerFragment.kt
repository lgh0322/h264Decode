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
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import com.vaca.h264decode.databinding.FragmentServerBinding
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

    lateinit var wifiManager: WifiManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        wifiManager =
            MainApplication.application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager





        return binding.root
    }



}