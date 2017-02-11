package ru.ifmo.android_2015.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import ru.ifmo.android_2015.db.util.DbExecutor;
import ru.ifmo.android_2015.json.CityJsonParser;
import ru.ifmo.android_2015.json.CityParserCallback;
import ru.ifmo.android_2015.model.City;
import ru.ifmo.android_2015.model.Point;
import ru.ifmo.android_2015.util.ObservableInputStream;
import ru.ifmo.android_2015.util.ProgressCallback;

public abstract class CityFileImporter implements CityParserCallback {

    public CityFileImporter(SQLiteDatabase db) {
        DbExecutor.getInstance().setDb(db);
    }

    public final synchronized void importCities(File srcFile, ProgressCallback progressCallback)
            throws IOException {

        InputStream in = null;
        try {
            long fileSize = srcFile.length();
            in = new FileInputStream(srcFile);
            in = new BufferedInputStream(in);
            in = new ObservableInputStream(in, fileSize, progressCallback);
            in = new GZIPInputStream(in);

            CityJsonParser parser = createParser();
            try {
                parser.parseCities(in, this);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to parse cities: " + e, e);
            }

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Failed to close file: " + e, e);
                }
            }
        }
    }

    protected abstract CityJsonParser createParser();


    @Override
    public void onCityParsed(final City city) {
        DbExecutor.getInstance().insertCity(city);
    }

    @Override
    public void onParsingBegins() {
        DbExecutor.getInstance().start();
    }

    @Override
    public void onParsingEnds() {
        Log.e(LOG_TAG, "Parsing finished");
        DbExecutor.getInstance().setParsingFinished(true);
    }

    private static final String LOG_TAG = "CityReader";

}
