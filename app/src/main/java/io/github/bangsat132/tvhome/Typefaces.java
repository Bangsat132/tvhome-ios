package io.github.bangsat132.tvhome;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

/** Outfit, from res/font (OFL, already bundled in the reference launcher). One family, three weights. */
public final class Typefaces {

    private static final Map<String, Typeface> CACHE = new HashMap<>();

    private Typefaces() {}

    public static Typeface thin(Context context) {
        return load(context, R.font.outfit_extralight, "thin");
    }

    public static Typeface light(Context context) {
        return load(context, R.font.outfit_light, "light");
    }

    public static Typeface regular(Context context) {
        return load(context, R.font.outfit_regular, "regular");
    }

    public static Typeface medium(Context context) {
        return load(context, R.font.outfit_medium, "medium");
    }

    private static Typeface load(Context context, int resId, String key) {
        Typeface cached = CACHE.get(key);
        if (cached != null) return cached;
        Typeface typeface;
        try {
            typeface = context.getResources().getFont(resId);
        } catch (Exception e) {
            typeface = Typeface.create("sans-serif", Typeface.NORMAL);
        }
        CACHE.put(key, typeface);
        return typeface;
    }
}
