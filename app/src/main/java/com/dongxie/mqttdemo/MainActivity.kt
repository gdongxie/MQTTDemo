package com.dongxie.mqttdemo

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.dongxie.mqttdemo.permissions.DialogHelper
import com.dongxie.mqttdemo.permissions.PermissionConstants
import com.dongxie.mqttdemo.permissions.PermissionUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission()
    }

    private fun requestPermission() {
        PermissionUtils.permission(PermissionConstants.PHONE)
            .rationale {
                Log.e("MainActivity", "onDenied: 权限被拒绝后弹框提示")
                DialogHelper.showRationaleDialog(it, this@MainActivity)
            }.callback(object : PermissionUtils.FullCallback {
                override fun onGranted(permissionsGranted: MutableList<String>?) {
                    var intent = Intent(this@MainActivity, MyMQTTService::class.java)
                    startService(intent)

                }

                override fun onDenied(
                    permissionsDeniedForever: MutableList<String>?,
                    permissionsDenied: MutableList<String>?
                ) {
                    Log.e("MainActivity", "onDenied: 权限被拒绝")
                    if (permissionsDeniedForever!!.isNotEmpty()) {
                        DialogHelper.showOpenAppSettingDialog(this@MainActivity)
                    }
                }
            }).request()


    }


    public fun publish(view: View) {
        //模拟闸机设备发送消息过来
        MyMQTTService.publish("publish_test")

    }


    override fun onDestroy() {
        super.onDestroy()

        var intent = Intent(this@MainActivity, MyMQTTService::class.java)
        stopService(intent)
    }

}
