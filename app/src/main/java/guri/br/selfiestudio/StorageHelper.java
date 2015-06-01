package guri.br.selfiestudio;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Created by Claudivan on 28/05/2015.
 */
public class StorageHelper {

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public static File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                +File.separator
                + albumName
                + File.separator);
        if (!file.mkdirs()) {
            Log.e("DIRECTORYYY", "Directory not created");
        }
        return file;
    }


}
