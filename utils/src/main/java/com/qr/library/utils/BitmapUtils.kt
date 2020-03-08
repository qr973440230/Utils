package com.qr.library.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import java.io.*
import kotlin.math.roundToInt

/**
 * Bitmap工具类
 */
object BitmapUtils {

    /**
     * 将一个view转换成bitmap位图
     *
     * @param view 要转换的View
     * @return view转换的bitmap
     */
    fun viewToBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(
            view.measuredWidth, view.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        view.draw(Canvas(bitmap))
        return bitmap
    }

    /**
     * 获取模糊虚化的bitmap
     *
     * @param context
     * @param bitmap  要模糊的图片
     * @param radius  模糊等级 >=0 && <=25
     * @return
     */
    fun getBlurBitmap(context: Context, bitmap: Bitmap, radius: Int): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            blurBitmap(context, bitmap, radius)
        } else bitmap
    }

    /**
     * android系统的模糊方法
     *
     * @param bitmap 要模糊的图片
     * @param radius 模糊等级 >=0 && <=25
     */
    private fun blurBitmap(context: Context, bitmap: Bitmap, radius: Int): Bitmap {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val outBitmap = Bitmap.createBitmap(
                bitmap.width, bitmap.height, Bitmap
                    .Config.ARGB_8888
            )
            val rs = RenderScript.create(context)
            val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

            val allIn = Allocation.createFromBitmap(rs, bitmap)
            val allOut = Allocation.createFromBitmap(rs, outBitmap)

            blurScript.setRadius(radius.toFloat())
            blurScript.setInput(allIn)
            blurScript.forEach(allOut)
            allOut.copyTo(outBitmap)
            bitmap.recycle()
            rs.destroy()
            return outBitmap
        } else {
            return bitmap
        }
    }

    /**
     * 根据资源获取Bitmap
     */
    fun getFitSampleBitmap(resources: Resources, id: Int, width: Int, height: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(resources, id, options)
        options.inSampleSize = getFitInSampleSize(height, width, options)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(resources, id)
    }

    /**
     * 按图片尺寸压缩 参数是bitmap
     * @param bitmap
     * @param pixelW
     * @param pixelH
     * @return
     */
    fun compressImageFromBitmap(bitmap: Bitmap, pixelW: Int, pixelH: Int): Bitmap? {
        val os = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
        if (os.toByteArray().size / 1024 > 512) {//判断如果图片大于0.5M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
            os.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, os)//这里压缩50%，把压缩后的数据存放到baos中
        }

        var byteArrayInputStream = ByteArrayInputStream(os.toByteArray())
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        options.inPreferredConfig = Bitmap.Config.RGB_565
        BitmapFactory.decodeStream(byteArrayInputStream, null, options)
        options.inJustDecodeBounds = false
        options.inSampleSize =
            getFitInSampleSize(if (pixelH > pixelW) pixelW else pixelH, pixelW * pixelH, options)
        byteArrayInputStream = ByteArrayInputStream(os.toByteArray())
        return BitmapFactory.decodeStream(byteArrayInputStream, null, options)
    }

    /**
     * 根据文件路径获取Bitmap
     */
    private fun getFitSampleBitmap(path: String, width: Int, height: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = getFitInSampleSize(height, width, options)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    /**
     * 根据字节流获取Bitmap
     */
    fun getFitSimpleBitmap(
        inputStream: InputStream,
        filePath: String,
        width: Int,
        height: Int
    ): Bitmap {
        return getFitSampleBitmap(createStreamToFile(filePath, inputStream), width, height)
    }

    fun createStreamToFile(path: String, inputStream: InputStream): String {

        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()

        val outputStream = FileOutputStream(file)
        val byte = ByteArray(1024)
        var len: Int
        while ((inputStream.read(byte)) != -1) {
            len = inputStream.read(byte)
            outputStream.write(byte, 0, len)
        }

        inputStream.close()
        outputStream.close()
        return path

    }


    /**
     * 获取压缩比例
     */
    private fun getFitInSampleSize(height: Int, width: Int, options: BitmapFactory.Options): Int {
        var inSampleSize = 1
        if (options.outWidth > width || options.outHeight > height) {
            val widthRadio: Int = (options.outWidth.toFloat() / width.toFloat()).roundToInt()
            val heightRadio: Int = (options.outHeight.toFloat() / height.toFloat()).roundToInt()
            inSampleSize = Math.min(widthRadio, heightRadio)
        }
        return inSampleSize
    }

    /**
     * Drawable To Bitmap
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val w = drawable.intrinsicWidth
        val h = drawable.intrinsicHeight
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * 压缩图片质量直到<50kb
     */
    fun compressBitmap(bitmap: Bitmap): Bitmap? {
        val byteOutStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteOutStream)
        var options = 100

        //循环判断如果压缩后图片是否大于50kb,大于继续压缩
        while (byteOutStream.toByteArray().size / 1024 > 50) {
            byteOutStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, byteOutStream)
            options -= 10//每次都减少10
        }

        val byteInStream = ByteArrayInputStream(byteOutStream.toByteArray())
        return BitmapFactory.decodeStream(byteInStream, null, null)
    }

    fun compressBitmapToFile(bitmap: Bitmap, file: File, quality: Int = 100): Boolean {
        return FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }
    }
}