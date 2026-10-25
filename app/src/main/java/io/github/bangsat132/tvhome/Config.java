package io.github.bangsat132.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The launcher's settings: defaults from DESIGN.md, overridable by a theme file the other tools can push.
 *
 * <p>The reference launcher reads a `config.json` overlay from its own app storage, so a theme can change the
 * dock without a rebuild. The same idea here, trimmed to the keys this launcher actually has. A file that
 * does not parse is ignored as a whole: a broken theme must never leave Home unusable.
 */
public final class Config {

    private static final String PREFS = "home";
    private static final String KEY_THEME = "theme_json";

    /** Apps in the dock, in order. Missing packages are skipped. */
    public final List<String> dockApps = new ArrayList<>();
    /** Tiles per row across both the dock and the grid. */
    public int columns = 5;
    /** Apps never shown as tiles. */
    public final List<String> hiddenApps = new ArrayList<>();
    /** City for the weather card, chosen in the card's picker; empty until then. */
    public String city = "";
    public double latitude = Double.NaN;
    public double longitude = Double.NaN;
    /** Celsius unless a theme says otherwise. */
    public boolean fahrenheit = false;
    /** Reduced motion: snap instead of spring, and no focus scale. */
    public boolean reducedMotion = false;
    /** Draw the wallpaper behind Home; empty means the built-in gradient. */
    public String wallpaper = "";

    public static Config load(Context context) {
        Config config = new Config();
        // The dock defaults to the apps a TV in this family actually carries: Netflix, the sideloaded
        // TizenTube, and the set's own Live TV. Prime Video and Hotstar are not installed on the POLYTRON 2K,
        // and a dock slot for an app that is not there is a hole (fill_dock fills it, so the order still reads).
        config.dockApps.add("com.netflix.ninja");
        config.dockApps.add("io.gh.reisxd.tizentube.cobalt");
        config.dockApps.add("com.mediatek.wwtv.tvcenter");
        config.dockApps.add("com.android.vending");

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        config.city = prefs.getString("city", "");
        config.fahrenheit = prefs.getBoolean("fahrenheit", false);
        double lat = Double.longBitsToDouble(prefs.getLong("lat", Double.doubleToLongBits(Double.NaN)));
        double lon = Double.longBitsToDouble(prefs.getLong("lon", Double.doubleToLongBits(Double.NaN)));
        config.latitude = lat;
        config.longitude = lon;

        String json = prefs.getString(KEY_THEME, null);
        if (json != null) {
            config.apply(json);
        }
        return config;
    }

    /** Apply a theme's JSON over the current values. Unknown keys and wrong types are ignored. */
    public void apply(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray dock = root.optJSONArray("dock_apps");
            if (dock != null) {
                dockApps.clear();
                for (int i = 0; i < dock.length(); i++) {
                    String pkg = dock.optString(i, "");
                    if (!pkg.isEmpty()) dockApps.add(pkg);
                }
            }
            JSONArray hidden = root.optJSONArray("hidden_apps");
            if (hidden != null) {
                hiddenApps.clear();
                for (int i = 0; i < hidden.length(); i++) {
                    String pkg = hidden.optString(i, "");
                    if (!pkg.isEmpty()) hiddenApps.add(pkg);
                }
            }
            columns = clampColumns(root.optInt("columns", columns));
            fahrenheit = root.optBoolean("fahrenheit", fahrenheit);
            reducedMotion = root.optBoolean("reduced_motion", reducedMotion);
            wallpaper = root.optString("wallpaper", wallpaper);
        } catch (Exception ignored) {
            // A theme that does not parse changes nothing.
        }
    }

    /** Five columns is the reference look; the range keeps a tile wider than its own label. */
    public static int clampColumns(int value) {
        return Math.max(3, Math.min(8, value));
    }

    public void save(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("city", city)
                .putBoolean("fahrenheit", fahrenheit)
                .putLong("lat", Double.doubleToLongBits(latitude))
                .putLong("lon", Double.doubleToLongBits(longitude))
                .apply();
    }
}
