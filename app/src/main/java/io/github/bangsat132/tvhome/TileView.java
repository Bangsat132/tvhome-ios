package io.github.bangsat132.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * One app tile: artwork on a glass slab, name only while focused.
 *
 * <p>DESIGN.md, "Components / app-tile": default, focused (ring, scale 1.06, brighter fill), pressed and
 * disabled. The tile grows toward the viewer rather than drawing a per-tile border, which is the tvOS cue
 * this launcher borrows.
 */
final class TileView extends FrameLayout {

    private final ImageView art;
    private final TextView name;
    private final View ring;
    private boolean focused;

    TileView(Context context, AppList.App app) {
        super(context);
        setFocusable(true);
        setClickable(true);
        setFocusableInTouchMode(false);
        setClipChildren(false);
        setClipToPadding(false);
        setContentDescription(app.label);

        art = new ImageView(context);
        art.setScaleType(ImageView.ScaleType.FIT_CENTER);
        // Icons, not banners: a TV banner is 320x180 and squashing it into a square tile stretched a pure
        // gradient across the whole tile, which is what the first build showed (an orange slab instead of
        // TizenTube). tvOS tiles are icon squares too (DESIGN.md, "Components / app-tile").
        Drawable artwork = app.icon != null ? app.icon : app.banner;
        if (artwork != null) {
            art.setImageDrawable(square(artwork));
        }
        FrameLayout.LayoutParams artLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int inset = Math.round(Tokens.dp(context, 14));
        artLp.setMargins(inset, inset, inset, inset);
        addView(art, artLp);

        name = new TextView(context);
        name.setText(AppList.fit(app.label, 22));
        name.setTextColor(Tokens.TEXT_PRIMARY);
        name.setTextSize(Tokens.dp(context, Tokens.META));
        name.setTypeface(Typefaces.medium(context));
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        // DESIGN.md, "Components / app-tile": the name is always there, dimmed while the tile is not focused.
        // Showing it only on focus left most of the grid unreadable on the first build.
        name.setAlpha(0.62f);
        name.setShadowLayer(Tokens.dp(context, 6), 0, Tokens.dp(context, 1), 0xCC000000);
        FrameLayout.LayoutParams nameLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameLp.gravity = Gravity.BOTTOM;
        nameLp.bottomMargin = Math.round(Tokens.dp(context, 8));
        addView(name, nameLp);

        // The ring is a separate view so it can sit outside the tile's bounds while the tile scales.
        ring = new View(context);
        ring.setBackground(Drawables.ring(context, Tokens.RADIUS_TILE));
        ring.setVisibility(INVISIBLE);
        addView(ring, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /** A banner is 16:9 and an icon is square; both are centred on a square canvas so the grid stays even. */
    private static Drawable square(Drawable source) {
        int size = 256;
        int w = source.getIntrinsicWidth();
        int h = source.getIntrinsicHeight();
        if (w <= 0 || h <= 0) return source;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        // Pillow-soft corners on the artwork itself, like the tvOS icon grid. The mask is punched with
        // DST_IN after the icon is drawn: drawing a rounded rect first did nothing at all, because an empty
        // Paint has no colour to fill with.
        float r = size * 0.20f;
        float scale = Math.min(size / (float) w, size / (float) h);
        int dw = Math.round(w * scale);
        int dh = Math.round(h * scale);
        source.setBounds((size - dw) / 2, (size - dh) / 2, (size + dw) / 2, (size + dh) / 2);
        source.draw(canvas);
        Paint mask = new Paint(Paint.ANTI_ALIAS_FLAG);
        mask.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));
        mask.setColor(Color.BLACK);
        canvas.drawRoundRect(new RectF(0, 0, size, size), r, r, mask);
        return new BitmapDrawable(null, bitmap);
    }

    void setFocusedState(boolean hasFocus, boolean reducedMotion) {
        focused = hasFocus;
        ring.setVisibility(hasFocus ? VISIBLE : INVISIBLE);
        name.setAlpha(hasFocus ? 1f : 0.62f);
        float scale = (!hasFocus || reducedMotion) ? 1f : Tokens.FOCUS_SCALE;
        animate().cancel();
        animate().scaleX(scale).scaleY(scale)
                .setDuration(reducedMotion ? 0 : Tokens.SETTLE_MS)
                .start();
        // One elevation step, so the focused tile draws over its neighbours' rims, never a shadow.
        setTranslationZ(hasFocus ? Tokens.dp(getContext(), 2) : 0f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        ViewGroup.LayoutParams lp = ring.getLayoutParams();
        if (lp != null) {
            lp.width = w;
            lp.height = h;
            ring.setLayoutParams(lp);
        }
    }
}
