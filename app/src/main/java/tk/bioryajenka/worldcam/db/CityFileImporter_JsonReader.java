package tk.bioryajenka.worldcam.db;

import android.database.sqlite.SQLiteDatabase;

import tk.bioryajenka.worldcam.json.CityJsonParser;
import tk.bioryajenka.worldcam.json.CityJsonReaderParser;

/**
 * Created by dmitry.trunin on 14.11.2015.
 */
public class CityFileImporter_JsonReader extends CityFileImporter {

    public CityFileImporter_JsonReader(SQLiteDatabase db) {
        super(db);
    }

    @Override
    protected CityJsonParser createParser() {
        return new CityJsonReaderParser();
    }
}
