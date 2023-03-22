package com.example.beaconforstaywatch

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.beaconforstaywatch.databinding.ActivityMainBinding
import com.example.beaconforstaywatch.service.ForegroundIbeaconOutputService
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val PERMISSION_REQUEST_CODE = 1

    val permissions = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
        arrayOf(
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    }else{
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!EasyPermissions.hasPermissions(this, *permissions)) {
            // パーミッションが許可されていない時の処理
            EasyPermissions.requestPermissions(this, "パーミッションに関する説明", PERMISSION_REQUEST_CODE, *permissions)
        }

        //ibeaconの出力を開始する。
        binding.ibeeconStartButton.setOnClickListener {

            val intent = Intent(this, ForegroundIbeaconOutputService::class.java)
            //値をintentした時に受け渡しをする用
            intent.putExtra("UUID",binding.uuidEditTextbox.text.toString())
            intent.putExtra("MAJOR",binding.majarEditTextbox.text.toString())
            intent.putExtra("MINOR",binding.minorEditTextbox.text.toString())

            //サービスの開始
            //パーミッションの確認をする。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && EasyPermissions.hasPermissions(this, *permissions)) {
                startForegroundService(intent)
            }

        }

        binding.ibeaconStopButton.setOnClickListener {
            val targetIntent = Intent(this, ForegroundIbeaconOutputService::class.java)
            stopService(targetIntent)
        }
    }

}