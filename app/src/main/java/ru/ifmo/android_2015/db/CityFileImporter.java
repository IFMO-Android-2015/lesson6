package ru.ifmo.android_2015.db;

import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import ru.ifmo.android_2015.json.CityJsonParser;
import ru.ifmo.android_2015.json.CityParserCallback;
import ru.ifmo.android_2015.util.ObservableInputStream;
import ru.ifmo.android_2015.util.ProgressCallback;

public abstract class CityFileImporter implements CityParserCallback {

    private SQLiteDatabase db;
    private int importedCount;

    private SQLiteStatement insertStatement;
    private static String QUERY =
            "INSERT INTO " + CityContract.Cities.TABLE
                    + "("
                    + CityContract.CityColumns.CITY_ID + ","
                    + CityContract.CityColumns.NAME + ","
                    + CityContract.CityColumns.COUNTRY + ","
                    + CityContract.CityColumns.LATITUDE + ","
                    + CityContract.CityColumns.LONGITUDE
                    + ")"
                    + "VALUES (?,?,?,?,?)";
    public CityFileImporter(SQLiteDatabase db) {
        this.db = db;
    }

    public final synchronized void importCities(File srcFile,
                                                ProgressCallback progressCallback)
            throws IOException {

        InputStream in = null;

        try {
            long fileSize = srcFile.length();
            in = new FileInputStream(srcFile);
            in = new BufferedInputStream(in);
            in = new ObservableInputStream(in, fileSize, progressCallback);
            in = new GZIPInputStream(in);
            importCities(in);

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

    private void importCities(InputStream in) {
        CityJsonParser parser = createParser();
        try {
            insertStatement = db.compileStatement(CityFileImporter.QUERY);
                try {
                    parser.parseCities(in, this);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Can't parse cities: " + e, e);
                }
        } finally {
            if (insertStatement != null) {
                try {
                    insertStatement.close();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Can't close: " + e, e);
                }
            }
        }
    }

    @Override
    public void onCityParsed(long id, String name, String country, double lat, double lon) {
        insertCity(db, id, name, country, lat, lon);
        importedCount++;
        if (importedCount % 1000 == 0) {
            Log.d(LOG_TAG, "Processed " + importedCount + " cities");
        }
    }

    private boolean insertCity(SQLiteDatabase db,
                               long id,
                               @NonNull String name,
                               @NonNull String country,
                               double latitude,
                               double longitude) {
        insertStatement.bindLong(1, id);
        insertStatement.bindString(2, name);
        insertStatement.bindString(3, country);
        insertStatement.bindDouble(4, latitude);
        insertStatement.bindDouble(5, longitude);
        long rowId =  insertStatement.executeInsert();
        if (rowId < 0) {
            Log.w(LOG_TAG, "Failed to insert city: id=" + id + " name=" + name);
            return false;
        }
        return true;
    }

    private static final String LOG_TAG = "CityReader";

}
