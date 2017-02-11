package ru.ifmo.android_2015.json;

/**
 * Created by dmitry.trunin on 16.11.2015.
 */
public interface CityParserCallback {

    boolean onCityParsed(long id, String name, String country, double lat, double lon);
}
