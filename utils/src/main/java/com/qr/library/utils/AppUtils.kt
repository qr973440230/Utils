package com.qr.library.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.telephony.TelephonyManager
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.app.ActivityCompat
import java.io.File

/**
 * App工具类
 */
object AppUtils {

    /**
     * 获取SD卡路径
     *
     * @return 如果sd卡不存在则返回null
     */
    private val sdPath: File?
        get() {
            var sdDir: File? = null
            val sdCardExist = Environment.getExternalStorageState() == Environment
                .MEDIA_MOUNTED
            if (sdCardExist) {
                sdDir = Environment.getExternalStorageDirectory()
            }
            return sdDir
        }

    /**
     * 获取版本名称
     */
    private fun getAppVersionName(context: Context): String? {
        var versionName: String? = null
        try {
            val pm = context.packageManager
            val pi = pm.getPackageInfo(context.packageName, 0)
            versionName = pi.versionName
            if (versionName == null || versionName.isEmpty()) {
                return ""
            }
        } catch (e: Exception) {
            Log.e("VersionInfo", "Exception", e)
        }

        return versionName
    }

    /**
     * 获取版本号
     */
    private fun getAppVersionCode(context: Context): Int {
        var versioncode = -1
        try {
            // ---get the package info---
            val pm = context.packageManager
            val pi = pm.getPackageInfo(context.packageName, 0)
            versioncode = pi.versionCode
        } catch (e: Exception) {
            Log.e("VersionInfo", "Exception", e)
        }

        return versioncode
    }

    @Suppress("DEPRECATION")
    @SuppressLint("HardwareIds")
    private fun getIMEI(context: Context): String? {
        val tm = context.getSystemService(
            Context
                .TELEPHONY_SERVICE
        ) as TelephonyManager
        return if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tm.imei
            } else {
                tm.deviceId
            }
        } else null
    }

    /**
     * 显示软键盘
     */
    private fun openSoftInput(et: EditText) {
        val inputMethodManager = et.context
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(et, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    /**
     * 隐藏软键盘
     */
    private fun hideSoftInput(et: EditText) {
        val inputMethodManager = et.context
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            et.windowToken, InputMethodManager
                .HIDE_NOT_ALWAYS
        )
    }

    /**
     * 安装文件
     * @param data
     */
    private fun promptInstall(context: Context, data: Uri) {
        val promptInstall = Intent(Intent.ACTION_VIEW)
            .setDataAndType(data, "application/vnd.android.package-archive")
        // FLAG_ACTIVITY_NEW_TASK 可以保证安装成功时可以正常打开 app
        promptInstall.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(promptInstall)
    }
}
