package com.healthcard.amap

import android.content.Context
import com.amap.api.maps.MapsInitializer

/**
 * 高德地图 SDK 合规初始化：在创建任何地图/坐标组件前必须先声明隐私政策已展示并同意。
 * 幂等，可多次调用。
 */
object AmapPrivacy {
    fun ensure(context: Context) {
        val app = context.applicationContext
        MapsInitializer.updatePrivacyShow(app, true, true)
        MapsInitializer.updatePrivacyAgree(app, true)
    }
}
