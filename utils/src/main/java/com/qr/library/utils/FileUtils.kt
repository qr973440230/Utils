package com.qr.library.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import java.io.File
import java.io.IOException
import java.util.*

object FileUtils {
    fun createTemporaryWritableApkFile(context: Context): File {
        val externalFilesDir =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return createTemporaryWritableFile(
            ".apk",
            externalFilesDir
        )
    }

    fun createTemporaryWritableImageFile(context: Context): File {
        val externalFilesDir =
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return createTemporaryWritableFile(
            ".jpg",
            externalFilesDir
        )
    }

    fun createTemporaryWritableVideoFile(context: Context): File {
        val externalFilesDir =
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return createTemporaryWritableFile(
            ".mp4",
            externalFilesDir
        )
    }

    private fun createTemporaryWritableFile(
        suffix: String,
        externalFilesDirectory: File?
    ): File {
        val filename = UUID.randomUUID().toString()
        val image: File
        image = try {
            File.createTempFile(filename, suffix, externalFilesDirectory)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return image
    }

    fun getAbsolutePath(
        context: Context,
        uri: Uri
    ): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(
                        uri
                    )
                ) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        return Environment.getExternalStorageDirectory()
                            .toString() + "/" + split[1]
                    }
                } else if (isDownloadsDocument(
                        uri
                    )
                ) {
                    val id = DocumentsContract.getDocumentId(uri)
                    if (!TextUtils.isEmpty(id)) {
                        return try {
                            val contentUri = ContentUris.withAppendedId(
                                Uri.parse(Environment.DIRECTORY_DOWNLOADS),
                                java.lang.Long.valueOf(id)
                            )
                            getDataColumn(
                                context,
                                contentUri,
                                null,
                                null
                            )
                        } catch (e: NumberFormatException) {
                            null
                        }
                    }
                } else if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    if ("image" == type) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if ("video" == type) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else if ("audio" == type) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    val selection = "_id=?"
                    val selectionArgs =
                        arrayOf(split[1])
                    return getDataColumn(
                        context,
                        contentUri,
                        selection,
                        selectionArgs
                    )
                }
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            // Return the remote address
            return if (isGooglePhotosUri(uri)) {
                null
            } else getDataColumn(
                context,
                uri,
                null,
                null
            )
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        val column = "_data"
        val projection = arrayOf(column)
        context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            .use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(column)
                    return if (columnIndex == -1) {
                        null
                    } else cursor.getString(columnIndex)
                }
            }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.contentprovider" == uri.authority
    }

}