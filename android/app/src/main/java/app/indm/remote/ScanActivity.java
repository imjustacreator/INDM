package app.indm.remote;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Scans the QR code that INDM shows on the PC. Uses the plain camera API: small and works on old phones. */
public class ScanActivity extends Activity implements SurfaceHolder.Callback {
    static final String EXTRA_BASE = "base";
    private static final int REQ_CAMERA = 11;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService decoder = Executors.newSingleThreadExecutor();
    private SurfaceView surface;
    private SurfaceHolder holder;
    private TextView hint;
    private Camera camera;
    private int previewW, previewH;
    private volatile boolean busy;
    private boolean done, askedPermission;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle(R.string.scan_title);
        }
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        surface = new SurfaceView(this);
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        root.addView(new Overlay(this), new FrameLayout.LayoutParams(-1, -1));
        hint = new TextView(this);
        hint.setText(R.string.scan_hint);
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(16);
        hint.setGravity(Gravity.CENTER);
        int pad = dp(24);
        hint.setPadding(pad, pad, pad, pad * 2);
        hint.setBackgroundColor(0x88000000);
        root.addView(hint, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        setContentView(root);
        holder = surface.getHolder();
        holder.addCallback(this);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        done = true;
        decoder.shutdownNow();
        super.onDestroy();
    }

    // ---- permission

    private boolean hasPermission() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code != REQ_CAMERA) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            hint.setText(R.string.scan_perm);
        }
    }

    // ---- surface

    @Override
    public void surfaceCreated(SurfaceHolder h) {
        if (!hasPermission()) {
            if (!askedPermission && Build.VERSION.SDK_INT >= 23) {
                askedPermission = true;
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            }
            return;
        }
        startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int format, int w, int hgt) {
        if (camera == null) return;
        try {
            camera.stopPreview();
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            requestFrame(0);
        } catch (Exception ignored) {
            // the camera was released at the same time
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder h) {
        releaseCamera();
    }

    // ---- camera

    private void startCamera() {
        if (camera != null || done || holder == null || holder.getSurface() == null || !holder.getSurface().isValid()) return;
        try {
            camera = Camera.open();
            Camera.Parameters p = camera.getParameters();
            Camera.Size best = null;
            long target = 1280L * 720L;
            for (Camera.Size s : p.getSupportedPreviewSizes()) {
                long px = (long) s.width * s.height;
                if (px < 640L * 480L || px > 1920L * 1080L) continue;
                if (best == null || Math.abs(px - target) < Math.abs((long) best.width * best.height - target)) best = s;
            }
            if (best == null) best = p.getPreviewSize();
            p.setPreviewSize(best.width, best.height);
            p.setPreviewFormat(ImageFormat.NV21);
            List<String> modes = p.getSupportedFocusModes();
            if (modes != null) {
                if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else if (modes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
            }
            camera.setParameters(p);
            previewW = best.width;
            previewH = best.height;
            camera.setDisplayOrientation(displayOrientation());
            fitSurface();
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            requestFrame(300);
        } catch (Exception e) {
            releaseCamera();
            hint.setText(R.string.scan_nocam);
        }
    }

    private int displayOrientation() {
        int cameraId = 0;
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = rotation == Surface.ROTATION_90 ? 90 : rotation == Surface.ROTATION_180 ? 180
                : rotation == Surface.ROTATION_270 ? 270 : 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - (info.orientation + degrees) % 360) % 360;
        }
        return (info.orientation - degrees + 360) % 360;
    }

    /** Gives the preview the camera's own proportions so the picture is not stretched. */
    private void fitSurface() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int shortSide = Math.min(previewW, previewH), longSide = Math.max(previewW, previewH);
        int w = dm.widthPixels;
        int h = (int) ((long) w * longSide / shortSide);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h, Gravity.CENTER);
        surface.setLayoutParams(lp);
    }

    private void releaseCamera() {
        Camera c = camera;
        camera = null;
        if (c == null) return;
        try {
            c.setOneShotPreviewCallback(null);
            c.stopPreview();
        } catch (Exception ignored) {
            // already stopped
        }
        c.release();
    }

    private void requestFrame(long delayMs) {
        main.postDelayed(new Runnable() {
            @Override
            public void run() {
                final Camera c = camera;
                if (c == null || done || busy) return;
                try {
                    c.setOneShotPreviewCallback(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(final byte[] data, Camera cam) {
                            if (done || data == null) return;
                            busy = true;
                            final int w = previewW, h = previewH;
                            try {
                                decoder.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        final String text = QrDecoder.decode(data, w, h);
                                        main.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                busy = false;
                                                onText(text);
                                                if (!done) requestFrame(120);
                                            }
                                        });
                                    }
                                });
                            } catch (RuntimeException e) {
                                busy = false;           // the screen is closing
                            }
                        }
                    });
                } catch (RuntimeException ignored) {
                    // the camera was released
                }
            }
        }, delayMs);
    }

    private void onText(String text) {
        if (text == null || done) return;
        String base = PairLink.parse(text);
        if (base == null) {
            hint.setText(R.string.scan_bad);
            return;
        }
        done = true;
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_BASE, base));
        finish();
    }

    /** A dim frame around a square, so the person knows where to hold the code. */
    private static final class Overlay extends View {
        private final Paint dim = new Paint();
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        Overlay(Context c) {
            super(c);
            dim.setColor(0x66000000);
            line.setColor(Color.WHITE);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(6);
            line.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas cv) {
            int w = getWidth(), h = getHeight();
            int side = Math.min(w, h) * 6 / 10;
            int l = (w - side) / 2, t = (h - side) / 2 - h / 14, r = l + side, b = t + side;
            cv.drawRect(0, 0, w, t, dim);
            cv.drawRect(0, b, w, h, dim);
            cv.drawRect(0, t, l, b, dim);
            cv.drawRect(r, t, w, b, dim);
            int k = side / 6;
            path.reset();
            path.moveTo(l, t + k); path.lineTo(l, t); path.lineTo(l + k, t);
            path.moveTo(r - k, t); path.lineTo(r, t); path.lineTo(r, t + k);
            path.moveTo(r, b - k); path.lineTo(r, b); path.lineTo(r - k, b);
            path.moveTo(l + k, b); path.lineTo(l, b); path.lineTo(l, b - k);
            cv.drawPath(path, line);
        }
    }
}
