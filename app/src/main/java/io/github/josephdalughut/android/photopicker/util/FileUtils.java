package io.github.josephdalughut.android.photopicker.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Joseph Dalughut on 2019-09-10
 * Copyright © 2019
 *
 * Utility class for handling files.
 */
public class FileUtils {

    /**
     * Creates a new {@link File}
     * @param context
     * @param cached true if the file should be saved in the cached directory.
     * @param name the name of the file. If this isn't provided, a random UUID is used.
     * @param folderName the name of a folder to save the file. If this isn't provided, it is ignored
     *                   and saved in the root directory.
     * @return a {@link File}
     * @throws IOException if an error occurs while creating the file.
     */
    public static File createFile(Context context, boolean cached, @Nullable String name, @Nullable String folderName) throws IOException {
        String _name = name == null || name.trim().isEmpty() ?
                UUID.randomUUID().toString() : name;
        String folder = folderName == null || folderName.trim().isEmpty() ? "" : folderName + File.separator;
        String fileName = folder + _name;

        File storageDir = cached ? context.getCacheDir() : context.getFilesDir();
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    /**
     * Creates a temporary {@link File} in the external files directory. This is good for
     * operations where you need an externally readable file real-quick, for example
     * camera capture
     * @param context
     * @return a {@link Uri} which can be accessed externally by other apps.
     * @throws IOException if an error occurred when creating the file.
     */
    public static Uri createTemporaryExternalUri(Context context, String authority) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = File.createTempFile(
                fileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return FileProvider.getUriForFile(context, authority, file);
    }

}
