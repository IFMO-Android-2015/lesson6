package ru.ifmo.android_2015.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import ru.ifmo.android_2015.json.CityJsonParser;
import ru.ifmo.android_2015.json.CityJsonReaderParser;

/**
 * Created by dmitry.trunin on 14.11.2015.
 */
public class CityFileImporter_JsonReader extends CityFileImporter {

    public CityFileImporter_JsonReader(SQLiteStatement insertStatement) {
        super(insertStatement);
    }

    @Override
    protected CityJsonParser createParser() {
        return new CityJsonReaderParser();
    }
}
