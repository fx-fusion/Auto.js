package org.autojs.autojs.build;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.stardust.pio.UncheckedIOException;
import com.stardust.util.DeveloperUtils;

import org.autojs.autojs.BuildConfig;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Stardust on 2017/11/29.
 */

public class ApkBuilderPluginHelper {

    private static final String PLUGIN_PACKAGE_NAME = "org.autojs.apkbuilderplugin";
    private static final String TEMPLATE_APK_PATH = "template.apk";
    private static final boolean DEBUG_APK_PLUGIN = false;

    public static boolean isPluginAvailable(Context context) {
        return DeveloperUtils.checkSignature(context, PLUGIN_PACKAGE_NAME);
    }

    public static InputStream openTemplateApk(@NonNull Context context) {
        try {
            if (DEBUG_APK_PLUGIN && BuildConfig.DEBUG) {
                return context.getAssets().open(TEMPLATE_APK_PATH);
            }
            return context.getPackageManager().getResourcesForApplication(PLUGIN_PACKAGE_NAME)
                    .getAssets().open(TEMPLATE_APK_PATH);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getPluginVersion(@NonNull Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(PLUGIN_PACKAGE_NAME, 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    public static int getSuitablePluginVersion() {
        return BuildConfig.VERSION_CODE - 200;
    }
}


