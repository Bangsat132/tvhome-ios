package io.github.bangsat132.tvhome;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * The weather card's data: Open-Meteo, no account, no key, one request per refresh.
 *
 * <p>Two endpoints, both free and keyless: the geocoding search for a city name, and the forecast for a
 * latitude and longitude. The card asks for the current conditions and three days, in °C, and the caller
 * decides how to show them; nothing here touches the UI, so it can be tested on a plain JVM.
 *
 * <p>Snow, not rain, near freezing: the models type precipitation by air temperature and call snow "light
 * rain" at 1 °C. Like the reference weather add-on, this reads the WMO code and the temperature together and
 * says snow when it is at or below 1 °C, which is what a person sees out of the window.
 */
public final class Weather {

    /** One reading. `stale` is set when the fetch failed and the caller kept an older number. */
    public static final class Now {
        public final String city;
        public final double temperature;
        public final double feelsLike;
        public final String condition;
        public final int wmoCode;
        public final int isDay;
        public final double[] dayHighs = new double[3];
        public final double[] dayLows = new double[3];
        public final int[] dayCodes = new int[3];
        public final long fetchedAt;
        public final boolean stale;

        Now(String city, double temperature, double feelsLike, String condition, int wmoCode, int isDay,
            double[] highs, double[] lows, int[] codes, long fetchedAt, boolean stale) {
            this.city = city;
            this.temperature = temperature;
            this.feelsLike = feelsLike;
            this.condition = condition;
            this.wmoCode = wmoCode;
            this.isDay = isDay;
            System.arraycopy(highs, 0, dayHighs, 0, 3);
            System.arraycopy(lows, 0, dayLows, 0, 3);
            System.arraycopy(codes, 0, dayCodes, 0, 3);
            this.fetchedAt = fetchedAt;
            this.stale = stale;
        }
    }

    /** A place the city search matched. */
    public static final class Place {
        public final String name;
        public final String country;
        public final double latitude;
        public final double longitude;

        Place(String name, String country, double latitude, double longitude) {
            this.name = name;
            this.country = country;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        public String toString() {
            return country.isEmpty() ? name : name + ", " + country;
        }
    }

    private Weather() {}

    // --- HTTP -----------------------------------------------------------------

    private static String get(String url, int timeoutMs) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(timeoutMs);
        c.setReadTimeout(timeoutMs);
        c.setRequestProperty("User-Agent", "tvhome-ios/1.0 (Android TV; one request per 30 min)");
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new IllegalStateException("HTTP " + code);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
                char[] buf = new char[4096];
                int n;
                while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
            }
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }

    // --- city search ----------------------------------------------------------

    public static java.util.List<Place> search(String query, int count) throws Exception {
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + enc(query)
                + "&count=" + count + "&language=en&format=json";
        JSONObject root = new JSONObject(get(url, 8000));
        java.util.List<Place> places = new java.util.ArrayList<>();
        org.json.JSONArray results = root.optJSONArray("results");
        if (results == null) return places;
        for (int i = 0; i < results.length(); i++) {
            JSONObject r = results.getJSONObject(i);
            places.add(new Place(
                    r.optString("name", query),
                    r.optString("country_code", ""),
                    r.optDouble("latitude", Double.NaN),
                    r.optDouble("longitude", Double.NaN)));
        }
        return places;
    }

    // --- forecast -------------------------------------------------------------

    public static Now fetch(String city, double lat, double lon) throws Exception {
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                        + "&current=temperature_2m,apparent_temperature,weather_code,is_day"
                        + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                        + "&forecast_days=4&timezone=auto", lat, lon);
        JSONObject root = new JSONObject(get(url, 8000));

        JSONObject current = root.optJSONObject("current");
        if (current == null) throw new IllegalStateException("no current block");
        double temp = current.optDouble("temperature_2m", Double.NaN);
        double feels = current.optDouble("apparent_temperature", temp);
        int wmo = current.optInt("weather_code", 0);
        int isDay = current.optInt("is_day", 1);

        double[] highs = new double[]{Double.NaN, Double.NaN, Double.NaN};
        double[] lows = new double[]{Double.NaN, Double.NaN, Double.NaN};
        int[] codes = new int[]{0, 0, 0};
        JSONObject daily = root.optJSONObject("daily");
        if (daily != null) {
            org.json.JSONArray max = daily.optJSONArray("temperature_2m_max");
            org.json.JSONArray min = daily.optJSONArray("temperature_2m_min");
            org.json.JSONArray dc = daily.optJSONArray("weather_code");
            // Index 0 is today; the card shows the next three days, so skip it.
            for (int i = 0; i < 3; i++) {
                if (max != null && max.length() > i + 1) highs[i] = max.optDouble(i + 1, Double.NaN);
                if (min != null && min.length() > i + 1) lows[i] = min.optDouble(i + 1, Double.NaN);
                if (dc != null && dc.length() > i + 1) codes[i] = dc.optInt(i + 1, 0);
            }
        }
        return new Now(city, temp, feels, conditionFor(wmo, temp), wmo, isDay,
                highs, lows, codes, System.currentTimeMillis() / 1000L, false);
    }

    // --- WMO codes ------------------------------------------------------------

    /**
     * Plain English for a WMO weather code. At or below 1 °C a rain code reads as snow, because that is
     * what falls: the models type precipitation by air temperature alone.
     */
    public static String conditionFor(int code, double temperatureC) {
        boolean snowing = temperatureC <= 1.0;
        switch (code) {
            case 0: return "Clear";
            case 1: return "Mostly clear";
            case 2: return "Partly cloudy";
            case 3: return "Overcast";
            case 45: case 48: return "Fog";
            case 51: case 53: case 55: return snowing ? "Light snow" : "Drizzle";
            case 56: case 57: return "Freezing drizzle";
            case 61: return snowing ? "Light snow" : "Light rain";
            case 63: return snowing ? "Snow" : "Rain";
            case 65: return snowing ? "Heavy snow" : "Heavy rain";
            case 66: case 67: return "Freezing rain";
            case 71: return "Light snow";
            case 73: return "Snow";
            case 75: return "Heavy snow";
            case 77: return "Snow grains";
            case 80: return snowing ? "Snow showers" : "Light showers";
            case 81: return snowing ? "Snow showers" : "Showers";
            case 82: return snowing ? "Heavy snow showers" : "Heavy showers";
            case 85: case 86: return "Snow showers";
            case 95: return "Thunderstorm";
            case 96: case 99: return "Thunderstorm with hail";
            default: return "—";
        }
    }

    /** A one-glyph cue for the card, kept text so no bitmap is bundled. */
    public static String glyphFor(int code) {
        if (code == 0 || code == 1) return "☀";
        if (code == 2) return "⛅";
        if (code == 3) return "☁";
        if (code == 45 || code == 48) return "≡";
        if (code >= 51 && code <= 67) return "☂";
        if (code >= 71 && code <= 77) return "❄";
        if (code >= 80 && code <= 82) return "☔";
        if (code >= 85 && code <= 86) return "❄";
        if (code >= 95) return "⚡";
        return "•";
    }

    private static String enc(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
