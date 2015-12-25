package tk.bioryajenka.worldcam.worldcam;

import java.io.IOException;

import tk.bioryajenka.worldcam.db.CityDBHelper;
import tk.bioryajenka.worldcam.util.FileUtils;

/**
 * Экран, выполняющий очистку: удаляет все файлы, сохраненные настройки и БД
 */
public class CleanActivity extends ProgressTaskActivity {

    @Override
    protected ProgressTask createTask() {
        return new CleanTask(this);
    }

    static class CleanTask extends ProgressTask {

        CleanTask(ProgressTaskActivity activity) {
            super(activity);
        }

        @Override
        protected void runTask() throws IOException {
            new WorldcamPreferences(appContext).clear();
            FileUtils.cleanExternalFilesDir(appContext);
            CityDBHelper.getInstance(appContext).dropDb();
        }
    }
}
