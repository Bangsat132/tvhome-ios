package io.github.bangsat132.tvhome;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;

/**
 * Small drawable helpers, so a glass fill or a focus ring is one call and its radius comes from the token
 * knob. Kept beside {@link Tokens} so a colour never repeats a literal outside this package.
 */
final class Drawables {

    private Drawables() {}

    static GradientDrawable glass(Context context, int fillColor, int radiusPx, int rimPx, int rimColor) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(fillColor);
        d.setCornerRadius(radiusPx);
        if (rimPx > 0) d.setStroke(rimPx, rimColor);
        return d;
    }

    static GradientDrawable ring(Context context, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(0x00000000);
        d.setCornerRadius(Tokens.dp(context, radiusDp));
        d.setStroke(Math.round(Tokens.dp(context, 2)), Tokens.RING);
        return d;
    }
}
