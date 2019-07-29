package com.dongxie.mqttdemo

import android.app.Application
import android.content.Context

/**
 *
 * @ClassName:      MyApplication
 * @Description:
 * @Author:         dongxie
 * @CreateDate:     2019/7/29 10:39
 */
class MyApplication : Application() {
    companion object {
        var context: Context? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}