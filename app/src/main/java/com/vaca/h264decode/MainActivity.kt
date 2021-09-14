package com.vaca.h264decode

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vaca.h264decode.databinding.ActivityMainBinding
import com.vaca.h264decode.server.Server
import com.vaca.h264decode.server.VideoDecoder
import com.vaca.h264decode.utils.PathUtil
import java.io.File

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PathUtil.initVar(this)
        File( PathUtil.getPathX("fuck.txt")).writeBytes(byteArrayOf(0x67.toByte()))
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}