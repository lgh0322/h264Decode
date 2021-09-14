package com.vaca.h264decode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.vaca.h264decode.client.Client

import com.vaca.h264decode.databinding.FragmentClientBinding
import com.vaca.h264decode.utils.PathUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Response
import java.io.*
import java.lang.Exception
import java.net.Socket
import java.net.UnknownHostException
import kotlin.experimental.inv
import android.annotation.SuppressLint
import java.util.*


class ClientFragment:Fragment() {
    lateinit var binding: FragmentClientBinding
    lateinit var wifiManager: WifiManager

    private var mClient: Client? = null
    private var count = 0



    private fun intToIp(paramInt: Int): String {
        return ((paramInt.and(255)).toString() + "." + (paramInt.shr(8).and(255)) + "." + (paramInt.shr(16).and(255)) + "."
                + (paramInt.shr(24).and(255)))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        wifiManager = MainApplication.application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val gate=intToIp(wifiManager.dhcpInfo.gateway)

        Client.HOST_ADDRESS=gate




        binding= FragmentClientBinding.inflate(inflater,container,false)


        val fileBytes = File(PathUtil.getPathX("w.h264")).readBytes()


        binding.sps.setOnClickListener(object : View.OnClickListener {
           override fun onClick(v: View?) {
                // TODO Auto-generated method stub
                Thread(java.lang.Runnable {
                    mClient = Client.getInstance()
                    mClient?.connect()
                    start@ for (i in 0 until fileBytes.size) {
                        if (fileBytes[i] == 0.toByte() && fileBytes[i + 1] == 0.toByte() && fileBytes[i + 2] == 0.toByte() && fileBytes[i + 3] == 1.toByte()) {
                            end@ for (j in i + 4 until fileBytes.size) {
                                if (fileBytes[j] == 0.toByte() && fileBytes[j + 1] == 0.toByte() && fileBytes[j + 2] == 0.toByte() && fileBytes[j + 3] == 1.toByte()) {
                                    val temp: ByteArray = Arrays.copyOfRange(
                                        fileBytes, i, j
                                    )
                                    mClient?.sendLength(intToBytes(temp.size))
                                    mClient?.sendSPSPPS(temp)
                                    count++
                                    if (count === 2) {
                                        MainScope().launch {
                                            binding.sps.setEnabled(
                                                false
                                            )
                                        }
                                        return@Runnable
                                    }
                                    break@end
                                }
                            }
                        }
                    }
                }).start()
            }
        })
        binding.frame.setOnClickListener(object : View.OnClickListener {
          override  fun onClick(v: View?) {
                // TODO Auto-generated method stub
                Thread { // TODO Auto-generated method stub
                    start@ for (i in 0 until fileBytes.size) {
                        if (fileBytes[i] == 0.toByte() && fileBytes[i + 1] == 0.toByte() && fileBytes[i + 2] == 0.toByte() && fileBytes[i + 3] == 1.toByte()
                        ) {
                            end@ for (j in i + 4 until fileBytes.size) {
                                if (fileBytes[j] == 0.toByte() && fileBytes[j + 1] == 0.toByte() && fileBytes[j + 2] == 0.toByte() && fileBytes[j + 3] == 1.toByte()
                                ) {
                                    val temp: ByteArray = Arrays.copyOfRange(
                                        fileBytes, i, j
                                    )
                                    mClient?.sendLength(intToBytes(temp.size))
                                    mClient?.sendFrame(temp)
                                    break@end
                                }
                            }
                        }
                    }
                }.start()
            }
        })

        return binding.root
    }


    private fun getByte(path: String): ByteArray? {
        val f = File(path)
        val `in`: InputStream
        var bytes: ByteArray? = null
        try {
            `in` = FileInputStream(f)
            bytes = ByteArray(f.length().toInt())
            `in`.read(bytes)
            `in`.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bytes
    }

    fun intToBytes(i: Int): ByteArray {
        val bytes = ByteArray(4)
        bytes[0] = (i.and(0xff)).toByte()
        bytes[1] = (i.shr(8) .and( 0xff)).toByte()
        bytes[2] = (i. shr (16).and (0xff)).toByte()
        bytes[3] = (i. shr( 24) .and( 0xff)).toByte()
        return bytes
    }




}