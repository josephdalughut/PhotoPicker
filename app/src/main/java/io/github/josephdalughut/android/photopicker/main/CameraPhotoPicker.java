package io.github.josephdalughut.android.photopicker.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.fragment.app.Fragment;

import java.io.IOException;

import io.github.josephdalughut.android.photopicker.util.FileUtils;

/**
 * Created by Joseph Dalughut on 2019-09-10
 * Copyright © 2019
 *
 * This is a {@link PhotoPicker} which gets its photos from the camera.
 */
public class CameraPhotoPicker extends PhotoPicker {

    public CameraPhotoPicker(Fragment fragment) {
        super(fragment);
    }

    @SuppressLint("QueryPermissionsNeeded")
    @Override
    void loadPhoto() throws IOException {
        if (authority == null || authority.isEmpty())
            throw new IOException("Please provide a file-provider authority.");

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(fragment.getContext().getPackageManager()) == null)
            throw new IOException("Unable to open camera");

        cropUri = FileUtils.createTemporaryExternalUri(fragment.getContext(), authority);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cropUri);
        fragment.startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

}
