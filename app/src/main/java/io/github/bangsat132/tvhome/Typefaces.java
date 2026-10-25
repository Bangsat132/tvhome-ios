package io.github.bangsat132.tvhome;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

/**
 * Outfit, from res/font (OFL: the same variable font the reference launcher bundles).
 *
 * <p>DESIGN.md, "Typography" needs four weights out of one variable font: thin 200 for the clock and the
 * temperature, light 300 for the shelf title, regular 400 for body text, medium 500 for labels and pills. The
 * font XML files under res/font declare those weights for a TextView, but a Paint drawn on a Canvas needs the
 * weight on the typeface itself, so every weight comes from {@link #weighted}.
 *
 * <p>On Android 8 (API 26, this launcher's floor) there is no adjustable weight: every call returns the
 * default instance and size, colour and the focus state carry the hierarchy. Setting the variation per view
 * cost about 10 ms each on a TV CPU in the reference launcher, so each weight is built once and cached.
 */
public final class Typefaces {

    private static final Map<Integer, Typeface> CACHE = new HashMap<>();
    private static Typeface base;

    private Typefaces() {}

    public static Typeface thin(Context context) {
        return weighted(context, 200);
    }

    public static Typeface light(Context context) {
        return weighted(context, 300);
    }

    public static Typeface regular(Context context) {
        return weighted(context, 400);
    }

    public static Typeface medium(Context context) {
        return weighted(context, 500);
    }

    public static Typeface weighted(Context context, int weight) {
        Typeface cached = CACHE.get(weight);
        if (cached != null) return cached;
        Typeface typeface = base(context);
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            try {
                typeface = Typeface.create(typeface, weight, false);
            } catch (Exception ignored) {
                // A TV that refuses the weight still gets the family, which is what matters at 2 m.
            }
        }
        CACHE.put(weight, typeface);
        return typeface;
    }

    /** Applies a weight to a Paint that draws on a Canvas; TextView callers set the typeface instead. */
    public static void apply(Paint paint, Context context, int weight) {
        paint.setTypeface(weighted(context, weight));
    }

    private static Typeface base(Context context) {
        if (base != null) return base;
        try {
            base = context.getResources().getFont(R.font.outfit);
        } catch (Exception e) {
            base = Typeface.create("sans-serif", Typeface.NORMAL);
        }
        return base;
    }
}
