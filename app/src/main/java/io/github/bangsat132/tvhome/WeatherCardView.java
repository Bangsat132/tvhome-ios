package io.github.bangsat132.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * The weather card: the first tile of the row and the only live one (DESIGN.md, "Signature Element").
 *
 * <p>Every size here is a fraction of the card's own box, not a dp constant. The card is one grid column
 * wide, so on this 720p set it is about 220 x 110 px; a layout written in dp puts the same text in a box a
 * third smaller and the numbers fall past the bottom, which is exactly why the first build drew an empty slab
 * with its own label invisible.
 *
 * <p>States (DESIGN.md, "Components / widget-card"): loaded, loading, no-city, focused, and stale (a failed
 * fetch keeps the last good reading and marks it).
 */
public final class WeatherCardView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF card = new RectF();
    private final RectF sky = new RectF();
    private final RectF ringRect = new RectF();

    private Weather.Now now;
    private boolean loading;
    private boolean focused;
    private boolean fahrenheit;
    private float daylight = 0.55f;
    private float progress;

    public WeatherCardView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(false);
        setClickable(true);
        text.setTypeface(Typefaces.regular(context));
        text.setSubpixelText(true);
    }

    // --- state ---------------------------------------------------------------

    public void setFahrenheit(boolean value) {
        fahrenheit = value;
        invalidate();
    }

    public void setFocusedState(boolean value) {
        if (focused == value) return;
        focused = value;
        invalidate();
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float value) {
        progress = Math.max(0f, Math.min(1f, value));
        invalidate();
    }

    public void setLoading(boolean value) {
        loading = value;
        invalidate();
    }

    public Weather.Now getNow() {
        return now;
    }

    public void setNow(Weather.Now value) {
        now = value;
        loading = false;
        if (value != null) {
            // Clear weather lifts the field, overcast settles it: the one cue the card needs to feel alive
            // without shipping a picture.
            daylight = value.isDay == 1 ? Math.min(1f, 0.45f + clarity(value.wmoCode)) : 0.12f;
            View field = getRootView().findViewById(R.id.field);
            if (field instanceof FieldView) ((FieldView) field).setDaylight(daylight);
            double shown = fahrenheit ? value.temperature * 9.0 / 5.0 + 32.0 : value.temperature;
            setContentDescription(String.format(java.util.Locale.getDefault(), "%s, %d degrees, %s",
                    value.city, Math.round(shown), value.condition));
        }
        invalidate();
    }

    private static float clarity(int code) {
        if (code <= 1) return 0.55f;
        if (code == 2) return 0.40f;
        if (code == 3) return 0.25f;
        if (code >= 45 && code <= 48) return 0.15f;
        return 0.20f;
    }

    // --- drawing -------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float pad = w * 0.062f;
        float radius = Math.min(w, h) * 0.13f;
        float inner = w - pad * 2;
        card.set(0, 0, w, h);

        // 1. The slab: one step above the field, no shadow (DESIGN.md, "Elevation & Depth").
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(focused ? lighten(Tokens.SURFACE_RAISED, 0.10f) : Tokens.SURFACE_RAISED);
        canvas.drawRoundRect(card, radius, radius, paint);

        // 2. The sky band, graded from the sun position: amber at dawn, blue at noon, indigo at night.
        sky.set(pad, pad, w - pad, h * 0.50f);
        float skyRadius = Math.min(sky.width(), sky.height()) * 0.22f;
        paint.setShader(new LinearGradient(0, sky.top, 0, sky.bottom,
                new int[]{skyTop(), skyBottom()}, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(sky, skyRadius, skyRadius, paint);
        paint.setShader(null);

        // 3. The horizon: the card's one signature detail, a hairline at the sky's base.
        paint.setColor(0x40FFFFFF);
        paint.setStrokeWidth(Math.max(1f, h * 0.008f));
        canvas.drawLine(sky.left + pad * 0.4f, sky.bottom - h * 0.045f,
                sky.right - pad * 0.4f, sky.bottom - h * 0.045f, paint);

        float left = pad;

        if (now != null) {
            // 4a. The temperature, thin, at 30 % of the card's height, sitting on the horizon.
            String degrees = format(now.temperature);
            text.setColor(Tokens.TEXT_PRIMARY);
            text.setTypeface(Typefaces.thin(getContext()));
            text.setTextSize(h * 0.30f);
            float baseline = sky.bottom - h * 0.055f;
            canvas.drawText(degrees, left, baseline, text);
            float used = text.measureText(degrees);

            text.setTypeface(Typefaces.medium(getContext()));
            text.setTextSize(h * 0.085f);
            text.setColor(Tokens.TEXT_SECONDARY);
            canvas.drawText("°" + unitLetter(), left + used + w * 0.008f, baseline, text);

            // 4b. The condition under the sky, with its glyph as a text cue (no bitmap bundled).
            text.setTypeface(Typefaces.regular(getContext()));
            text.setTextSize(h * 0.105f);
            text.setColor(Tokens.TEXT_PRIMARY);
            String condition = Weather.glyphFor(now.wmoCode) + "  " + now.condition;
            canvas.drawText(fit(condition, text, inner), left, h * 0.72f, text);

            // 4c. The city at the base, small caps and dimmed; "STALE" instead when the last fetch failed.
            text.setTypeface(Typefaces.medium(getContext()));
            text.setTextSize(h * 0.078f);
            text.setColor(now.stale ? Tokens.DANGER : Tokens.TEXT_TERTIARY);
            String foot = now.stale ? "STALE" : now.city.toUpperCase(java.util.Locale.getDefault());
            canvas.drawText(fit(foot, text, inner * 0.55f), left, h * 0.90f, text);

            // 4d. The day highs, right-aligned at the base, only as many as fit.
            float right = w - pad;
            float dy = h * 0.90f;
            for (int i = 0; i < 3; i++) {
                if (Double.isNaN(now.dayHighs[i])) continue;
                String mark = format(now.dayHighs[i]) + "°";
                float width = text.measureText(mark);
                if (right - width < left + inner * 0.42f) break;
                text.setColor(i == 0 ? Tokens.TEXT_SECONDARY : Tokens.TEXT_TERTIARY);
                canvas.drawText(mark, right - width, dy, text);
                right -= width + w * 0.05f;
            }
        } else if (loading) {
            // 5a. Loading: a word, never a spinner (a spinner on a TV reads as a fault).
            text.setTypeface(Typefaces.regular(getContext()));
            text.setTextSize(h * 0.135f);
            text.setColor(Tokens.TEXT_SECONDARY);
            canvas.drawText("Weather", left, h * 0.44f, text);
        } else {
            // 5b. No city yet: the invitation. The first build was in this state and drew nothing, because
            // its layout pushed the text past the card's own box.
            text.setTypeface(Typefaces.regular(getContext()));
            text.setTextSize(h * 0.135f);
            text.setColor(Tokens.TEXT_PRIMARY);
            canvas.drawText("Weather", left, h * 0.42f, text);
            text.setTypeface(Typefaces.medium(getContext()));
            text.setTextSize(h * 0.082f);
            text.setColor(Tokens.SKY);
            canvas.drawText(fit("PRESS OK TO PICK A CITY", text, inner), left, h * 0.63f, text);
        }

        // 6. Loading line: a share of the card's height, along its base.
        if (loading) {
            float lineH = Math.max(2f, h * 0.02f);
            float lineY = h - pad * 0.6f;
            paint.setColor(0x26FFFFFF);
            canvas.drawRect(pad, lineY, w - pad, lineY + lineH, paint);
            float width = (w - pad * 2) * 0.35f;
            float start = pad + (w - pad * 2 - width) * progress;
            paint.setColor(Tokens.SKY);
            canvas.drawRect(start, lineY, start + width, lineY + lineH, paint);
        }

        // 7. The focus ring, its own token so it reads on the card's own blue.
        if (focused) {
            float stroke = Math.max(2f, h * 0.018f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Tokens.RING);
            ringRect.set(stroke / 2f, stroke / 2f, w - stroke / 2f, h - stroke / 2f);
            canvas.drawRoundRect(ringRect, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    /** Ellipsises a line to a width, so a long city name never runs into the day marks. */
    private static String fit(String value, Paint paint, float maxWidth) {
        if (paint.measureText(value) <= maxWidth) return value;
        String cut = value;
        while (cut.length() > 1 && paint.measureText(cut + "…") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    private String format(double value) {
        if (Double.isNaN(value)) return "--";
        double shown = fahrenheit ? value * 9.0 / 5.0 + 32.0 : value;
        return String.valueOf(Math.round(shown));
    }

    private String unitLetter() {
        return fahrenheit ? "F" : "C";
    }

    private int skyTop() {
        int night = 0xFF1B1F3B;
        int day = 0xFF2F6FB5;
        int warm = 0xFFB4622F;
        if (daylight < 0.25f) return blendColor(night, warm, daylight / 0.25f);
        if (daylight < 0.45f) return warm;
        return blendColor(warm, day, (daylight - 0.45f) / 0.55f);
    }

    private int skyBottom() {
        int night = 0xFF2A2F52;
        int day = 0xFF6FA8D6;
        int warm = 0xFFE0A063;
        if (daylight < 0.25f) return blendColor(night, warm, daylight / 0.25f);
        if (daylight < 0.45f) return warm;
        return blendColor(warm, day, (daylight - 0.45f) / 0.55f);
    }

    private static int blendColor(int from, int to, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        return Color.rgb(
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * k),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * k),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * k));
    }

    private static int lighten(int color, float amount) {
        return blendColor(color, Color.WHITE, amount);
    }
}
