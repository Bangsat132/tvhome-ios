package io.github.bangsat132.tvhome;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The apps Home can show: the leanback ones first, then ordinary Android apps, deduplicated and sorted.
 *
 * <p>Without a {@code <queries>} block in the manifest, Android 11+ hides every package from the launcher and
 * the grid comes up empty; that block is in the manifest, and this class assumes it worked.
 */
public final class AppList {

    /** One installed app, with the artwork the grid needs. */
    public static final class App {
        public final String label;
        public final String pkg;
        public final String component;
        public final Drawable icon;
        public final Drawable banner;

        App(String label, String pkg, String component, Drawable icon, Drawable banner) {
            this.label = label;
            this.pkg = pkg;
            this.component = component;
            this.icon = icon;
            this.banner = banner;
        }
    }

    private AppList() {}

    public static List<App> load(Context context, List<String> hidden) {
        PackageManager pm = context.getPackageManager();
        Map<String, App> byPackage = new LinkedHashMap<>();

        // Leanback apps first: on a TV these are the real targets.
        collect(context, pm, byPackage, hidden, "android.intent.category.LEANBACK_LAUNCHER");
        collect(context, pm, byPackage, hidden, "android.intent.category.LAUNCHER");

        List<App> apps = new ArrayList<>(byPackage.values());
        Collections.sort(apps, new Comparator<App>() {
            @Override
            public int compare(App a, App b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return apps;
    }

    private static void collect(Context context, PackageManager pm, Map<String, App> into,
                                List<String> hidden, String category) {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_MAIN);
        intent.addCategory(category);
        List<ResolveInfo> found;
        try {
            found = pm.queryIntentActivities(intent, 0);
        } catch (Exception e) {
            return;
        }
        String self = context.getPackageName();
        for (ResolveInfo info : found) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (self.equals(pkg) || hidden.contains(pkg) || into.containsKey(pkg)) continue;
            ApplicationInfo appInfo = info.activityInfo.applicationInfo;
            if (appInfo == null) continue;
            if ((appInfo.flags & ApplicationInfo.FLAG_INSTALLED) == 0) continue;
            String label;
            try {
                label = pm.getApplicationLabel(appInfo).toString();
            } catch (Exception e) {
                label = pkg;
            }
            Drawable icon = null;
            Drawable banner = null;
            try {
                icon = pm.getApplicationIcon(appInfo);
            } catch (Exception ignored) { }
            try {
                banner = pm.getApplicationBanner(appInfo);
            } catch (Exception ignored) { }
            String component = pkg + "/" + info.activityInfo.name;
            into.put(pkg, new App(label, pkg, component, icon, banner));
        }
    }

    /** The dock in `dock_apps` order, anything missing skipped, then filled from the grid while `fill`. */
    public static List<App> dock(List<App> apps, List<String> dockPackages, int slots, boolean fill) {
        List<App> row = new ArrayList<>();
        for (String pkg : dockPackages) {
            for (App app : apps) {
                if (app.pkg.equals(pkg) && !row.contains(app)) {
                    row.add(app);
                    break;
                }
            }
        }
        if (fill) {
            for (App app : apps) {
                if (row.size() >= slots) break;
                if (!row.contains(app)) row.add(app);
            }
        }
        while (row.size() > slots) row.remove(row.size() - 1);
        return row;
    }

    /** Everything not already in the dock, in the grid's order. */
    public static List<App> grid(List<App> apps, List<App> dock) {
        List<App> rest = new ArrayList<>();
        for (App app : apps) {
            if (!dock.contains(app)) rest.add(app);
        }
        return rest;
    }

    /** A tile's name must never wrap; the tile is measured to fit, so this only guards the absurd case. */
    public static String fit(String label, int maxChars) {
        if (label == null) return "";
        return label.length() <= maxChars ? label : label.substring(0, Math.max(1, maxChars - 1)) + "…";
    }

    /** Unused in the grid, kept for the item layout's type so a caller can size a banner safely. */
    static void scaleToWidth(Drawable drawable, int widthPx) {
        if (drawable == null) return;
        int h = drawable.getIntrinsicHeight();
        int w = drawable.getIntrinsicWidth();
        if (w <= 0 || h <= 0) return;
        drawable.setBounds(0, 0, widthPx, Math.round(widthPx * (float) h / w));
    }

    static void clearDrawable(ImageView view) {
        view.setImageDrawable(null);
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp != null) view.setLayoutParams(lp);
    }
}
