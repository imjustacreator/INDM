package app.indm.remote;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shows the control page of INDM on the PC (the page is served by the PC, so the app stays small and always
 * matches the PC version), and adds what a web page cannot do: scan the QR code, listen for finished downloads
 * and save finished files onto the phone.
 */
public class MainActivity extends Activity {
    private static final int REQ_SCAN = 1, REQ_NOTIF = 2, REQ_STORAGE = 3;
    private static final int ID_SCAN = 1, ID_ENTER = 2, ID_NOTIFY = 3, ID_RELOAD = 4, ID_BATTERY = 5, ID_FORGET = 6;
    private static final Pattern REL = Pattern.compile("^files/([A-Za-z0-9_-]{1,40})/.{0,700}$", Pattern.DOTALL);

    private WebView web;
    private LinearLayout empty;
    private TextView emptyTitle, emptyHint;
    private Button emptyRetry;
    private String pendingRel, pendingName;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF16181D);

        web = new WebView(this);
        web.setBackgroundColor(0xFF16181D);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportZoom(false);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new Client());
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setBackgroundColor(0xFF16181D);
        int pad = dp(28);
        empty.setPadding(pad, pad, pad, pad);
        emptyTitle = text(20, Color.WHITE);
        emptyHint = text(15, 0xFF9AA1AD);
        empty.addView(emptyTitle);
        empty.addView(emptyHint);
        Button scan = button(R.string.btn_scan, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scan();
            }
        });
        Button enter = button(R.string.btn_enter, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterAddress();
            }
        });
        emptyRetry = button(R.string.btn_retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });
        empty.addView(scan);
        empty.addView(emptyRetry);
        empty.addView(enter);
        root.addView(empty, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        refresh();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView text(int sp, int color) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 0, 0, dp(16));
        return t;
    }

    private Button button(int label, View.OnClickListener l) {
        Button bt = new Button(this);
        bt.setText(label);
        bt.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(6), 0, 0);
        bt.setLayoutParams(lp);
        return bt;
    }

    // ---- states

    private void refresh() {
        String base = Prefs.base(this);
        if (base.isEmpty()) {
            showEmpty(R.string.empty_title, R.string.empty_hint, false);
        } else {
            empty.setVisibility(View.GONE);
            web.setVisibility(View.VISIBLE);
            web.loadUrl(base);
        }
    }

    private void showEmpty(int title, int hint, boolean retry) {
        web.setVisibility(View.INVISIBLE);
        emptyTitle.setText(title);
        emptyHint.setText(hint);
        emptyRetry.setVisibility(retry ? View.VISIBLE : View.GONE);
        empty.setVisibility(View.VISIBLE);
    }

    private final class Client extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView v, String url) {
            return !url.startsWith(Prefs.base(MainActivity.this));      // never leave the PC's page
        }

        @Override
        public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
            if (req.isForMainFrame()) showEmpty(R.string.offline_title, R.string.offline_hint, true);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onReceivedError(WebView v, int code, String desc, String url) {
            if (url != null && url.equals(Prefs.base(MainActivity.this))) {
                showEmpty(R.string.offline_title, R.string.offline_hint, true);
            }
        }

        @Override
        public void onPageStarted(WebView v, String url, Bitmap icon) {
            if (url != null && url.equals(Prefs.base(MainActivity.this))) empty.setVisibility(View.GONE);
        }
    }

    // ---- menu

    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        m.add(0, ID_SCAN, 0, R.string.menu_scan);
        m.add(0, ID_ENTER, 1, R.string.menu_enter);
        m.add(0, ID_NOTIFY, 2, R.string.menu_notify).setCheckable(true);
        m.add(0, ID_RELOAD, 3, R.string.menu_reload);
        m.add(0, ID_BATTERY, 4, R.string.menu_battery);
        m.add(0, ID_FORGET, 5, R.string.menu_forget);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu m) {
        m.findItem(ID_NOTIFY).setChecked(Prefs.notifyOn(this));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ID_SCAN:
                scan();
                return true;
            case ID_ENTER:
                enterAddress();
                return true;
            case ID_NOTIFY:
                setNotify(!item.isChecked());
                return true;
            case ID_RELOAD:
                refresh();
                return true;
            case ID_BATTERY:
                batterySettings();
                return true;
            case ID_FORGET:
                Prefs.forget(this);
                stopService(new Intent(this, NotifyService.class));
                refresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // ---- pairing

    private void scan() {
        startActivityForResult(new Intent(this, ScanActivity.class), REQ_SCAN);
    }

    private void enterAddress() {
        final EditText et = new EditText(this);
        et.setSingleLine(true);
        et.setHint("192.168.1.20:38460/m/...");
        new AlertDialog.Builder(this)
                .setTitle(R.string.enter_title)
                .setMessage(R.string.enter_msg)
                .setView(et)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String base = PairLink.parseLoose(et.getText().toString());
                        if (base == null) {
                            Toast.makeText(MainActivity.this, R.string.enter_bad, Toast.LENGTH_LONG).show();
                        } else {
                            paired(base);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void paired(String base) {
        Prefs.setBase(this, base);
        if (Prefs.notifyOn(this)) startListening();
        refresh();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_SCAN && res == RESULT_OK && data != null) {
            String base = data.getStringExtra(ScanActivity.EXTRA_BASE);
            if (base != null) paired(base);
        }
    }

    // ---- notifications

    private void setNotify(boolean on) {
        Prefs.setNotify(this, on);
        if (!on) {
            stopService(new Intent(this, NotifyService.class));
            return;
        }
        if (Prefs.base(this).isEmpty()) {
            Prefs.setNotify(this, false);
            Toast.makeText(this, R.string.pair_first, Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        }
        startListening();
        askBatteryOnce();
    }

    private void startListening() {
        Intent i = new Intent(this, NotifyService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.listen_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void askBatteryOnce() {
        if (Build.VERSION.SDK_INT < 23 || Prefs.askedBattery(this)) return;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm.isIgnoringBatteryOptimizations(getPackageName())) return;
        Prefs.setAskedBattery(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.battery_title)
                .setMessage(R.string.battery_msg)
                .setPositiveButton(R.string.battery_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        batterySettings();
                    }
                })
                .setNegativeButton(R.string.battery_no, null)
                .show();
    }

    private void batterySettings() {
        if (Build.VERSION.SDK_INT < 23) return;
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));
        } catch (ActivityNotFoundException | SecurityException e) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(this, R.string.battery_missing, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
        if (code == REQ_NOTIF && !granted) {
            Toast.makeText(this, R.string.notif_denied, Toast.LENGTH_LONG).show();
        } else if (code == REQ_STORAGE) {
            if (granted && pendingRel != null) save(pendingRel, pendingName);
            else Toast.makeText(this, R.string.storage_denied, Toast.LENGTH_LONG).show();
            pendingRel = pendingName = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        if (Prefs.notifyOn(this) && !Prefs.base(this).isEmpty()) startListening();
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }

    // ---- saving a finished file from the PC onto the phone

    private final class Bridge {
        @JavascriptInterface
        public void save(final String rel, final String name) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.save(rel, name);
                }
            });
        }
    }

    private void save(String rel, String name) {
        String base = Prefs.base(this);
        if (base.isEmpty() || rel == null) return;
        Matcher m = REL.matcher(rel);
        if (!m.matches()) return;
        String url = base + "files/" + m.group(1) + "/f";        // the PC only looks at the id
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 28
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingRel = rel;
            pendingName = name;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
            return;
        }
        Intent i = new Intent(this, SaveService.class);
        i.putExtra(SaveService.EXTRA_URL, url);
        i.putExtra(SaveService.EXTRA_NAME, Names.clean(name));
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
            Toast.makeText(this, R.string.saving_started, Toast.LENGTH_SHORT).show();
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show();
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        }
    }
}
