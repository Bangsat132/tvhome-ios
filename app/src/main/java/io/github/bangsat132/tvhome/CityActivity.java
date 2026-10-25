package io.github.bangsat132.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * The city picker: type a name with the remote's keyboard, pick from the matches, and Home remembers it.
 *
 * <p>DESIGN.md does not cover a full-screen form, so this screen borrows its field, its type and its glass:
 * one raised card on the field, an input at the top, results below. It is reachable from Home by holding OK on
 * the weather card, and it never shows on its own.
 */
public final class CityActivity extends Activity {

    public static final String EXTRA_PICKED = "picked";
    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LON = "lon";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout results;
    private EditText input;
    private TextView status;
    private Runnable pending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(android.R.color.black);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Tokens.SURFACE);
        int pad = Math.round(Tokens.dp(this, 72));
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(getString(R.string.pick_city));
        title.setTextColor(Tokens.TEXT_PRIMARY);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, Tokens.TITLE_XL);
        title.setTypeface(Typefaces.light(this));
        root.addView(title);

        status = new TextView(this);
        status.setTextColor(Tokens.TEXT_TERTIARY);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, Tokens.META);
        status.setTypeface(Typefaces.regular(this));
        status.setPadding(0, Math.round(Tokens.dp(this, 8)), 0, Math.round(Tokens.dp(this, 24)));
        root.addView(status);

        input = new EditText(this);
        input.setHint(getString(R.string.pick_city_hint));
        input.setSingleLine(true);
        input.setTextColor(Tokens.TEXT_PRIMARY);
        input.setHintTextColor(Tokens.TEXT_TERTIARY);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, Tokens.TITLE_LG);
        input.setTypeface(Typefaces.regular(this));
        input.setBackground(Drawables.glass(this, Tokens.GLASS_PILL, Math.round(Tokens.dp(this, Tokens.RADIUS_SM)),
                Math.round(Tokens.dp(this, 1)), Tokens.GLASS_RIM));
        input.setPadding(Math.round(Tokens.dp(this, 20)), Math.round(Tokens.dp(this, 16)),
                Math.round(Tokens.dp(this, 20)), Math.round(Tokens.dp(this, 16)));
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        root.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setPadding(0, Math.round(Tokens.dp(this, 24)), 0, 0);
        root.addView(results);

        setContentView(root);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { scheduleSearch(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (pending != null) { handler.removeCallbacks(pending); pending.run(); }
                return true;
            }
        });

        input.requestFocus();
    }

    /** Search after a short pause, so the keyboard's every letter is not a request to Open-Meteo. */
    private void scheduleSearch(final String query) {
        if (pending != null) handler.removeCallbacks(pending);
        if (query.trim().length() < 2) {
            results.removeAllViews();
            return;
        }
        pending = new Runnable() {
            @Override public void run() { search(query.trim()); }
        };
        handler.postDelayed(pending, 450);
    }

    private void search(final String query) {
        status.setText(getString(R.string.search) + "…");
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final List<Weather.Place> places = Weather.search(query, 6);
                    runOnUiThread(new Runnable() {
                        @Override public void run() { show(places); }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            results.removeAllViews();
                            status.setText(getString(R.string.weather_unavailable));
                        }
                    });
                }
            }
        }, "city-search").start();
    }

    private void show(List<Weather.Place> places) {
        results.removeAllViews();
        status.setText(places.isEmpty() ? "—" : "");
        for (final Weather.Place place : places) {
            TextView row = new TextView(this);
            row.setText(place.toString());
            row.setTextColor(Tokens.TEXT_PRIMARY);
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, Tokens.TITLE_LG);
            row.setTypeface(Typefaces.regular(this));
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setFocusable(true);
            row.setClickable(true);
            row.setPadding(Math.round(Tokens.dp(this, 20)), Math.round(Tokens.dp(this, 16)),
                    Math.round(Tokens.dp(this, 20)), Math.round(Tokens.dp(this, 16)));
            row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    v.setBackground(Drawables.glass(
                            hasFocus ? Tokens.GLASS_PILL_FOCUS : Color.TRANSPARENT,
                            Math.round(Tokens.dp(CityActivity.this, Tokens.RADIUS_SM)),
                            Math.round(Tokens.dp(CityActivity.this, 1)),
                            hasFocus ? Tokens.RING : Tokens.GLASS_RIM));
                }
            });
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { pick(place); }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, Math.round(Tokens.dp(this, 8)));
            results.addView(row, lp);
        }
        if (!places.isEmpty()) results.getChildAt(0).requestFocus();
    }

    private void pick(Weather.Place place) {
        setResult(RESULT_OK, new Intent()
                .putExtra(EXTRA_PICKED, place.name)
                .putExtra(EXTRA_LAT, place.latitude)
                .putExtra(EXTRA_LON, place.longitude));
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private String upper(String s) {
        return s.toUpperCase(Locale.getDefault());
    }
}
