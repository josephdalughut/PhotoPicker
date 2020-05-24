package io.github.josephdalughut.android.photopicker.main;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.github.josephdalughut.android.photopicker.R;

import static com.yalantis.ucrop.UCrop.REQUEST_CROP;

/**
 * Created by Joseph Dalughut on 2019-09-10
 * Copyright © 2019
 *
 * Base class for each kind of photo picker.
 */
public abstract class PhotoPicker {

    private static final String LOG_TAG = PhotoPicker.class.getSimpleName();

    private static final int REQUEST_CODE_PERMISSIONS = 404;
    static final int REQUEST_CODE_PICK_IMAGE = 405;

    Fragment fragment;
    private OnResultListener mOnResultListener;
    private String fileName;
    private String folderName;
    String authority;
    private boolean cached = false;
    private boolean cropped = true;
    private boolean timestamped = false;

    @ColorRes
    private Integer colorRes = R.color.colorPrimary;
    private UCrop.Options options;

    // Uri we'll be cropping from.
    Uri cropUri;



    /**
     * Creates a new instance.
     * @param fragment the fragment handling this instance and handling the photo picking.
     */
    PhotoPicker(Fragment fragment) {
        this.fragment = fragment;
    }

    /**
     * Starts the photo-picking process.
     */
    void start() {
        Log.d(LOG_TAG, "Starting photo picker");
        if (hasPermissions()) {
            tryLoadPhoto();
        }
    }

    private void tryLoadPhoto() {
        try {
            loadPhoto();
        } catch (IOException e) {
            e.printStackTrace();
            if (mOnResultListener != null) {
                mOnResultListener.onImageError(e);
            }
        }
    }

