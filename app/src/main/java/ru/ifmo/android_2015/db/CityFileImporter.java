package ru.ifmo.android_2015.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
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
    private SQLiteStatement statement;

    public CityFileImporter(SQLiteDatabase db) {
        this.db = db;
    }

    public final synchronized void importCities(
            File srcFile, ProgressCallback progressCallback) throws IOException {

        InputStream in = null;
        try {
            long fileSize = srcFile.length();
            in = new FileInputStream(srcFile);
            in = new BufferedInputStream(in);
            in = new ObservableInputStream(in, fileSize, progressCallback);
            in = new GZIPInputStream(in);

            CityJsonParser parser = createParser();
            try {
                db.execSQL("DROP TABLE IF EXISTS`" + CityContract.Cities.TABLE + "`");
                db.execSQL(CityContract.Cities.CREATE_TABLE);
                statement = db.compileStatement(CityContract.Cities.INSERT_INTO_TABLE);
                db.beginTransaction();
                parser.parseCities(in, this);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to parse cities: " + e, e);
            } finally {
                db.endTransaction();
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error while close SQLiteStatement: " + e.getMessage());
                    }
                }
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
    public void onCityParsed(long id, String name, String country, double lat, double lon) {
        statement.bindLong(1, id);
        statement.bindString(2, name);
        statement.bindString(3, country);
        statement.bindDouble(4, lat);
        statement.bindDouble(5, lon);

        long rowId = statement.executeInsert();
        if (rowId < 0) {
            Log.w(LOG_TAG, "Failed to insert city: id=" + id + " name=" + name);
        }

        importedCount++;
        if (importedCount % 1000 == 0) {
            Log.d(LOG_TAG, "Processed " + importedCount + " cities");
        }
    }

    private static final String LOG_TAG = "CityReader";

}
