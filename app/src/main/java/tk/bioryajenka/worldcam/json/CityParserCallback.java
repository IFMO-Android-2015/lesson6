package tk.bioryajenka.worldcam.json;

/**
 * Created by dmitry.trunin on 16.11.2015.
 */
public interface CityParserCallback {

    void onCityParsed(long id, String name, String country, double lat, double lon);
}
