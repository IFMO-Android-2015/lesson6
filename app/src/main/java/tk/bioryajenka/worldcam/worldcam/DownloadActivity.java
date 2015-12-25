package tk.bioryajenka.worldcam.worldcam;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import tk.bioryajenka.worldcam.util.DownloadUtils;
import tk.bioryajenka.worldcam.util.FileUtils;
import tk.bioryajenka.worldcam.util.ProgressCallback;

/**
 * Экран, выполняющий инициализацию при первом запуске приложения. В процессе инициализации
 * скачивается файл с данными, нужными для работы приложения. Пока идет инициализация, показывается
 * сплэш-скрин с индикатором прогресса.
 */
public class DownloadActivity extends ProgressTaskActivity {

    // Урл для скачивания файла с данными, нужными для инициализации приложения при первом запуске.
    // GZIP-архив, содержащий список городов в формате JSON.
    private static final String CITIES_GZ_URL =
            "https://www.dropbox.com/s/d99ky6aac6upc73/city_array.json.gz?dl=1";

    @Override
    protected ProgressTask createTask() {
        return new DownloadTask(this);
    }

    static class DownloadTask extends ProgressTask {

        DownloadTask(ProgressTaskActivity activity) {
            super(activity);
        }

        @Override
        protected void runTask() throws IOException {
            File file = downloadFile(appContext, this);
            new WorldcamPreferences(appContext).saveCitiesFileName(file.getName());
            Log.d(LOG_TAG, "Downloaded file: " + file);
        }
    }

    /**
     * Скачивает список городов во временный файл.
     */
    static File downloadFile(Context context,
                             ProgressCallback progressCallback) throws IOException {
        File destFile = FileUtils.createExternalFile(context, "cities_json", "gz");
        DownloadUtils.downloadFile(CITIES_GZ_URL, destFile, progressCallback);
        return destFile;
    }

}
