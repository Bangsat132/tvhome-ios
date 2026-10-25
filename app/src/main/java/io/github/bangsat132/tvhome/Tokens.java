package io.github.bangsat132.tvhome;

/**
 * The design tokens from DESIGN.md, in one place so no screen invents its own value.
 *
 * <p>Colours are ARGB ints; sizes are dp and converted where they are used ({@link #dp}). Keeping the
 * contract in Java rather than in {@code res/values} is deliberate: a theme JSON can override a colour at
 * runtime (see {@link Theme}), which a resource lookup cannot do without a rebuild.
 */
public final class Tokens {

    private Tokens() {}

    // --- colors (DESIGN.md, "Colors") ---
    public static final int PRIMARY = 0xFF0A84FF;
    public static final int SURFACE = 0xFF0B0B0F;
    public static final int SURFACE_RAISED = 0xFF16161C;
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFA1A1AA;
    public static final int TEXT_TERTIARY = 0xFF6E6E76;
    public static final int SCRIM = 0xFF000000;
    public static final int RING = 0xFFFFFFFF;
    public static final int SKY = 0xFF4FC3F7;
    public static final int WARM = 0xFFFFB74D;
    public static final int DANGER = 0xFFFF453A;
    public static final int SUCCESS = 0xFF32D74B;

    /** Glass fills, as alpha over whatever is behind: the dock, the pills, the focus highlight. */
    public static final int GLASS_DOCK = 0x1FFFFFFF;      // 12 %
    public static final int GLASS_PILL = 0x14FFFFFF;      // 8 %
    public static final int GLASS_PILL_FOCUS = 0x2EFFFFFF; // 18 %
    public static final int GLASS_RIM = 0x38FFFFFF;       // 22 %

    // --- radius (DESIGN.md, "Shapes": one knob) ---
    public static final int RADIUS_KNOB = 18;
    public static final int RADIUS_SM = 8;
    public static final int RADIUS_TILE = 14;
    public static final int RADIUS_LG = 18;

    // --- spacing (DESIGN.md, "Layout") ---
    public static final int SCREEN_EDGE = 48;
    public static final int DOCK_GAP = 20;
    public static final int TOP_BAR_HEIGHT = 96;
    public static final int DOCK_BOTTOM = 32;

    // --- type scale (DESIGN.md, "Typography") ---
    public static final float DISPLAY_XL = 72f;
    public static final float DISPLAY_LG = 48f;
    public static final float TITLE_XL = 28f;
    public static final float TITLE_LG = 22f;
    public static final float BODY = 16f;
    public static final float LABEL = 13f;
    public static final float META = 12f;

    // --- motion (DESIGN.md, "Motion & Interaction") ---
    public static final int SNAP_MS = 120;
    public static final int SETTLE_MS = 220;
    public static final int CROSSFADE_MS = 180;
    /** The focused tile grows 6 %; at 6 columns a larger lift collides with its neighbour. */
    public static final float FOCUS_SCALE = 1.06f;

    /**
     * dp to px at the density this TV reports, scaled for the panel's height.
     *
     * <p>Every length above is written for a 1080p panel: DESIGN.md was drawn at that size because it is what
     * the reference launcher and most sets here use. This launcher's first TV is 1280x720 (POLYTRON 2K, HKC
     * nagai, 213 dpi), where the unscaled dock and grid fell off the bottom of the screen. Rather than a second
     * set of numbers to keep in step, one factor scales all of them: 720/1080 = 0.667 on a 720p set, 1.0 on a
     * 1080p one, and it is capped at 1 on a larger panel so a 4K set does not grow a 300 dp clock.
     */
    public static float dp(android.content.Context ctx, float value) {
        return value * ctx.getResources().getDisplayMetrics().density * scaleFactor(ctx);
    }

    /** The panel factor: 1.0 at 1080p and above, 0.667 at 720p, floored so nothing gets unreadable. */
    public static float scaleFactor(android.content.Context ctx) {
        int heightPx = ctx.getResources().getDisplayMetrics().heightPixels;
        int widthPx = ctx.getResources().getDisplayMetrics().widthPixels;
        int shortSide = Math.min(heightPx, widthPx);
        float factor = shortSide / 1080f;
        return Math.max(0.62f, Math.min(1f, factor));
    }
}