    private boolean hasPermissions() {
        Context context = fragment.getContext();
        boolean hasPermissions = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (!hasPermissions) {
            fragment.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSIONS);
        }
        return hasPermissions;
    }

    abstract void loadPhoto() throws IOException;

    /**
     * Call this to allow the picker handle permissions results from your
     * {@link Fragment#onRequestPermissionsResult(int, String[], int[])} callback.
     * @param requestCode the request code from the callback.
     * @param permissions the array of permissions requested.
     * @param grantResults the results of the request.
     * @return <code>true</code> if the request was made by this photo-picker.
     */
    public boolean handlePermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode != REQUEST_CODE_PERMISSIONS) return false;

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            tryLoadPhoto();
        } else if (mOnResultListener != null) {
            mOnResultListener.onPermissionDenied();
        }
        return true;
    }

    /**
     * Call this to allow the picker consume an activity result. The picker would check the request
     * if it was sent by it, and return <code>false</code> if not.
     * @param requestCode the request code from the callback
     * @param resultCode the result of the operation
     * @param data the accompanying data
     * @return <code>true</code> if the picker handled this request.
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "Handling activity result, code: " + resultCode);
        if (requestCode != REQUEST_CODE_PICK_IMAGE && requestCode != REQUEST_CROP) return false;
        if (resultCode != Activity.RESULT_OK) {
            Log.d(LOG_TAG, "Error from activity");
            if (mOnResultListener != null) {
                mOnResultListener.onImageError(new Exception("The image couldn't be loaded"));
            }
            return true;
        }

        switch (requestCode) {
            case REQUEST_CODE_PICK_IMAGE:
                if (data != null && data.getData() != null) {
                    Log.d(LOG_TAG, "Uri: " + data.getData());
                    Log.d(LOG_TAG, "Crop uri: " + cropUri);
                    cropUri = data.getData();
                } else {
                    Log.d(LOG_TAG, "No uri");
                }

                if (cropped) {
                    try {
                        startCrop();
                    } catch (Exception e) {
                        e.printStackTrace();
                        mOnResultListener.onImageError(new Exception("An error occurred while cropping your image."));
                    }
                } else {
                    mOnResultListener.onImagePicked(cropUri);
                }
                break;
            case REQUEST_CROP:
                Log.d(LOG_TAG, "Request crop result: " + data);
                if (data != null) {
                    onPhotoCropped(UCrop.getOutput(data));
                } else if (mOnResultListener != null) {
                    mOnResultListener.onImageError(new Exception("An error occurred while cropping your image."));
                }
                break;
        }
        return true;
    }

    private Uri createImageFile() {
        File fileDir = cached ? fragment.getContext().getExternalCacheDir() : fragment.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String finalFolderName = folderName == null || folderName.trim().isEmpty() ? "" : folderName;
        String imagePath = fileDir + File.separator + finalFolderName;
        File path = new File(imagePath);
        if (!path.exists()) path.mkdir();

        String name = fileName == null || fileName.trim().isEmpty() ? "" : fileName;
        if (this.timestamped || name.isEmpty()) {
            if (!name.isEmpty()) name += "_";
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            name += timeStamp;
        }
        name += ".jpg";

        File photo = new File(path, name);
        return Uri.fromFile(photo);
    }

    private void startCrop() throws Exception {
        Uri outputUri = createImageFile();

        UCrop.Options options = this.options;

        if (options == null) {
            options = new UCrop.Options();
            int color = fragment.getResources().getColor(this.colorRes);
            options.setToolbarColor(color);
            options.setStatusBarColor(color);
            options.setActiveControlsWidgetColor(color);
            options.setFreeStyleCropEnabled(true);
        }

        UCrop uCrop = UCrop.of(cropUri, outputUri)
                .useSourceImageAspectRatio()
                .withOptions(options);
        uCrop.start(fragment.getActivity(), fragment);
    }

    private void onPhotoCropped(Uri uri) {
        Log.d(LOG_TAG, "Image cropped: "+uri.toString());
        if (mOnResultListener != null) {
            mOnResultListener.onImagePicked(uri);
        }
    }



    // ------------ CHILDREN ---------------- //

    /**
     * Builder class which builds a photo picker. This contains
     * the config required to sucessfully pick a photo.
     */
    public static class Builder {

        private String fileName;
        private String folderName;
        private String authority;
        private boolean cached = false;
        private boolean timestamped = false;
        private boolean cropped = true;
        private Source source = Source.GALLERY;
        private OnResultListener onResultListener;
        private Integer colorRes;
        private UCrop.Options uCropOptions;

        /**
         * Sets the name of the photo file
         */
        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * Sets the name of the folder in which to save the photo file
         */
        public Builder folderName(String folderName) {
            this.folderName = folderName;
            return this;
        }

        /**
         * Sets the {@link Source} of the photo.
         *
         * Options include {@link Source#CAMERA} or {@link Source#GALLERY}
         */
        public Builder source(Source source) {
            this.source = source;
            return this;
        }

        /**
         * Sets the {@link androidx.core.content.FileProvider} authority used in sharing files
         * with external apps.
         */
        public Builder authority(String authority) {
            this.authority = authority;
            return this;
        }

        /**
         * Enables/disables making use of the apps cache folder for saving output images.
         */
        public Builder cached(boolean cached) {
            this.cached = cached;
            return this;
        }

        /**
         * Enables/disable cropping of the selected image.
         */
        public Builder cropped(boolean cropped) {
            this.cropped = cropped;
            return this;
        }

        /**
         * Enables/disables adding a timestamp to the name of the photo.
         */
        public Builder timestamped(boolean timestamped) {
            this.timestamped = timestamped;
            return this;
        }

        /**
         * Sets the colorRes used to theme the cropping tool.
         */
        public Builder color(@ColorRes Integer colorRes) {
            this.colorRes = colorRes;
            return this;
        }

        /**
         * Sets a custom set of {@link UCrop.Options} for the cropping tool.
         */
        public Builder uCropOptions(UCrop.Options options) {
            this.uCropOptions = options;
            return this;
        }

        /**
         * Sets the {@link OnResultListener} which would receive callbacks on the photo
         * picking progress.
         *
         * @see OnResultListener
         */
        public Builder setOnResultListener(OnResultListener onResultListener) {
            this.onResultListener = onResultListener;
            return this;
        }

        /**
         * Starts the picker.
         * @param fragment a {@link Fragment} which would overseer the pickers operations.
         * @return the {@link PhotoPicker} instance
         */
        public PhotoPicker start(Fragment fragment) {
            PhotoPicker picker = source == Source.GALLERY ? new GalleryPhotoPicker(fragment) :
                    new CameraPhotoPicker(fragment);
            picker.authority = authority;
            picker.cached = cached;
            picker.cropped = cropped;
            picker.fileName = fileName;
            picker.folderName = folderName;
            picker.mOnResultListener = onResultListener;
            picker.colorRes = colorRes;
            picker.timestamped = timestamped;
            picker.options = uCropOptions;

            picker.start();
            return picker;
        }

    }

    /**
     * Interface for receiving callbacks from the picker.
     */
    public interface OnResultListener {

        /**
         * Called when the user denies access to the gallery or
         * camera.
         */
        void onPermissionDenied();

        /**
         * Called when a photo has been successfully selected & cropped.
         * @param photoUri the {@link Uri} where you can access the photo.
         */
        void onImagePicked(Uri photoUri);

        /**
         * Called when an error occurs while selecting a photo.
         * @param e the exception which occurred while selecting a photo.
         */
        void onImageError(Exception e);

    }

    /**
     * Enum representation of each image source
     */
    public enum Source {
        CAMERA, GALLERY
    }



}
