package ru.ifmo.android_2015.json;

import ru.ifmo.android_2015.model.City;

/**
 * Created by dmitry.trunin on 16.11.2015.
 */
public interface CityParserCallback {

    void onParsingBegins();
    void onParsingEnds();
    void onCityParsed(final City city);
}
