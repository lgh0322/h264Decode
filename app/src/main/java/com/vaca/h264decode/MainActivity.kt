package com.vaca.h264decode

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vaca.h264decode.databinding.ActivityMainBinding
import com.vaca.h264decode.server.Server
import com.vaca.h264decode.server.VideoDecoder

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mSurfaceView=binding.fuck
        val mServer= Server.getInstance()
        val mVideoDecoder= VideoDecoder(
            mSurfaceView.holder.surface,
            mServer
        )
        mVideoDecoder.start()
    }
}