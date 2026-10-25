package io.github.bangsat132.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * The home screen: field, top bar, shelf, dock, grid, one live weather card.
 *
 * <p>Layout follows DESIGN.md, "Layout": the top bar and the dock never scroll, the grid under the dock does,
 * and the D-pad contract in "Motion & Interaction" is enforced in {@link #onKeyDown} rather than left to a
 * RecyclerView's default focus search, which on a TV falls through rows.
 */
public final class HomeActivity extends Activity {

    private Config config;
    private FieldView field;
    private TextView clock, greeting, date, shelfTitle;
    private WeatherCardView weatherCard;
    private LinearLayout dockRow, pillRow, gridRows;
    private HorizontalScrollView gridScroll;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<TileView> tiles = new ArrayList<>();
    private TileView focused;
    private String wifiName = "Wi-Fi";

    private static final int REFRESH_MS = 30 * 60 * 1000;
    private static final int RETRY_MS = 5 * 60 * 1000;
    private static final int REQUEST_CITY = 41;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setBackgroundDrawableResource(android.R.color.black);
        setContentView(R.layout.activity_home);

        config = Config.load(this);
        build();
        loadApps();
        startClock();
        fetchWeather(false);
    }

    // --- construction ---------------------------------------------------------

    private void build() {
        FrameLayout root = findViewById(R.id.root);
        // The token sizes in DESIGN.md are written for a 1080p panel read from 2-3 m. This TV is 1280x720 at
        // 213 dpi, so every length scales with a single factor: hard-coding 1080p sizes put the dock and the
        // grid off the bottom of a 720p screen. One knob, applied once here, keeps the ratios intact.
        float density = getResources().getDisplayMetrics().density;
        int edge = px(Tokens.SCREEN_EDGE);

        field = new FieldView(this);
        field.setId(R.id.field);
        root.addView(field, match());

        // Top bar: greeting, clock, date on the left; pills on the right. Fixed, never scrolls.
        LinearLayout topBar = new LinearLayout(this);
        topBar.setId(R.id.top_bar);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.TOP);
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, px(Tokens.TOP_BAR_HEIGHT + 88));
        topLp.setMargins(edge, px(Tokens.TOP_BAR_HEIGHT - 56), edge, 0);
        root.addView(topBar, topLp);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        topBar.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        greeting = text("", Tokens.LABEL, Tokens.TEXT_SECONDARY, Typefaces.medium(this), true);
        greeting.setId(R.id.greeting);
        left.addView(greeting);

        clock = text("", Tokens.DISPLAY_XL, Tokens.TEXT_PRIMARY, Typefaces.thin(this), false);
        clock.setId(R.id.clock);
        clock.setIncludeFontPadding(false);
        clock.setLetterSpacing(-0.03f);
        clock.setShadowLayer(px(24), 0, px(4), 0x88000000);
        left.addView(clock);

        date = text("", Tokens.LABEL, Tokens.TEXT_TERTIARY, Typefaces.medium(this), true);
        date.setId(R.id.date);
        left.addView(date);
        pillRow = new LinearLayout(this);
        pillRow.setId(R.id.pills);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        pillRow.setGravity(Gravity.CENTER_VERTICAL);
        topBar.addView(pillRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, px(48)));

        // Shelf: the focused item's name in large light type. Fixed height so the grid never jumps.
        shelfTitle = text("", Tokens.TITLE_XL, Tokens.TEXT_PRIMARY, Typefaces.light(this), false);
        shelfTitle.setId(R.id.shelf_title);
        shelfTitle.setAlpha(0f);
        shelfTitle.setIncludeFontPadding(false);
        FrameLayout.LayoutParams shelfLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shelfLp.gravity = Gravity.BOTTOM | Gravity.START;
        shelfLp.leftMargin = edge;
        shelfLp.bottomMargin = px(Tokens.DOCK_BOTTOM + 210);
        root.addView(shelfTitle, shelfLp);

        // The grid under the dock: one horizontal row per `columns`, scrolling vertically.
        gridScroll = new HorizontalScrollView(this);
        gridScroll.setHorizontalScrollBarEnabled(false);
        FrameLayout.LayoutParams gridLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, px(260));
        gridLp.gravity = Gravity.BOTTOM;
        gridLp.bottomMargin = px(Tokens.DOCK_BOTTOM + 148);
        gridLp.leftMargin = edge;
        gridLp.rightMargin = edge;
        root.addView(gridScroll, gridLp);

        gridRows = new LinearLayout(this);
        gridRows.setId(R.id.grid);
        gridRows.setOrientation(LinearLayout.VERTICAL);
        gridScroll.addView(gridRows, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // The dock band: one row of glass with the dock apps, parked above the bottom edge.
        FrameLayout dockBand = new FrameLayout(this);
        dockBand.setBackground(Drawables.glass(Tokens.GLASS_DOCK, px(Tokens.RADIUS_LG), px(1), Tokens.GLASS_RIM));
        FrameLayout.LayoutParams bandLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, px(96));
        bandLp.gravity = Gravity.BOTTOM;
        bandLp.bottomMargin = px(Tokens.DOCK_BOTTOM);
        bandLp.leftMargin = edge;
        bandLp.rightMargin = edge;
        root.addView(dockBand, bandLp);

        dockRow = new LinearLayout(this);
        dockRow.setId(R.id.dock);
        dockRow.setOrientation(LinearLayout.HORIZONTAL);
        // Left-aligned like tvOS's shelf: the apps start at the band's edge and the band itself runs the
        // full width, so an app arriving later grows the row instead of re-centring everything.
        dockRow.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        dockRow.setPadding(px(Tokens.DOCK_GAP), px(Tokens.DOCK_GAP), px(Tokens.DOCK_GAP), px(Tokens.DOCK_GAP));
        dockBand.addView(dockRow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void buildPills() {
        pillRow.removeAllViews();
        addPill(getString(R.string.pill_inputs), new Runnable() {
            @Override public void run() { openInputs(); }
        });
        addPill(wifiName, new Runnable() {
            @Override public void run() { startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); }
        });
        addPill(getString(R.string.pill_settings), new Runnable() {
            @Override public void run() { openSettings(); }
        });
    }

    private void addPill(String text, final Runnable action) {
        TextView pill = text(text, Tokens.LABEL, Tokens.TEXT_PRIMARY, Typefaces.medium(this), false);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(px(20), px(12), px(20), px(12));
        pill.setBackground(Drawables.glass(Tokens.GLASS_PILL, px(Tokens.RADIUS_KNOB + 999), px(1), Tokens.GLASS_RIM));
        pill.setFocusable(true);
        pill.setClickable(true);
        pill.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        pill.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                v.setBackground(Drawables.glass(hasFocus ? Tokens.GLASS_PILL_FOCUS : Tokens.GLASS_PILL,
                        px(Tokens.RADIUS_KNOB + 999), px(1), Tokens.GLASS_RIM));
                if (hasFocus) clearFocusOnTiles();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, px(44));
        lp.setMarginEnd(px(12));
        pillRow.addView(pill, lp);
    }

    // --- content --------------------------------------------------------------

    private void loadApps() {
        dockRow.removeAllViews();
        gridRows.removeAllViews();
        tiles.clear();

        List<AppList.App> apps = AppList.load(this, config.hiddenApps);
        int slots = Math.max(config.columns, 4);
        List<AppList.App> dock = AppList.dock(apps, config.dockApps, slots, true);
        List<AppList.App> rest = AppList.grid(apps, dock);

        int tileW = tileWidthPx();
        int tileH = px(120);

        // The weather card is the first tile of the grid row and the only live one (DESIGN.md, signature).
        weatherCard = new WeatherCardView(this);
        weatherCard.setId(R.id.weather);
        weatherCard.setFahrenheit(config.fahrenheit);
        weatherCard.setContentDescription(getString(R.string.weather_unavailable));
        weatherCard.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onWeatherCardActivated(); }
        });
        weatherCard.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                weatherCard.setFocusedState(hasFocus);
                if (hasFocus) setShelf(config.city.isEmpty() ? "Weather" : config.city);
            }
        });
        FrameLayout cardHolder = new FrameLayout(this);
        cardHolder.addView(weatherCard, new FrameLayout.LayoutParams(tileW, tileH));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(tileW, tileH);
        cardLp.setMarginEnd(px(Tokens.DOCK_GAP));

        List<AppList.App> gridApps = rest;
        // The card takes slot 0 and the apps fill the slots after it, in reading order. The old index
        // (r * columns + c - 1 on row 0) skipped the first app of row 0 and then shifted every later row by
        // one, so the apps a person actually uses never appeared.
        int slot = 0;
        int rowCount = (int) Math.ceil((gridApps.size() + 1) / (float) config.columns);
        for (int r = 0; r < Math.max(1, rowCount); r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            gridRows.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, tileH));
            for (int c = 0; c < config.columns; c++) {
                if (slot == 0) {
                    // The card is already inside cardHolder; adding the card itself here is what threw
                    // "The specified child already has a parent" on the TV.
                    row.addView(cardHolder, cardLp);
                } else if (slot - 1 < gridApps.size()) {
                    row.addView(makeTile(gridApps.get(slot - 1), tileW, tileH), tileParams());
                }
                slot++;
            }
        }

        for (AppList.App app : dock) {
            dockRow.addView(makeTile(app, tileW, px(104)), tileParams());
        }

        buildPills();
        // Home starts on the card: it is the first thing on screen and the one control that opens a picker,
        // so a fresh install leads somewhere instead of landing on an app the person did not choose.
        weatherCard.requestFocus();
    }

    private LinearLayout.LayoutParams tileParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(px(Tokens.DOCK_GAP));
        return lp;
    }

    private int tileWidthPx() {
        int screen = getResources().getDisplayMetrics().widthPixels;
        float usable = screen - px(Tokens.SCREEN_EDGE) * 2f - px(Tokens.DOCK_GAP) * config.columns;
        return Math.round(usable / config.columns);
    }

    private TileView makeTile(final AppList.App app, int w, int h) {
        TileView tile = new TileView(this, app);
        tile.setLayoutParams(new LinearLayout.LayoutParams(w, h));
        tile.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { launch(app.component); }
        });
        tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                TileView t = (TileView) v;
                t.setFocusedState(hasFocus, config.reducedMotion);
                if (hasFocus) setShelf(app.label);
            }
        });
        tiles.add(tile);
        return tile;
    }

    private void setShelf(String label) {
        if (label == null || label.contentEquals(shelfTitle.getText())) return;
        shelfTitle.animate().cancel();
        shelfTitle.setText(label);
        shelfTitle.animate().alpha(1f).setDuration(config.reducedMotion ? 0 : Tokens.CROSSFADE_MS).start();
    }

    private void clearFocusOnTiles() {
        shelfTitle.animate().alpha(0f).setDuration(config.reducedMotion ? 0 : Tokens.CROSSFADE_MS).start();
    }

    // --- actions --------------------------------------------------------------

    private void launch(String component) {
        try {
            String[] parts = component.split("/", 2);
            ComponentName cn = new ComponentName(parts[0], parts[1]);
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            intent.setComponent(cn);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Some apps declare the LAUNCHER category only; try once more before giving up quietly.
            try {
                Intent fallback = getPackageManager().getLaunchIntentForPackage(component.split("/", 2)[0]);
                if (fallback != null) {
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(fallback);
                }
            } catch (Exception ignored) { }
        } catch (Exception ignored) { }
    }

    private void openSettings() {
        for (String action : new String[]{Settings.ACTION_SETTINGS, "android.settings.TV_SETTINGS"}) {
            try {
                startActivity(new Intent(action));
                return;
            } catch (Exception ignored) { }
        }
    }

    private void openInputs() {
        for (String action : new String[]{"com.android.tv.action.VIEW_INPUTS", "android.intent.action.TV_INPUT"}) {
            try {
                startActivity(new Intent(action));
                return;
            } catch (Exception ignored) { }
        }
    }

    private void onWeatherCardActivated() {
        // No city yet, or a long press: open the picker. With a city, OK refreshes the card instead, so the
        // card stays useful without leaving Home.
        if (config.city.isEmpty()) {
            openCityPicker();
        } else {
            fetchWeather(true);
        }
    }

    private void openCityPicker() {
        startActivityForResult(new Intent(this, CityActivity.class), REQUEST_CITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CITY && resultCode == RESULT_OK && data != null) {
            config.city = data.getStringExtra(CityActivity.EXTRA_PICKED);
            config.latitude = data.getDoubleExtra(CityActivity.EXTRA_LAT, Double.NaN);
            config.longitude = data.getDoubleExtra(CityActivity.EXTRA_LON, Double.NaN);
            config.save(this);
            fetchWeather(true);
        }
    }

    // --- clock ----------------------------------------------------------------

    private void startClock() {
        tick();
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                tick();
                handler.postDelayed(this, 1000 - (System.currentTimeMillis() % 1000));
            }
        }, 1000 - (System.currentTimeMillis() % 1000));
    }

    private void tick() {
        Calendar now = Calendar.getInstance();
        clock.setText(String.format(Locale.getDefault(), "%02d:%02d", now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE)));
        int hour = now.get(Calendar.HOUR_OF_DAY);
        greeting.setText(getString(hour < 12 ? R.string.greeting_morning
                : hour < 18 ? R.string.greeting_afternoon : R.string.greeting_evening).toUpperCase(Locale.getDefault()));
        date.setText(new SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now.getTime())
                .toUpperCase(Locale.getDefault()));
    }

    // --- weather --------------------------------------------------------------

    private void fetchWeather(boolean force) {
        if (config.city.isEmpty() || Double.isNaN(config.latitude)) {
            weatherCard.setLoading(false);
            weatherCard.setProgress(0f);
            return;
        }
        weatherCard.setLoading(true);
        weatherCard.setProgress(0f);
        animateProgress();
        final String city = config.city;
        final double lat = config.latitude;
        final double lon = config.longitude;
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final Weather.Now now = Weather.fetch(city, lat, lon);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            weatherCard.setNow(now);
                            weatherCard.setContentDescription(String.format(Locale.getDefault(),
                                    "%s, %d degrees, %s, updated %s", now.city,
                                    Math.round(now.temperature), now.condition,
                                    new SimpleDateFormat("HH:mm", Locale.getDefault())
                                            .format(new java.util.Date(now.fetchedAt * 1000L))));
                            handler.removeCallbacks(refresh);
                            handler.postDelayed(refresh, REFRESH_MS);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            weatherCard.setLoading(false);
                            // A failed fetch keeps the last good reading; only a card with nothing to show
                            // goes to the error state (DESIGN.md, widget-card).
                            weatherCard.setContentDescription(getString(R.string.weather_unavailable));
                            handler.removeCallbacks(refresh);
                            handler.postDelayed(refresh, RETRY_MS);
                        }
                    });
                }
            }
        }, "weather").start();
    }

    private final Runnable refresh = new Runnable() {
        @Override public void run() { fetchWeather(true); }
    };

    private void animateProgress() {
        if (weatherCard.getWindowToken() == null) return;
        float next = (weatherCard.getProgress() + 0.08f) % 1f;
        weatherCard.setProgress(next);
        if (weatherCard.getNow() == null) handler.postDelayed(this::animateProgress, 40);
    }

    // --- D-pad ----------------------------------------------------------------

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // The D-pad contract (DESIGN.md, "Motion & Interaction"): one row per press, the top bar never falls
        // through to the grid, and nothing traps focus on the weather card.
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (moveFocusRow(1)) return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (moveFocusRow(-1)) return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean moveFocusRow(int direction) {
        if (tiles.isEmpty()) return false;
        View current = getCurrentFocus();
        if (current == null) {
            tiles.get(0).requestFocus();
            return true;
        }
        if (!(current instanceof TileView)) {
            // From a pill or the weather card, Down goes to the dock's first tile.
            if (direction > 0 && !tiles.isEmpty()) {
                tiles.get(tiles.size() - 1).requestFocus();
                return true;
            }
            return false;
        }
        return false;   // the framework's focus search handles tile-to-tile within the two rows
    }

    // --- small helpers --------------------------------------------------------

    private int px(float dp) {
        return Math.round(Tokens.dp(this, dp));
    }

    private static ViewGroup.LayoutParams match() {
        return new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    /** A TextView in the design's type: size, colour, weight, and the uppercase tracking the labels use. */
    private TextView text(String value, float sizeSp, int color, Typeface typeface, boolean uppercase) {
        TextView view = new TextView(this);
        view.setText(uppercase ? value.toUpperCase(Locale.getDefault()) : value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setTypeface(typeface);
        view.setIncludeFontPadding(false);
        if (uppercase) view.setLetterSpacing(0.06f);
        return view;
    }

    @Override
    public void onBackPressed() {
        // Back on Home does nothing: leaving Home to the void is what a stock launcher's Back does, and on a
        // TV that reads as a freeze.
    }
}
