package io.github.bangsat132.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * The background field: a black surface with a radial lift toward the top centre, then the scrim grade.
 *
 * <p>DESIGN.md, "Colors": the field is not flat black, because a flat black panel at 2 m reads as a
 * switched-off TV. DESIGN.md, "Elevation & Depth": the depth comes from the scrim grade, not from a shadow.
 * The weather card's sky gradient lives here too, drawn from the same sun position the card uses, so the
 * field and the card never disagree about what time it is.
 */
public final class FieldView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    /** 0 = night, 1 = full day. Set from the weather card's is_day, or from the clock before it arrives. */
    private float daylight = 0.5f;
    private int wallpaperColorTop = 0;
    private int wallpaperColorBottom = 0;

    public FieldView(Context context) {
        super(context);
        setFocusable(false);
        setClickable(false);
    }

    public void setDaylight(float value) {
        if (Math.abs(value - daylight) < 0.01f) return;
        daylight = Math.max(0f, Math.min(1f, value));
        invalidate();
    }

    public void setWallpaper(int top, int bottom) {
        wallpaperColorTop = top;
        wallpaperColorBottom = bottom;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        bounds.set(0, 0, w, h);

        // 1. The base field.
        canvas.drawColor(Tokens.SURFACE);

        // 2. A wheel of colour behind the shelf that follows the sun: warm at dawn and dusk, blue at noon,
        //    indigo at night. Alpha stays low so a wallpaper still reads through it.
        int glow = blend(nightGlow(), dayGlow(), daylight);
        paint.setShader(new RadialGradient(w * 0.62f, h * 0.10f, Math.max(w, h) * 0.85f,
                new int[]{withAlpha(glow, 66), withAlpha(glow, 0)}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(bounds, paint);
        paint.setShader(null);

        // 3. The wallpaper, if a theme brought one, as a soft vertical grade.
        if (wallpaperColorTop != 0) {
            paint.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{wallpaperColorTop, wallpaperColorBottom != 0 ? wallpaperColorBottom : wallpaperColorTop},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(bounds, paint);
            paint.setShader(null);
        }

        // 4. The scrim grade: 70 % black at the top, 92 % at the bottom. It keeps type readable on any
        //    wallpaper and gives the picture depth (DESIGN.md, "Elevation & Depth").
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{argb(0.70f), argb(0.78f), argb(0.92f)},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(bounds, paint);
        paint.setShader(null);

        // 5. A faint horizon line where the dock band will sit, so the field has a floor rather than a fade.
        paint.setColor(0x14FFFFFF);
        paint.setStrokeWidth(Math.max(1f, h / 1080f));
        canvas.drawLine(0, h * 0.86f, w, h * 0.86f, paint);
    }

    private int dayGlow() {
        return Color.rgb(0x3A, 0x6E, 0xA8);
    }

    private int nightGlow() {
        return Color.rgb(0x24, 0x22, 0x4A);
    }

    private static int argb(float alpha) {
        return Color.argb(Math.round(alpha * 255), 0, 0, 0);
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int blend(int from, int to, float t) {
        return Color.rgb(
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t));
    }
}
