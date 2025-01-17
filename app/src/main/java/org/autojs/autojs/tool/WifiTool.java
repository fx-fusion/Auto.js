package org.autojs.autojs.tool;

import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by Stardust on 2017/5/11.
 */

public class WifiTool {

    @Nullable
    public static String getWifiAddress(@NonNull Context context) {
        WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiMgr == null) {
            return null;
        }
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        return Formatter.formatIpAddress(ip);
    }

    @Nullable
    public static String getRouterIp(@NonNull Context context) {
        WifiManager wifiService = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiService == null) {
            return null;
        }
        DhcpInfo dhcpInfo = wifiService.getDhcpInfo();
        return Formatter.formatIpAddress(dhcpInfo.gateway);
    }
}
