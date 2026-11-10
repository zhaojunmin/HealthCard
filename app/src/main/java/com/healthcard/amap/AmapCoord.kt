package com.healthcard.amap

import android.content.Context
import com.amap.api.maps.CoordinateConverter
import com.amap.api.maps.model.LatLng
import com.healthcard.location.GeoPoint

/** 坐标转换工具：系统 GPS(WGS-84) → 高德(GCJ-02)。 */
object AmapCoord {
    fun wgsToGcj(context: Context, p: GeoPoint): RoutePoint {
        AmapPrivacy.ensure(context)
        return try {
            val c = CoordinateConverter(context)
                .from(CoordinateConverter.CoordType.GPS)
                .coord(LatLng(p.latitude, p.longitude))
                .convert()
            RoutePoint(c.latitude, c.longitude)
        } catch (e: Exception) {
            RoutePoint(p.latitude, p.longitude)
        }
    }
}
