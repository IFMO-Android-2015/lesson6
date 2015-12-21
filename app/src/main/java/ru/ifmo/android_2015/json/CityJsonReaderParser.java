package ru.ifmo.android_2015.json;

import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import ru.ifmo.android_2015.model.City;

/**
 * Created by dmitry.trunin on 16.11.2015.
 */
public class CityJsonReaderParser implements CityJsonParser {

    @Override
    public void parseCities(InputStream in, @Nullable CityParserCallback callback)
            throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in));
        reader.beginArray();

        if (callback != null) {
            callback.onParsingBegins();
        }

        while (reader.hasNext()) {
            parseCity(reader, callback);
        }

        if (callback != null) {
            callback.onParsingEnds();
        }

        reader.endArray();


    }

    private void parseCity(JsonReader reader, CityParserCallback callback) throws IOException {
        reader.beginObject();

        City city = new City();
        while (reader.hasNext()) {
            final String name = reader.nextName();
            if (name == null) {
                // Вообще, такого быть не должно, но ВСЕГДА надо проверять на null,
                // перед тем как делать switch по строке -- иначе NPE
                reader.skipValue();
                continue;
            }
            switch (name) {
                case "_id":
                    city.setId(reader.nextLong());
                    break;
                case "name":
                    city.setName(reader.nextString());
                    break;
                case "country":
                    city.setCountry(reader.nextString());
                    break;
                case "coord":
                    parseCoord(reader, city);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();

        /*if (id == Long.MIN_VALUE || cityName == null || country == null || latLon == null) {
            Log.w(LOG_TAG, "Incomplete city data: id=" + id + " cityName=" + cityName
                    + " country=" + country + " latLon=" + Arrays.toString(latLon));

        } else if (callback != null) {*/
            callback.onCityParsed(city);
        /*}*/
    }

    private void parseCoord(JsonReader reader, City city) throws IOException {
        reader.beginObject();

        while (reader.hasNext()) {
            final String name = reader.nextName();
            if (name == null) {
                reader.skipValue();
                continue;
            }
            switch (name) {
                case "lat":
                    city.getCoord().setLat(reader.nextDouble());
                    break;
                case "lon":
                    city.getCoord().setLon(reader.nextDouble());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
    }

    private static final String LOG_TAG = "CityParser";
}
