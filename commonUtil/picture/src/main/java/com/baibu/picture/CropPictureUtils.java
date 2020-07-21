package com.baibu.picture;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

import androidx.core.content.FileProvider;

/**
 * @author Tangzhiquan
 * @date 2020/7/10
 * @detail 裁剪图片工具类
 */
public class CropPictureUtils {
    public static final int CROP_ACTIVITY_RESULT = 3;

    private Activity activity;
    private Uri imageUri;

    public CropPictureUtils(Activity activity) {
        this.activity = activity;
    }


    /***
     * 剪切图片
     * @param uri
     * @return
     */
    @SuppressLint("SdCardPath")
    public Uri crop(Uri uri, String tempName) {
        imageUri = Uri.parse(String.format("file://%s%s.jpg", Environment.getExternalStorageDirectory().getPath(), tempName));

        Intent intent = new Intent("com.android.camera.action.CROP");

        //但裁剪了后想对裁剪后的图片继续裁剪的话，此时的uri因为是file开头，所以要转换成content开头
        //Android 7.0（API24）对文件的安全性做了控制，只能通过FileProvide添加临时权限来读写文件
        String scheme = uri.getScheme();
        if (TextUtils.equals(ContentResolver.SCHEME_FILE, scheme)){
            try {
                File file = new File(new URI(uri.toString()));
                uri = FileProvider.getUriForFile(activity, "com.baibu.bussiness.fileProvider", file);
                /**添加这一句表示对目标应用临时授权该Uri所代表的文件*/
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        /**图片源*/
        intent.setDataAndType(uri, "image/*");
        /**支持裁剪*/
        intent.putExtra("crop", "true");
        /**可缩放*/
        intent.putExtra("scale", true);
        /**返回编辑后的图片数据，当为true时，通过Intent中的data来传递，当数据过大，即超过1M（经测试，这个数值在不同手机还不一样）时会崩*/
        /**返回编辑后的图片数据，当为false时，将图片保存在Uri中，故编辑后的图片尽管很大，也不会崩溃*/
        intent.putExtra("return-data", false);
        /**编辑后的图片保存路径*/
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        /**编辑后的图片格式*/
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        /**人脸识别*/
        intent.putExtra("noFaceDetection", false);
        activity.startActivityForResult(intent, CROP_ACTIVITY_RESULT);

        return imageUri;
    }

    /***
     * uri转换成Bitmap
     * @param uri
     * @return
     */
    public Bitmap decodeUriAsBitmap(Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(activity.getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    public static void delFile(Context context, Uri uri) {
        final String scheme = uri.getScheme();

        if (TextUtils.equals(ContentResolver.SCHEME_CONTENT, scheme)) {
            context.getContentResolver().delete(uri, null, null);
        } else if (TextUtils.equals(ContentResolver.SCHEME_FILE, scheme)){
            File file = new File(getRealFilePath(context, uri));
            if (file.exists()){
                file.delete();
                //删除后同步更新图库
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(file));
                context.sendBroadcast(intent);
            }
        }
    }


    /**
     * Try to return the absolute file path from the given Uri
     *
     * @param context
     * @param uri
     * @return the file path or null
     */
    public static String getRealFilePath(final Context context, final Uri uri ) {
        if (null == uri) {
            return null;
        }
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }
}
