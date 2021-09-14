package com.vaca.h264decode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController

import com.vaca.h264decode.databinding.FragmentMainBinding

class MainFragment : Fragment() {
    lateinit var binding: FragmentMainBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        binding.client.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_clientFragment)
        }


        binding.server.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_serverFragment)
        }

        return binding.root
    }

}