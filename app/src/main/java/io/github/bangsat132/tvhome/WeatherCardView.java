package io.github.bangsat132.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.view.View;

/**
 * The weather card: the first tile of the row and the only live one (DESIGN.md, "Signature Element").
 *
 * <p>It is one tile wide so it lines up with the grid's columns in every theme, and it shows only what fits
 * at that width: the temperature, the city, the condition, and three small day marks. Tap or OK refreshes;
 * a failed fetch keeps the last good reading and marks it.
 *
 * <p>States come from DESIGN.md, "Components / widget-card": loading (sky gradient, no numbers, a 2dp
 * progress line), loaded (numbers), error (a danger dot and the last reading), focused (ring plus a glow in
 * the sky accent).
 */
public final class WeatherCardView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF card = new RectF();
    private final RectF sky = new RectF();

    private Weather.Now now;
    private boolean loading;
    private boolean focused;
    private boolean fahrenheit;
    private float daylight = 0.5f;
    /** 0..1, animated while loading so the progress line is visibly alive. */
    private float progress;

    public WeatherCardView(Context context) {
        super(context);
        setFocusable(false);
        setClickable(true);
        text.setTypeface(Typefaces.light(context));
    }

    public void setFahrenheit(boolean value) {
        fahrenheit = value;
        invalidate();
    }

    public void setFocusedState(boolean value) {
        if (focused == value) return;
        focused = value;
        invalidate();
    }

    public void setLoading(boolean value) {
        loading = value;
        invalidate();
    }

    public void setProgress(float value) {
        progress = Math.max(0f, Math.min(1f, value));
        invalidate();
    }

    public void setNow(Weather.Now value) {
        now = value;
        loading = false;
        if (value != null) {
            // Clear conditions show a brighter field than an overcast one, which is the only cue the card
            // needs to feel alive without a picture.
            daylight = value.isDay == 1 ? Math.min(1f, 0.45f + clarity(value.wmoCode)) : 0.12f;
            if (getParent() instanceof android.view.ViewGroup) {
                android.view.ViewGroup parent = (android.view.ViewGroup) getParent();
                android.view.View field = parent.findViewById(R.id.field);
                if (field instanceof FieldView) ((FieldView) field).setDaylight(daylight);
            }
        }
        invalidate();
    }

    public Weather.Now getNow() {
        return now;
    }

    private static float clarity(int code) {
        if (code <= 1) return 0.55f;
        if (code == 2) return 0.40f;
        if (code == 3) return 0.25f;
        if (code >= 45 && code <= 48) return 0.15f;
        return 0.20f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        float pad = Tokens.dp(getContext(), 16);
        float radius = Tokens.dp(getContext(), Tokens.RADIUS_TILE);
        card.set(0, 0, w, h);

        // 1. The card slab: raised surface, one step lighter than the field, so it reads as a card without a
        //    shadow (DESIGN.md, "Elevation & Depth": no shadows on this surface).
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(focused ? lighten(Tokens.SURFACE_RAISED, 0.06f) : Tokens.SURFACE_RAISED);
        canvas.drawRoundRect(card, radius, radius, paint);

        // 2. The sky: a vertical grade from the sun position. Dawn amber, day blue, dusk rose, night indigo.
        sky.set(pad, pad, w - pad, h * 0.52f);
        float skyRadius = Math.max(Tokens.dp(getContext(), Tokens.RADIUS_SM), radius - pad * 0.4f);
        paint.setShader(new LinearGradient(0, sky.top, 0, sky.bottom,
                new int[]{skyTop(), skyBottom()}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(sky, skyRadius, skyRadius, paint);
        paint.setShader(null);

        // 3. A thin horizon at the sky's base: the card's one signature detail.
        paint.setColor(0x33FFFFFF);
        paint.setStrokeWidth(Tokens.dp(getContext(), 1));
        canvas.drawLine(sky.left + pad, sky.bottom - Tokens.dp(getContext(), 10),
                sky.right - pad, sky.bottom - Tokens.dp(getContext(), 10), paint);

        float x = pad;
        float y = pad;

        // 4. The temperature, in the light weight so it reads as air rather than as a headline.
        if (now != null) {
            String temp = format(now.temperature) + "°";
            text.setColor(Tokens.TEXT_PRIMARY);
            text.setTextSize(Tokens.dp(getContext(), 44));
            text.setTypeface(Typefaces.thin(getContext()));
            canvas.drawText(temp, x, y + Tokens.dp(getContext(), 46), text);

            text.setTypeface(Typefaces.regular(getContext()));
            text.setTextSize(Tokens.dp(getContext(), Tokens.META));
            text.setColor(Tokens.TEXT_SECONDARY);
            canvas.drawText(short(temp), x + text.measureText(temp) + Tokens.dp(getContext(), 8),
                    y + Tokens.dp(getContext(), 44), text);

            float lineY = y + Tokens.dp(getContext(), 72);
            text.setColor(Tokens.TEXT_PRIMARY);
            text.setTextSize(Tokens.dp(getContext(), Tokens.BODY));
            canvas.drawText(Weather.glyphFor(now.wmoCode) + "  " + now.condition, x, lineY, text);

            text.setColor(Tokens.TEXT_SECONDARY);
            text.setTextSize(Tokens.dp(getContext(), Tokens.META));
            canvas.drawText(now.stale ? "Last reading" : "Now", x, lineY + Tokens.dp(getContext(), 18), text);
        } else if (loading) {
            text.setColor(Tokens.TEXT_SECONDARY);
            text.setTextSize(Tokens.dp(getContext(), Tokens.BODY));
            canvas.drawText("Weather…", x, y + Tokens.dp(getContext(), 46), text);
        } else {
            text.setColor(Tokens.TEXT_SECONDARY);
            text.setTextSize(Tokens.dp(getContext(), Tokens.BODY));
            canvas.drawText("Set a city", x, y + Tokens.dp(getContext(), 46), text);
            text.setTextSize(Tokens.dp(getContext(), Tokens.META));
            text.setColor(Tokens.TEXT_TERTIARY);
            canvas.drawText("Press OK", x, y + Tokens.dp(getContext(), 68), text);
        }

        // 5. The city, bottom left; the three day marks, bottom right. The card is one tile wide, so the
        //    full three-day strip lives in the weather app (DESIGN.md, accepted debt).
        if (now != null && !now.city.isEmpty()) {
            text.setTypeface(Typefaces.medium(getContext()));
            text.setTextSize(Tokens.dp(getContext(), Tokens.LABEL));
            text.setColor(Tokens.TEXT_TERTIARY);
            canvas.drawText(now.city.toUpperCase(java.util.Locale.ROOT), x, h - Tokens.dp(getContext(), 16), text);

            float dy = h - Tokens.dp(getContext(), 16);
            float right = w - pad;
            text.setTextSize(Tokens.dp(getContext(), Tokens.META));
            for (int i = 2; i >= 0; i--) {
                String mark = Weather.glyphFor(now.dayCodes[i]) + " " + format(now.dayHighs[i]) + "°";
                float width = text.measureText(mark);
                text.setColor(i == 0 ? Tokens.TEXT_SECONDARY : Tokens.TEXT_TERTIARY);
                canvas.drawText(mark, right - width, dy, text);
                right -= width + Tokens.dp(getContext(), 10);
                if (right < x + Tokens.dp(getContext(), 90)) break;
            }
        }

        // 6. Loading: a 2dp progress line at the card's base, never a spinner (a spinner on a TV reads as
        //    "something is wrong").
        if (loading) {
            float lineY = h - Tokens.dp(getContext(), 8);
            paint.setColor(0x22FFFFFF);
            canvas.drawRect(pad, lineY, w - pad, lineY + Tokens.dp(getContext(), 2), paint);
            float width = (w - pad * 2) * 0.35f;
            float start = pad + (w - pad * 2 - width) * progress;
            paint.setColor(Tokens.SKY);
            canvas.drawRect(start, lineY, start + width, lineY + Tokens.dp(getContext(), 2), paint);
        }

        // 7. Focused: the ring and a glow in the sky accent. The ring is its own token so it stays visible on
        //    this card's own blue (DESIGN.md, "Colors").
        if (focused) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Tokens.dp(getContext(), 2));
            paint.setColor(Tokens.RING);
            float inset = Tokens.dp(getContext(), 1);
            canvas.drawRoundRect(new RectF(inset, inset, w - inset, h - inset), radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private String format(double value) {
        if (Double.isNaN(value)) return "--";
        double shown = fahrenheit ? value * 9.0 / 5.0 + 32.0 : value;
        return String.valueOf(Math.round(shown));
    }

    private String short(String ignored) {
        return fahrenheit ? "F" : "C";
    }

    private int skyTop() {
        // Dawn and dusk lean warm, midday blue, night indigo. daylight is 0 at night and 1 at noon.
        int night = 0xFF1B1F3B;
        int day = 0xFF2F6FB5;
        int warm = 0xFFB4622F;
        if (daylight < 0.25f) return blendColor(night, warm, daylight / 0.25f);
        if (daylight < 0.45f) return blendColor(warm, warm, 0f);
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
