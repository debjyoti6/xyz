package com.quiet.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import java.text.Collator;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Android 12+ launcher. No network, accessibility, or device-admin permissions. */
public final class MainActivity extends Activity {
    private SharedPreferences prefs;
    private final ArrayList<App> apps = new ArrayList<>();
    private final ArrayList<String> favorites = new ArrayList<>();
    private Set<String> paused, hidden;
    private LinearLayout root;
    private boolean light, started, loaded;
    private int fg, muted, bg, accent;
    private String screen = "home";
    private CountDownTimer timer;
    private AlertDialog pauseDialog;
    private EditText search;
    private TextView emptyMessage;
    private final ArrayList<App> filtered = new ArrayList<>();
    private BaseAdapter adapter;
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private int loadGeneration;
    private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { loadApps(); }
    };

    private static final class App {
        final String name, id;
        final ComponentName component;
        App(String name, ComponentName component) {
            this.name = name; this.component = component; this.id = component.flattenToString();
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("quiet", MODE_PRIVATE);
        light = prefs.getBoolean("light", false);
        paused = new HashSet<>(prefs.getStringSet("paused", Collections.emptySet()));
        hidden = new HashSet<>(prefs.getStringSet("hidden", Collections.emptySet()));
        try {
            JSONArray a = new JSONArray(prefs.getString("favorites", "[]"));
            for (int i = 0; i < a.length(); i++) {
                String id = a.getString(i); if (!favorites.contains(id)) favorites.add(id);
            }
        } catch (JSONException ignored) { favorites.clear(); }
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::home);
        }
        if (!prefs.getBoolean("welcomed", false)) welcome();
        else if (state != null && "settings".equals(state.getString("screen"))) settings();
        else if (state != null && "apps".equals(state.getString("screen"))) {
            allApps(); search.setText(state.getString("query", ""));
        } else home();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("screen", screen);
        if (search != null) state.putString("query", search.getText().toString());
        super.onSaveInstanceState(state);
    }
    @Override protected void onStart() {
        super.onStart(); started = true;
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED); f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED); f.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packagesChanged, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(packagesChanged, f);
    }
    @Override protected void onResume() { super.onResume(); loadApps(); }
    @Override protected void onPause() { cancelPause(); super.onPause(); }
    @Override protected void onStop() {
        if (started) { unregisterReceiver(packagesChanged); started = false; }
        super.onStop();
    }
    @Override protected void onDestroy() { loadGeneration++; loader.shutdownNow(); super.onDestroy(); }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); home(); }
    @Override public void onBackPressed() { home(); }

    /** Package queries stay off the UI thread. Only the newest result is applied. */
    private void loadApps() {
        final int generation = ++loadGeneration;
        loader.execute(() -> {
            ArrayList<App> result = new ArrayList<>();
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            HashSet<String> seen = new HashSet<>();
            try {
                for (ResolveInfo info : getPackageManager().queryIntentActivities(query, 0)) {
                    if (info.activityInfo == null || !info.activityInfo.exported ||
                        getPackageName().equals(info.activityInfo.packageName)) continue;
                    ComponentName c = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                    if (seen.add(c.flattenToString())) {
                        result.add(new App(info.loadLabel(getPackageManager()).toString(), c));
                    }
                }
            } catch (RuntimeException e) {
                runOnUiThread(() -> { if (!isDestroyed() && generation == loadGeneration) toast("Could not refresh apps. Try opening Quiet again."); });
                return;
            }
            runOnUiThread(() -> {
                if (isDestroyed() || generation != loadGeneration) return;
                apps.clear(); apps.addAll(result); loaded = true; sortApps();
                if ("apps".equals(screen) && search != null) filter(search.getText().toString());
                else if ("home".equals(screen)) home();
            });
        });
    }
    private void sortApps() { Collator c = Collator.getInstance(); apps.sort((a,b) -> c.compare(label(a),label(b))); }
    private String label(App a) { return prefs.getString("name:" + a.id, a.name); }
    private int appTextSize() { return Math.max(20, Math.min(36, prefs.getInt("textSize", 28))); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setFontFeatureSettings("kern"); t.setIncludeFontPadding(true);
        return t;
    }
    private TextView action(String value, Runnable run) {
        TextView v = text(value, 17, fg); v.setGravity(Gravity.CENTER_VERTICAL);
        v.setMinHeight(dp(52)); v.setPadding(dp(4),dp(12),dp(4),dp(12));
        android.content.res.ColorStateList ripple = android.content.res.ColorStateList.valueOf(
            light ? 0x18000000 : 0x25FFFFFF);
        v.setBackground(new android.graphics.drawable.RippleDrawable(ripple, null,
            new android.graphics.drawable.ColorDrawable(Color.WHITE)));
        v.setFocusable(true); v.setOnClickListener(w -> run.run());
        v.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info); info.setClassName(Button.class.getName());
            }
        });
        return v;
    }
    private void gap(LinearLayout into, int size) { View v = new View(this); into.addView(v,new LinearLayout.LayoutParams(1,dp(size))); }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout scrollingBody() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        LinearLayout body = column(); scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,-1)); return body;
    }
    private void frame(String next) {
        screen = next; search = null; adapter = null; emptyMessage = null;
        light = prefs.getBoolean("light", false);
        bg = Color.parseColor(light ? "#F7F5EF" : "#000000");
        fg = Color.parseColor(light ? "#202020" : "#F5F5F2");
        muted = Color.parseColor(light ? "#66635E" : "#AAAAA5"); accent = fg;
        root = column(); root.setBackgroundColor(bg); setContentView(root);
        getWindow().setDecorFitsSystemWindows(false);
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            int bars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            controller.setSystemBarsAppearance(light ? bars : 0, bars);
        }
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets b = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            v.setPadding(dp(24)+b.left,dp(12)+b.top,dp(24)+b.right,dp(12)+b.bottom); return insets;
        }); root.requestApplyInsets();
    }
    private void closeKeyboard() {
        View focused = getCurrentFocus();
        if (focused != null) ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focused.getWindowToken(),0);
    }
    private void welcome() {
        frame("welcome"); LinearLayout body = scrollingBody(); gap(body,40);
        body.addView(text("Quiet Launcher",36,fg)); gap(body,24);
        body.addView(text("Make room for what matters.",24,fg)); gap(body,18);
        body.addView(text("A simple home screen for Android 12 and newer. Choose favorite apps, reduce visual clutter, and add a pause before opening distractions.\n\nYour apps and files stay on your phone. You can switch back to your previous launcher in Android settings at any time.",18,muted));
        gap(body,24); body.addView(action("Start using Quiet",()->{prefs.edit().putBoolean("welcomed",true).apply();home();}));
        body.addView(action("Choose default home app",this::defaultHome));
    }
    private void home() {
        cancelPause(); closeKeyboard(); frame("home");
        LinearLayout body = scrollingBody();
        TextClock clock = new TextClock(this); clock.setFormat12Hour("h:mm"); clock.setFormat24Hour("HH:mm");
        clock.setTextColor(fg); clock.setTextSize(60); clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        clock.setPadding(0,dp(18),0,0); body.addView(clock);
        TextClock date = new TextClock(this); date.setFormat12Hour("EEE, d MMM"); date.setFormat24Hour("EEE, d MMM");
        date.setTextColor(muted); date.setTextSize(17); body.addView(date); gap(body,24);
        if (!isDefaultHome()) body.addView(action("Set Quiet as your home screen",this::defaultHome));
        View breathingRoom = new View(this);
        body.addView(breathingRoom,new LinearLayout.LayoutParams(1,dp(24),1));
        int shown=0;
        for (String id : favorites) {
            App a = find(id); if (a == null || hidden.contains(id)) continue; shown++;
            TextView row = action(label(a),() -> launch(a)); row.setTextSize(appTextSize());
            row.setContentDescription(label(a)+", favorite");
            row.setOnLongClickListener(v -> { options(a); return true; }); body.addView(row);
        }
        if (shown == 0) {
            body.addView(text(loaded ? "Choose your essentials." : "Loading your apps…",24,fg));
            if (loaded) body.addView(action("Add favorite apps",this::allApps));
        }
        gap(body,26);
        TextView all = action("All apps  →",this::allApps); all.setId(R.id.all_apps); body.addView(all);
        TextView settings = action("Preferences",this::settings); settings.setId(R.id.preferences); body.addView(settings);
    }
    private App find(String id) { for (App a: apps) if (a.id.equals(id)) return a; return null; }
    private void saveFavorites() { prefs.edit().putString("favorites",new JSONArray(favorites).toString()).apply(); }
    private void refreshCurrent() {
        sortApps();
        if ("apps".equals(screen) && search != null) filter(search.getText().toString());
        else if ("home".equals(screen)) home();
    }
    private void allApps() {
        frame("apps"); root.addView(action("←  Home",this::home));
        search = new EditText(this); search.setId(R.id.search_apps); search.setSingleLine(true); search.setTextSize(18);
        search.setTextColor(fg); search.setHintTextColor(muted); search.setHint("Search apps");
        search.setBackgroundTintList(android.content.res.ColorStateList.valueOf(muted));
        search.setContentDescription("Search installed apps"); search.setMinHeight(dp(56));
        root.addView(search,new LinearLayout.LayoutParams(-1,-2));
        TextView help=text("Hold an app for options.",13,muted); help.setPadding(0,dp(8),0,dp(8)); root.addView(help);
        ListView list = new ListView(this); list.setId(R.id.app_list); list.setDivider(null);
        list.setFastScrollEnabled(true);
        adapter = new BaseAdapter() {
            public int getCount() { return filtered.size(); }
            public Object getItem(int p) { return filtered.get(p); }
            public long getItemId(int p) { return p; }
            public View getView(int p, View convert, ViewGroup parent) {
                App a=filtered.get(p); TextView row = convert instanceof TextView ? (TextView)convert : text("",23,fg);
                row.setText(label(a) + (favorites.contains(a.id) ? "  ·" : "") + (paused.contains(a.id) ? "  ◦" : ""));
                row.setTextSize(Math.max(20,appTextSize()-4)); row.setTextColor(fg);
                row.setMinHeight(dp(58)); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(2),dp(12),dp(2),dp(12));
                row.setContentDescription(label(a) + (favorites.contains(a.id) ? ", favorite" : "") + (paused.contains(a.id) ? ", pause enabled" : ""));
                return row;
            }
        };
        list.setAdapter(adapter); root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        emptyMessage = text("",18,muted); emptyMessage.setId(R.id.empty_apps); root.addView(emptyMessage); list.setEmptyView(emptyMessage);
        list.setOnItemClickListener((p,v,pos,id) -> launch(filtered.get(pos)));
        list.setOnItemLongClickListener((p,v,pos,id) -> { closeKeyboard(); options(filtered.get(pos)); return true; });
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int count,int after) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) { filter(s.toString()); }
            public void afterTextChanged(Editable e) {}
        }); filter(""); root.setFocusableInTouchMode(true); root.requestFocus();
    }
    private void filter(String q) {
        filtered.clear(); String needle=q.trim().toLowerCase(Locale.ROOT);
        for(App a:apps) if(!hidden.contains(a.id) &&
            (label(a).toLowerCase(Locale.ROOT).contains(needle) || a.name.toLowerCase(Locale.ROOT).contains(needle))) filtered.add(a);
        if(emptyMessage != null) emptyMessage.setText(loaded ? "No matching apps. Hidden apps can be restored in Preferences." : "Loading your apps…");
        if(adapter != null) adapter.notifyDataSetChanged();
    }
    private AlertDialog.Builder dialog() {
        return new AlertDialog.Builder(this,light ? android.R.style.Theme_Material_Light_Dialog_Alert : android.R.style.Theme_Material_Dialog_Alert);
    }
    private void options(App a) {
        String[] labels={favorites.contains(a.id)?"Remove from favorites":"Add to favorites",
            paused.contains(a.id)?"Remove opening pause":"Add opening pause", "Rename in Quiet", "Move favorite to top", "Hide from Quiet", "Android app information"};
        dialog().setTitle(label(a)).setItems(labels,(d,which)->{
            switch(which) {
                case 0:
                    if(favorites.contains(a.id)) favorites.remove(a.id);
                    else {
                        int visible=0; for (String id:favorites) if(find(id)!=null && !hidden.contains(id)) visible++;
                        if(visible>=8) { toast("Keep up to eight favorites. Remove one first."); return; }
                        favorites.add(a.id);
                    }
                    saveFavorites(); break;
                case 1:
                    if(!paused.add(a.id)) paused.remove(a.id);
                    prefs.edit().putStringSet("paused",new HashSet<>(paused)).apply(); break;
                case 2: rename(a); return;
                case 3:
                    if(!favorites.contains(a.id)) { toast("Add this app to favorites first."); return; }
                    favorites.remove(a.id); favorites.add(0,a.id); saveFavorites(); break;
                case 4:
                    dialog().setTitle("Hide "+label(a)+"?")
                        .setMessage("This only removes it from Quiet. It stays installed. Restore it from Preferences → Hidden apps.")
                        .setNegativeButton("Cancel",null).setPositiveButton("Hide",(dd,w)->{
                            hidden.add(a.id); favorites.remove(a.id); saveFavorites();
                            prefs.edit().putStringSet("hidden",new HashSet<>(hidden)).apply(); refreshCurrent();
                        }).show(); return;
                default:
                    safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+a.component.getPackageName()))); return;
            }
            refreshCurrent();
        }).setNegativeButton("Close",null).show();
    }
    private void rename(App a) {
        EditText input = new EditText(this); input.setSingleLine(true); input.setText(label(a)); input.setSelectAllOnFocus(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(48)}); input.setTextColor(fg);
        input.setContentDescription("App display name");
        AlertDialog d = dialog().setTitle("Rename in Quiet").setView(input).setNegativeButton("Cancel",null)
            .setNeutralButton("Use original",(x,w)->{prefs.edit().remove("name:"+a.id).apply();refreshCurrent();})
            .setPositiveButton("Save",null).create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if(value.isEmpty()) { input.setError("Enter a name"); return; }
            prefs.edit().putString("name:"+a.id,value).apply(); d.dismiss(); refreshCurrent();
        })); d.show();
    }
    private void hiddenApps() {
        if (hidden.isEmpty()) { toast("No hidden apps."); return; }
        ArrayList<String> ids = new ArrayList<>(hidden);
        ids.sort((a,b)->displayHidden(a).compareToIgnoreCase(displayHidden(b)));
        String[] names = new String[ids.size()]; for(int i=0;i<ids.size();i++) names[i]=displayHidden(ids.get(i));
        dialog().setTitle("Tap an app to restore it").setItems(names,(d,pos)->{
            hidden.remove(ids.get(pos)); prefs.edit().putStringSet("hidden",new HashSet<>(hidden)).apply();
            toast("App restored to All apps.");settings();
        }).setPositiveButton("Restore all",(d,w)->{
            hidden.clear();prefs.edit().putStringSet("hidden",new HashSet<>(hidden)).apply();settings();
        }).setNegativeButton("Close",null).show();
    }
    private String displayHidden(String id) { App a=find(id); return a==null ? prefs.getString("name:"+id,id) : label(a); }
    private int pauseSeconds() { return Math.max(3, Math.min(15, prefs.getInt("pauseSeconds", 5))); }
    private void launch(App app) {
        closeKeyboard();
        if(!paused.contains(app.id)) { openApp(app); return; }
        cancelPause(); int seconds=pauseSeconds();
        pauseDialog=dialog().setTitle("Take a breath.").setMessage("What are you opening "+label(app)+" for?")
            .setNegativeButton("Stay here",null).setPositiveButton("Wait "+seconds+"s",(d,w)->openApp(app)).create();
        pauseDialog.setOnDismissListener(d->{ if(timer!=null) {timer.cancel();timer=null;} pauseDialog=null; });
        pauseDialog.show(); Button button=pauseDialog.getButton(AlertDialog.BUTTON_POSITIVE); button.setEnabled(false);
        timer=new CountDownTimer(seconds*1000L,250) {
            public void onTick(long left) { button.setText("Wait "+((left+999)/1000)+"s"); }
            public void onFinish() { button.setText("Open app"); button.setEnabled(true); }
        }.start();
    }
    private void cancelPause() {
        if(timer!=null) {timer.cancel();timer=null;}
        if(pauseDialog!=null) { AlertDialog d=pauseDialog; pauseDialog=null; d.dismiss(); }
    }
    private void openApp(App a) {
        safeStart(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(a.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
    }
    private void safeStart(Intent i) {
        try { startActivity(i); }
        catch (android.content.ActivityNotFoundException | SecurityException e) { toast("Unable to open. The app may be unavailable or restricted by Android."); loadApps(); }
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    private boolean isDefaultHome() {
        RoleManager rm=getSystemService(RoleManager.class); return rm!=null && rm.isRoleHeld(RoleManager.ROLE_HOME);
    }
    private void defaultHome() {
        RoleManager rm=getSystemService(RoleManager.class);
        if(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
            safeStart(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)); return;
        }
        safeStart(new Intent(Settings.ACTION_HOME_SETTINGS));
    }
    private void settings() {
        closeKeyboard(); frame("settings"); LinearLayout body=scrollingBody();
        body.addView(action("←  Home",this::home)); body.addView(text("Preferences",32,fg)); gap(body,18);
        body.addView(action(isDefaultHome()?"Change default home app":"Set as default home",this::defaultHome));
        body.addView(action(light?"Appearance: Paper":"Appearance: Black",()->{prefs.edit().putBoolean("light",!light).apply();settings();}));
        body.addView(action("App text size: "+appTextSize(),()->{
            String[] labels={"Compact · 24","Comfortable · 28","Large · 32"}; int[] sizes={24,28,32};
            dialog().setTitle("App text size").setItems(labels,(d,w)->{prefs.edit().putInt("textSize",sizes[w]).apply();settings();}).show();
        }));
        body.addView(action("Opening pause: "+pauseSeconds()+" seconds",()->{
            String[] labels={"3 seconds","5 seconds","10 seconds","15 seconds"}; int[] seconds={3,5,10,15};
            dialog().setTitle("Opening pause").setItems(labels,(d,w)->{prefs.edit().putInt("pauseSeconds",seconds[w]).apply();settings();}).show();
        }));
        body.addView(action("Choose favorite apps",this::allApps));
        TextView h=action("Hidden apps ("+hidden.size()+")",this::hiddenApps);h.setId(R.id.hidden_apps);body.addView(h);
        body.addView(action("Clear favorites",()->dialog().setTitle("Clear favorites?").setMessage("Your apps will stay installed.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{favorites.clear();saveFavorites();settings();}).show()));
        body.addView(action("Remove all opening pauses",()->{paused.clear();prefs.edit().putStringSet("paused",new HashSet<>(paused)).apply();toast("Opening pauses removed.");}));
        body.addView(action("Android settings",()->safeStart(new Intent(Settings.ACTION_SETTINGS))));
        body.addView(action("Help & privacy",()->dialog().setTitle("Your phone, your choice")
            .setMessage("Hold an app to rename, favorite, hide, or add an opening pause.\n\nTo switch back, open Android Settings → Apps → Default apps → Home app.\n\nPauses work only for apps opened from Quiet. Hidden apps remain accessible outside Quiet. This is not a device-wide app blocker.\n\nOffline. No ads, account, analytics, or sensitive permissions. Preferences stay on this device; Android backup is disabled.\n\nVersion 1.1 · Android 12+ · Personal profile only. Work profiles, Private Space, widgets, notification filtering, and in-app time reminders are not included.")
            .setPositiveButton("Got it",null).show()));
    }
}
