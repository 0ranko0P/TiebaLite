package com.huanchengfly.tieba.post.core.network.model.web;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import com.huanchengfly.tieba.post.core.network.model.UploadResultBean;
import com.huanchengfly.tieba.post.core.network.model.WebUploadPicBean;

import java.io.File;

public final class PhotoInfoBean {
    private Uri fileUri;
    private File file;
    private WebUploadPicBean webUploadPicBean;
    private UploadResultBean uploadResult;

    public PhotoInfoBean(Context context, Uri fileUri) {
        this(context, fileUri, null);
    }

    public PhotoInfoBean(Context context, Uri fileUri, UploadResultBean uploadResult) {
        this.fileUri = fileUri;
        this.uploadResult = uploadResult;
        try {
            this.file = new File(getRealPathFromUri(context, fileUri));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public WebUploadPicBean getWebUploadPicBean() {
        return webUploadPicBean;
    }

    public void setWebUploadPicBean(WebUploadPicBean webUploadPicBean) {
        this.webUploadPicBean = webUploadPicBean;
    }

    public Uri getFileUri() {
        return fileUri;
    }

    public PhotoInfoBean setFileUri(Uri fileUri) {
        this.fileUri = fileUri;
        return this;
    }

    @Nullable
    public File getFile() {
        return file;
    }

    public UploadResultBean getUploadResult() {
        return uploadResult;
    }

    public PhotoInfoBean setUploadResult(UploadResultBean uploadResult) {
        this.uploadResult = uploadResult;
        return this;
    }

    private static String getRealPathFromUri(Context context, Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        try (Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null)) {
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }
        }
        return "";
    }
}
