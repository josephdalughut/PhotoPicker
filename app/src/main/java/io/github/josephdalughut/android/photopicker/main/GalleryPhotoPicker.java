package io.github.josephdalughut.android.photopicker.main;

import android.content.Intent;
import android.provider.MediaStore;

import androidx.fragment.app.Fragment;

import java.io.IOException;

/**
 * Created by Joseph Dalughut on 2019-09-10
 * Copyright © 2019
 *
 * This is a {@link PhotoPicker} which selects images from the users gallery.
 */
public class GalleryPhotoPicker extends PhotoPicker {

    public GalleryPhotoPicker(Fragment fragment) {
        super(fragment);
    }

    @Override
    void loadPhoto() throws IOException {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        fragment.startActivityForResult(Intent.createChooser(intent, "Pick Photo"),
                REQUEST_CODE_PICK_IMAGE);
    }
}
