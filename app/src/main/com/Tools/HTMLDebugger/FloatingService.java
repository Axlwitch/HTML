package com.Tools.HTMLDebugger;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import android.webkit.*;
import android.webkit.JavascriptInterface;
import android.util.Base64;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class FloatingService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private WebView floatWebView;
    private LinearLayout lnrMainContent;
    private LinearLayout lnrHeader;
    private LinearLayout rootLayout;
    private boolean isMinimized = false;
    private boolean isFullWindow = false;
    private boolean isRotated = false;
    private String pathHtmlDijalankan = "";
    private Spinner spinnerPilihApp;
    private ArrayList<String> namaAplikasi = new ArrayList<String>();
    private ArrayList<String> pathIndexHtml = new ArrayList<String>();
    private File folderUtama;
    private boolean isAwalDimuat = true;
    private UDPManager udpManager;
    private GameOptimizer gameOptimizer;
    private StorageSupport storageSupport;
    private int originalWidth;
    private int originalHeight;
    private int originalX;
    private int originalY;
    private WebAppInterface webAppInterface;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_layout, null);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= 26) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        int layarWidth = getResources().getDisplayMetrics().widthPixels;
        int layarHeight = getResources().getDisplayMetrics().heightPixels;

        int widthPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 380, getResources().getDisplayMetrics());
        int heightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 520, getResources().getDisplayMetrics());

        originalWidth = widthPx;
        originalHeight = heightPx;
        originalX = 80;
        originalY = 80;

        params = new WindowManager.LayoutParams(
            widthPx,
            heightPx,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = originalX;
        params.y = originalY;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);

        // INISIALISASI VIEW
        rootLayout = (LinearLayout) floatingView.findViewById(R.id.rootLayout);
        floatWebView = (WebView) floatingView.findViewById(R.id.floatWebView);
        lnrMainContent = (LinearLayout) floatingView.findViewById(R.id.lnrMainContent);
        lnrHeader = (LinearLayout) floatingView.findViewById(R.id.lnrHeader);
        spinnerPilihApp = (Spinner) floatingView.findViewById(R.id.spinnerPilihApp);
        TextView txtHeaderDrag = (TextView) floatingView.findViewById(R.id.txtHeaderDrag);
        final Button btnMinimizeApp = (Button) floatingView.findViewById(R.id.btnMinimizeApp);
        Button btnCloseApp = (Button) floatingView.findViewById(R.id.btnCloseApp);
        Button btnFullWindow = (Button) floatingView.findViewById(R.id.btnFullWindow);
        final Button btnRotate = (Button) floatingView.findViewById(R.id.btnRotate);

        floatWebView.setFocusable(true);
        floatWebView.setFocusableInTouchMode(true);

        udpManager = new UDPManager(this, floatWebView);
        gameOptimizer = new GameOptimizer(this, floatWebView);
        storageSupport = new StorageSupport(this);
        webAppInterface = new WebAppInterface();

        storageSupport.setDownloadCallback(new StorageSupport.DownloadCallback() {
                @Override
                public void onProgress(final String jsonData) {
                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (floatWebView != null) {
                                    String js = "if (typeof window.downloadCallback === 'function') { window.downloadCallback(" + jsonData + "); }";
                                    floatWebView.loadUrl("javascript:" + js);
                                }
                            }
                        });
                }

                @Override
                public void onComplete(final String taskId, final String filePath) {
                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (floatWebView != null) {
                                    String js = "if (typeof window.downloadComplete === 'function') { window.downloadComplete('" + taskId + "', '" + filePath.replace("'", "\\'") + "'); }";
                                    floatWebView.loadUrl("javascript:" + js);
                                }
                            }
                        });
                }

                @Override
                public void onError(final String taskId, final String error) {
                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (floatWebView != null) {
                                    String js = "if (typeof window.downloadError === 'function') { window.downloadError('" + taskId + "', '" + error.replace("'", "\\'") + "'); }";
                                    floatWebView.loadUrl("javascript:" + js);
                                }
                            }
                        });
                }
            });

        // WEBSETTINGS
        WebSettings webSettings = floatWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setLoadWithOverviewMode(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(getCacheDir().getAbsolutePath());
        webSettings.setDatabaseEnabled(false);

        if (Build.VERSION.SDK_INT >= 19) {
            floatWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            WebView.setWebContentsDebuggingEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= 21) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
            webSettings.setEnableSmoothTransition(false);
        }

        if (Build.VERSION.SDK_INT >= 23) {
            webSettings.setMediaPlaybackRequiresUserGesture(false);
        }

        floatWebView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        floatWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        floatWebView.setFadingEdgeLength(0);

        // ============================================
        // 1. MODERN CSS FIX - WEBVIEW CLIENT MODERN
        // ============================================
        floatWebView.setWebViewClient(new ModernWebViewClient());

        floatWebView.addJavascriptInterface(storageSupport, "Android");
        floatWebView.addJavascriptInterface(udpManager, "UDP");
        floatWebView.addJavascriptInterface(webAppInterface, "WebApp");

        // ============================================
        // ONTOUCH WEBSITE - HAPUS FLAG UNTUK KEYBOARD
        // ============================================
        floatWebView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                        windowManager.updateViewLayout(floatingView, params);

                        floatWebView.requestFocus();
                        floatWebView.setFocusable(true);
                        floatWebView.setFocusableInTouchMode(true);
                    }
                    return false;
                }
            });

        // ============================================
        // HEADER DRAG - KEMBALIKAN FLAG SAAT DRAG
        // ============================================
        txtHeaderDrag.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                            windowManager.updateViewLayout(floatingView, params);

                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);

                            int layarW = getResources().getDisplayMetrics().widthPixels;
                            int layarH = getResources().getDisplayMetrics().heightPixels;

                            if (params.x < 0) params.x = 0;
                            if (params.y < 0) params.y = 0;
                            if (params.x + params.width > layarW) params.x = layarW - params.width;
                            if (params.y + params.height > layarH) params.y = layarH - params.height;

                            windowManager.updateViewLayout(floatingView, params);
                            return true;
                    }
                    return false;
                }
            });

        // TOMBOL ROTATE
        btnRotate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleRotateWindow();
                }
            });

        // MINIMIZE BUTTON
        btnMinimizeApp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isMinimized) {
                        lnrMainContent.setVisibility(View.GONE);
                        floatWebView.setVisibility(View.GONE);
                        spinnerPilihApp.setVisibility(View.GONE);

                        params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 180, getResources().getDisplayMetrics());
                        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42, getResources().getDisplayMetrics());
                        params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                        btnMinimizeApp.setText("[]");
                        isMinimized = true;
                    } else {
                        lnrMainContent.setVisibility(View.VISIBLE);
                        floatWebView.setVisibility(View.VISIBLE);
                        spinnerPilihApp.setVisibility(View.VISIBLE);

                        restoreWindowSize();
                        btnMinimizeApp.setText("-");
                        isMinimized = false;
                    }
                    windowManager.updateViewLayout(floatingView, params);
                }
            });

        // CLOSE BUTTON
        btnCloseApp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopSelf();
                }
            });

        // FULL WINDOW BUTTON
        btnFullWindow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFullWindow();
                }
            });

        // FOLDER & SCAN
        folderUtama = new File(Environment.getExternalStorageDirectory(), "HTMLTools");
        if (!folderUtama.exists()) {
            folderUtama = new File("/storage/emulated/0/HTMLTools/");
        }
        if (!folderUtama.exists()) {
            folderUtama.mkdirs();
        }
        scanDaftarProgramRealtime();

        if (gameOptimizer != null) {
            gameOptimizer.enableGameMode();
        }
    }

    // ============================================
    // WebAppInterface Class
    // ============================================
    public class WebAppInterface {

		@JavascriptInterface
		public void writeFile(String filePath, String content) {
			if (filePath == null || content == null) {
				return;
			}

			File file = new File(filePath);
			File parentDir = file.getParentFile();

			if (parentDir != null && !parentDir.exists()) {
				parentDir.mkdirs();
			}

			if (filePath.endsWith(".png")) {
				FileOutputStream fos = null;
				try {
					if (content.contains(",")) {
						content = content.substring(content.indexOf(",") + 1);
					}
					byte[] decodedBytes = Base64.decode(content, Base64.DEFAULT);
					fos = new FileOutputStream(file, false);
					fos.write(decodedBytes);
					fos.flush();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (fos != null) {
						try {
							fos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} else {
				FileWriter writer = null;
				try {
					writer = new FileWriter(file, false);
					writer.write(content);
					writer.flush();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (writer != null) {
						try {
							writer.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		@JavascriptInterface
		public String readFileBase64(String path) {
			if (path == null) {
				return "";
			}

			File file = new File(path);
			if (!file.exists() || !file.canRead()) {
				return "";
			}

			BufferedInputStream buf = null;
			try {
				int size = (int) file.length();
				byte[] bytes = new byte[size];

				buf = new BufferedInputStream(new FileInputStream(file));

				int totalBytesRead = 0;
				while (totalBytesRead < size) {
					int bytesRead = buf.read(bytes, totalBytesRead, size - totalBytesRead);
					if (bytesRead == -1) {
						break;
					}
					totalBytesRead += bytesRead;
				}

				return Base64.encodeToString(bytes, Base64.NO_WRAP);
			} catch (Exception e) {
				e.printStackTrace();
				return "";
			} finally {
				if (buf != null) {
					try {
						buf.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	

    // ============================================
    // 1. MODERN CSS FIX - INJECT CSS MODERN
    // ============================================
    private void injectModernCSSFix(WebView view) {
        String cssFix = 
            "javascript:(function() {" +
            "   var style = document.createElement('style');" +
            "   style.type = 'text/css';" +
            "   style.textContent = '" +
            "       /* === MODERN CSS RESET === */" +
            "       *, *::before, *::after {" +
            "           -webkit-box-sizing: border-box !important;" +
            "           -moz-box-sizing: border-box !important;" +
            "           box-sizing: border-box !important;" +
            "           margin: 0;" +
            "           padding: 0;" +
            "       }" +
            "       " +
            "       /* === HTML5 SEMANTIC ELEMENTS === */" +
            "       article, aside, details, figcaption, figure, " +
            "       footer, header, hgroup, menu, nav, section, main, " +
            "       dialog, figure, mark, time, summary {" +
            "           display: block !important;" +
            "       }" +
            "       " +
            "       /* === MODERN FLEXBOX SUPPORT === */" +
            "       .flex, .row, .columns, [class*='flex'] {" +
            "           display: -webkit-box !important;" +
            "           display: -moz-box !important;" +
            "           display: -ms-flexbox !important;" +
            "           display: -webkit-flex !important;" +
            "           display: flex !important;" +
            "       }" +
            "       " +
            "       .inline-flex, [class*='inline-flex'] {" +
            "           display: -webkit-inline-box !important;" +
            "           display: -moz-inline-box !important;" +
            "           display: -ms-inline-flexbox !important;" +
            "           display: -webkit-inline-flex !important;" +
            "           display: inline-flex !important;" +
            "       }" +
            "       " +
            "       /* === FLEX PROPERTIES === */" +
            "       .flex-1 { -webkit-flex: 1 !important; -ms-flex: 1 !important; flex: 1 !important; }" +
            "       .flex-2 { -webkit-flex: 2 !important; -ms-flex: 2 !important; flex: 2 !important; }" +
            "       .flex-3 { -webkit-flex: 3 !important; -ms-flex: 3 !important; flex: 3 !important; }" +
            "       .flex-4 { -webkit-flex: 4 !important; -ms-flex: 4 !important; flex: 4 !important; }" +
            "       .flex-5 { -webkit-flex: 5 !important; -ms-flex: 5 !important; flex: 5 !important; }" +
            "       " +
            "       .flex-row { -webkit-flex-direction: row !important; -ms-flex-direction: row !important; flex-direction: row !important; }" +
            "       .flex-column { -webkit-flex-direction: column !important; -ms-flex-direction: column !important; flex-direction: column !important; }" +
            "       .flex-wrap { -webkit-flex-wrap: wrap !important; -ms-flex-wrap: wrap !important; flex-wrap: wrap !important; }" +
            "       .flex-nowrap { -webkit-flex-wrap: nowrap !important; -ms-flex-wrap: nowrap !important; flex-wrap: nowrap !important; }" +
            "       " +
            "       .justify-start { -webkit-justify-content: flex-start !important; -ms-flex-pack: start !important; justify-content: flex-start !important; }" +
            "       .justify-center { -webkit-justify-content: center !important; -ms-flex-pack: center !important; justify-content: center !important; }" +
            "       .justify-end { -webkit-justify-content: flex-end !important; -ms-flex-pack: end !important; justify-content: flex-end !important; }" +
            "       .justify-between { -webkit-justify-content: space-between !important; -ms-flex-pack: justify !important; justify-content: space-between !important; }" +
            "       .justify-around { -webkit-justify-content: space-around !important; -ms-flex-pack: distribute !important; justify-content: space-around !important; }" +
            "       " +
            "       .items-start { -webkit-align-items: flex-start !important; -ms-flex-align: start !important; align-items: flex-start !important; }" +
            "       .items-center { -webkit-align-items: center !important; -ms-flex-align: center !important; align-items: center !important; }" +
            "       .items-end { -webkit-align-items: flex-end !important; -ms-flex-align: end !important; align-items: flex-end !important; }" +
            "       .items-stretch { -webkit-align-items: stretch !important; -ms-flex-align: stretch !important; align-items: stretch !important; }" +
            "       " +
            "       .self-start { -webkit-align-self: flex-start !important; -ms-flex-item-align: start !important; align-self: flex-start !important; }" +
            "       .self-center { -webkit-align-self: center !important; -ms-flex-item-align: center !important; align-self: center !important; }" +
            "       .self-end { -webkit-align-self: flex-end !important; -ms-flex-item-align: end !important; align-self: flex-end !important; }" +
            "       .self-stretch { -webkit-align-self: stretch !important; -ms-flex-item-align: stretch !important; align-self: stretch !important; }" +
            "       " +
            "       /* === CSS GRID SUPPORT === */" +
            "       .grid, [class*='grid'] {" +
            "           display: -ms-grid !important;" +
            "           display: grid !important;" +
            "       }" +
            "       " +
            "       .inline-grid, [class*='inline-grid'] {" +
            "           display: -ms-inline-grid !important;" +
            "           display: inline-grid !important;" +
            "       }" +
            "       " +
            "       /* === CSS VARIABLES FALLBACK === */" +
            "       :root {" +
            "           --primary: #007bff;" +
            "           --secondary: #6c757d;" +
            "           --success: #28a745;" +
            "           --danger: #dc3545;" +
            "           --warning: #ffc107;" +
            "           --info: #17a2b8;" +
            "           --light: #f8f9fa;" +
            "           --dark: #343a40;" +
            "       }" +
            "       " +
            "       /* === MODERN FONT RENDERING === */" +
            "       body {" +
            "           font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif !important;" +
            "           -webkit-font-smoothing: antialiased !important;" +
            "           -moz-osx-font-smoothing: grayscale !important;" +
            "           text-rendering: optimizeLegibility !important;" +
            "           line-height: 1.6 !important;" +
            "       }" +
            "       " +
            "       /* === MODERN SCROLLING === */" +
            "       html {" +
            "           scroll-behavior: smooth !important;" +
            "       }" +
            "       body {" +
            "           overflow-y: auto !important;" +
            "           -webkit-overflow-scrolling: touch !important;" +
            "           height: 100% !important;" +
            "       }" +
            "       " +
            "       /* === MODERN BUTTONS & INPUTS === */" +
            "       input, button, select, textarea {" +
            "           font-family: inherit !important;" +
            "           font-size: inherit !important;" +
            "           line-height: inherit !important;" +
            "           -webkit-appearance: none !important;" +
            "           appearance: none !important;" +
            "       }" +
            "       " +
            "       input:focus, button:focus, select:focus, textarea:focus {" +
            "           outline: 2px solid #007bff !important;" +
            "           outline-offset: 2px !important;" +
            "       }" +
            "       " +
            "       /* === MODERN IMAGES === */" +
            "       img {" +
            "           max-width: 100% !important;" +
            "           height: auto !important;" +
            "           display: block !important;" +
            "           image-rendering: auto !important;" +
            "       }" +
            "       " +
            "       /* === MODERN SCROLLBAR === */" +
            "       ::-webkit-scrollbar {" +
            "           width: 8px !important;" +
            "           height: 8px !important;" +
            "       }" +
            "       ::-webkit-scrollbar-track {" +
            "           background: #f1f1f1 !important;" +
            "           border-radius: 4px !important;" +
            "       }" +
            "       ::-webkit-scrollbar-thumb {" +
            "           background: #c1c1c1 !important;" +
            "           border-radius: 4px !important;" +
            "       }" +
            "       ::-webkit-scrollbar-thumb:hover {" +
            "           background: #a8a8a8 !important;" +
            "       }" +
            "       " +
            "       /* === GPU ACCELERATION === */" +
            "       .gpu, .accelerated, [class*='gpu'] {" +
            "           -webkit-transform: translateZ(0) !important;" +
            "           -moz-transform: translateZ(0) !important;" +
            "           -ms-transform: translateZ(0) !important;" +
            "           -o-transform: translateZ(0) !important;" +
            "           transform: translateZ(0) !important;" +
            "           will-change: transform, opacity !important;" +
            "       }" +
            "       " +
            "       /* === CSS TRANSITIONS === */" +
            "       .transition, [class*='transition'] {" +
            "           -webkit-transition: all 0.3s ease !important;" +
            "           -moz-transition: all 0.3s ease !important;" +
            "           -ms-transition: all 0.3s ease !important;" +
            "           -o-transition: all 0.3s ease !important;" +
            "           transition: all 0.3s ease !important;" +
            "       }" +
            "       " +
            "       /* === CSS ANIMATIONS === */" +
            "       .animated, [class*='animated'] {" +
            "           -webkit-animation-duration: 1s !important;" +
            "           -moz-animation-duration: 1s !important;" +
            "           -ms-animation-duration: 1s !important;" +
            "           -o-animation-duration: 1s !important;" +
            "           animation-duration: 1s !important;" +
            "       }" +
            "       " +
            "       .fade-in {" +
            "           -webkit-animation: fadeIn 0.3s ease-in !important;" +
            "           -moz-animation: fadeIn 0.3s ease-in !important;" +
            "           -ms-animation: fadeIn 0.3s ease-in !important;" +
            "           -o-animation: fadeIn 0.3s ease-in !important;" +
            "           animation: fadeIn 0.3s ease-in !important;" +
            "       }" +
            "       " +
            "       @-webkit-keyframes fadeIn {" +
            "           from { opacity: 0; }" +
            "           to { opacity: 1; }" +
            "       }" +
            "       @-moz-keyframes fadeIn {" +
            "           from { opacity: 0; }" +
            "           to { opacity: 1; }" +
            "       }" +
            "       @-ms-keyframes fadeIn {" +
            "           from { opacity: 0; }" +
            "           to { opacity: 1; }" +
            "       }" +
            "       @-o-keyframes fadeIn {" +
            "           from { opacity: 0; }" +
            "           to { opacity: 1; }" +
            "       }" +
            "       @keyframes fadeIn {" +
            "           from { opacity: 0; }" +
            "           to { opacity: 1; }" +
            "       }" +
            "   ';" +
            "   document.head.appendChild(style);" +
            "   console.log('Modern CSS Fix injected');" +
            "})()";

        view.loadUrl(cssFix);
    }

    // ============================================
    // 2. JAVASCRIPT MODERN SUPPORT - ES6+ POLYFILLS
    // ============================================
    private void injectModernJavaScript(WebView view) {
        String modernJS = 
            "javascript:(function() {" +
            "   // === 2. JAVASCRIPT MODERN SUPPORT - ES6+ POLYFILLS === " +
            "   " +
            "   // 2.1 Promise Support" +
            "   if (typeof Promise === 'undefined') {" +
            "       window.Promise = function(executor) {" +
            "           var self = this;" +
            "           self._callbacks = [];" +
            "           self._state = 'pending';" +
            "           self._value = null;" +
            "           " +
            "           function resolve(value) {" +
            "               if (self._state === 'pending') {" +
            "                   self._state = 'fulfilled';" +
            "                   self._value = value;" +
            "                   var callbacks = self._callbacks.slice(0);" +
            "                   for (var i = 0; i < callbacks.length; i++) {" +
            "                       try { callbacks[i].onFulfilled(value); } catch(e) {}" +
            "                   }" +
            "               }" +
            "           }" +
            "           " +
            "           function reject(reason) {" +
            "               if (self._state === 'pending') {" +
            "                   self._state = 'rejected';" +
            "                   self._value = reason;" +
            "                   var callbacks = self._callbacks.slice(0);" +
            "                   for (var i = 0; i < callbacks.length; i++) {" +
            "                       try { callbacks[i].onRejected(reason); } catch(e) {}" +
            "                   }" +
            "               }" +
            "           }" +
            "           " +
            "           try { executor(resolve, reject); } catch(e) { reject(e); }" +
            "       };" +
            "       " +
            "       Promise.prototype.then = function(onFulfilled, onRejected) {" +
            "           var self = this;" +
            "           return new Promise(function(resolve, reject) {" +
            "               self._callbacks.push({" +
            "                   onFulfilled: function(value) {" +
            "                       try {" +
            "                           var result = onFulfilled ? onFulfilled(value) : value;" +
            "                           resolve(result);" +
            "                       } catch(e) {" +
            "                           reject(e);" +
            "                       }" +
            "                   }," +
            "                   onRejected: function(reason) {" +
            "                       try {" +
            "                           var result = onRejected ? onRejected(reason) : reason;" +
            "                           reject(result);" +
            "                       } catch(e) {" +
            "                           reject(e);" +
            "                       }" +
            "                   }" +
            "               });" +
            "           });" +
            "       };" +
            "       " +
            "       Promise.prototype.catch = function(onRejected) {" +
            "           return this.then(null, onRejected);" +
            "       };" +
            "   }" +
            "   " +
            "   // 2.2 fetch API" +
            "   if (typeof fetch === 'undefined') {" +
            "       window.fetch = function(url, options) {" +
            "           return new Promise(function(resolve, reject) {" +
            "               var xhr = new XMLHttpRequest();" +
            "               var method = options && options.method ? options.method : 'GET';" +
            "               xhr.open(method, url);" +
            "               " +
            "               if (options && options.headers) {" +
            "                   var headerKeys = Object.keys(options.headers);" +
            "                   for (var i = 0; i < headerKeys.length; i++) {" +
            "                       var key = headerKeys[i];" +
            "                       xhr.setRequestHeader(key, options.headers[key]);" +
            "                   }" +
            "               }" +
            "               " +
            "               xhr.onload = function() {" +
            "                   var response = {" +
            "                       ok: xhr.status >= 200 && xhr.status < 300," +
            "                       status: xhr.status," +
            "                       statusText: xhr.statusText," +
            "                       headers: {" +
            "                           get: function(name) {" +
            "                               return xhr.getResponseHeader(name);" +
            "                           }" +
            "                       }," +
            "                       text: function() { return Promise.resolve(xhr.responseText); }," +
            "                       json: function() { return Promise.resolve(JSON.parse(xhr.responseText)); }," +
            "                       blob: function() { return Promise.resolve(new Blob([xhr.response])); }," +
            "                       arrayBuffer: function() { return Promise.resolve(xhr.response); }" +
            "                   };" +
            "                   resolve(response);" +
            "               };" +
            "               " +
            "               xhr.onerror = function() {" +
            "                   reject(new Error('Network request failed'));" +
            "               };" +
            "               " +
            "               xhr.ontimeout = function() {" +
            "                   reject(new Error('Request timeout'));" +
            "               };" +
            "               " +
            "               if (options && options.timeout) {" +
            "                   xhr.timeout = options.timeout;" +
            "               }" +
            "               " +
            "               xhr.send(options && options.body ? options.body : null);" +
            "           });" +
            "       };" +
            "   }" +
            "   " +
            "   // 2.3 Intersection Observer" +
            "   if (typeof IntersectionObserver === 'undefined') {" +
            "       window.IntersectionObserver = function(callback, options) {" +
            "           this.callback = callback;" +
            "           this.options = options || {};" +
            "           this.observed = [];" +
            "           this.timeoutId = null;" +
            "           " +
            "           this.observe = function(element) {" +
            "               this.observed.push(element);" +
            "               var self = this;" +
            "               if (this.timeoutId) {" +
            "                   clearTimeout(this.timeoutId);" +
            "               }" +
            "               this.timeoutId = setTimeout(function() {" +
            "                   var entries = [];" +
            "                   for (var i = 0; i < self.observed.length; i++) {" +
            "                       var el = self.observed[i];" +
            "                       var rect = el.getBoundingClientRect();" +
            "                       var isIntersecting = rect.top < window.innerHeight && rect.bottom > 0;" +
            "                       entries.push({" +
            "                           target: el," +
            "                           isIntersecting: isIntersecting," +
            "                           intersectionRatio: isIntersecting ? 1 : 0," +
            "                           boundingClientRect: rect," +
            "                           intersectionRect: rect" +
            "                       });" +
            "                   }" +
            "                   self.callback(entries, self);" +
            "               }, 100);" +
            "           };" +
            "           " +
            "           this.unobserve = function(element) {" +
            "               var index = this.observed.indexOf(element);" +
            "               if (index > -1) {" +
            "                   this.observed.splice(index, 1);" +
            "               }" +
            "           };" +
            "           " +
            "           this.disconnect = function() {" +
            "               this.observed = [];" +
            "               if (this.timeoutId) {" +
            "                   clearTimeout(this.timeoutId);" +
            "               }" +
            "           };" +
            "       };" +
            "   }" +
            "   " +
            "   // 2.4 Custom Elements" +
            "   if (typeof customElements === 'undefined') {" +
            "       window.customElements = {" +
            "           define: function(name, constructor) {" +
            "               console.log('Custom element defined: ' + name);" +
            "           }," +
            "           get: function(name) {" +
            "               return null;" +
            "           }," +
            "           whenDefined: function(name) {" +
            "               return Promise.resolve();" +
            "           }" +
            "       };" +
            "   }" +
            "   " +
            "   // 2.5 localStorage" +
            "   if (typeof localStorage === 'undefined') {" +
            "       window.localStorage = {" +
            "           _data: {}," +
            "           getItem: function(key) {" +
            "               var value = this._data[key];" +
            "               return value !== undefined ? value : null;" +
            "           }," +
            "           setItem: function(key, value) {" +
            "               this._data[key] = String(value);" +
            "           }," +
            "           removeItem: function(key) {" +
            "               delete this._data[key];" +
            "           }," +
            "           clear: function() {" +
            "               this._data = {};" +
            "           }," +
            "           key: function(index) {" +
            "               var keys = Object.keys(this._data);" +
            "               return keys[index] || null;" +
            "           }," +
            "           get length() {" +
            "               return Object.keys(this._data).length;" +
            "           }" +
            "       };" +
            "   }" +
            "   " +
            "   // 2.6 sessionStorage" +
            "   if (typeof sessionStorage === 'undefined') {" +
            "       window.sessionStorage = {" +
            "           _data: {}," +
            "           getItem: function(key) {" +
            "               var value = this._data[key];" +
            "               return value !== undefined ? value : null;" +
            "           }," +
            "           setItem: function(key, value) {" +
            "               this._data[key] = String(value);" +
            "           }," +
            "           removeItem: function(key) {" +
            "               delete this._data[key];" +
            "           }," +
            "           clear: function() {" +
            "               this._data = {};" +
            "           }," +
            "           key: function(index) {" +
            "               var keys = Object.keys(this._data);" +
            "               return keys[index] || null;" +
            "           }," +
            "           get length() {" +
            "               return Object.keys(this._data).length;" +
            "           }" +
            "       };" +
            "   }" +
            "   " +
            "   // 2.7 Array.from" +
            "   if (!Array.from) {" +
            "       Array.from = function(arrayLike) {" +
            "           return Array.prototype.slice.call(arrayLike);" +
            "       };" +
            "   }" +
            "   " +
            "   // 2.8 Array.includes" +
            "   if (!Array.prototype.includes) {" +
            "       Array.prototype.includes = function(searchElement, fromIndex) {" +
            "           return this.indexOf(searchElement, fromIndex || 0) !== -1;" +
            "       };" +
            "   }" +
            "   " +
            "   // 2.9 String.includes" +
            "   if (!String.prototype.includes) {" +
            "       String.prototype.includes = function(search, start) {" +
            "           return this.indexOf(search, start || 0) !== -1;" +
            "       };" +
            "   }" +
            "   " +
            "   // 2.10 String.startsWith" +
            "   if (!String.prototype.startsWith) {" +
            "       String.prototype.startsWith = function(search, pos) {" +
            "           pos = pos || 0;" +
            "           return this.substr(pos, search.length) === search;" +
            "       };" +
            "   }" +
            "   " +
            "   // 2.11 String.endsWith" +
            "   if (!String.prototype.endsWith) {" +
            "       String.prototype.endsWith = function(search, this_len) {" +
            "           if (this_len === undefined || this_len > this.length) {" +
            "               this_len = this.length;" +
            "           }" +
            "           return this.substring(this_len - search.length, this_len) === search;" +
            "       };" +
            "   }" +
            "   " +
            "   // 2.12 Object.assign" +
            "   if (!Object.assign) {" +
            "       Object.assign = function(target) {" +
            "           for (var i = 1; i < arguments.length; i++) {" +
            "               var source = arguments[i];" +
            "               if (source) {" +
            "                   var keys = Object.keys(source);" +
            "                   for (var j = 0; j < keys.length; j++) {" +
            "                       var key = keys[j];" +
            "                       if (Object.prototype.hasOwnProperty.call(source, key)) {" +
            "                           target[key] = source[key];" +
            "                       }" +
            "                   }" +
            "               }" +
            "           }" +
            "           return target;" +
            "       };" +
            "   }" +
            "   " +
            "   // 2.13 console.log dengan debug mode" +
            "   var originalConsole = window.console;" +
            "   window.console = {" +
            "       log: function() {" +
            "           if (window.__debug) {" +
            "               originalConsole.log.apply(originalConsole, arguments);" +
            "           }" +
            "       }," +
            "       warn: function() {" +
            "           originalConsole.warn.apply(originalConsole, arguments);" +
            "       }," +
            "       error: function() {" +
            "           originalConsole.error.apply(originalConsole, arguments);" +
            "       }," +
            "       info: function() {" +
            "           if (window.__debug) {" +
            "               originalConsole.info.apply(originalConsole, arguments);" +
            "           }" +
            "       }," +
            "       debug: function() {" +
            "           if (window.__debug) {" +
            "               originalConsole.debug.apply(originalConsole, arguments);" +
            "           }" +
            "       }" +
            "   };" +
            "   " +
            "   // 2.14 Performance API" +
            "   if (typeof performance === 'undefined') {" +
            "       window.performance = {" +
            "           now: function() {" +
            "               return Date.now();" +
            "           }," +
            "           mark: function(name) {" +
            "               console.log('Performance mark: ' + name);" +
            "           }," +
            "           measure: function(name) {" +
            "               console.log('Performance measure: ' + name);" +
            "           }," +
            "           getEntriesByType: function(type) {" +
            "               return [];" +
            "           }" +
            "       };" +
            "   }" +
            "   " +
            "   // 2.15 navigator.language" +
            "   if (typeof navigator.language === 'undefined') {" +
            "       navigator.language = 'en-US';" +
            "   }" +
            "   " +
            "   // 2.16 navigator.userAgent" +
            "   if (typeof navigator.userAgent === 'undefined') {" +
            "       navigator.userAgent = 'Mozilla/5.0 (Linux; Android) AppleWebKit/537.36';" +
            "   }" +
            "   " +
            "   console.log('Modern JavaScript ES6+ polyfills injected');" +
            "})()";

        view.loadUrl(modernJS);
    }

    // ============================================
    // 3. INJECT TRANSLATOR HOOK (CANVAS 2D)
    // ============================================
	private void injectTextTranslator(WebView view) {
		String translatorJS = 
			"javascript:(function() {" +
			"   if (window.__textHookInjected) {" +
			"       if (window.__translator && typeof window.__translator.reload === 'function') window.__translator.reload();" +
			"       return;" +
			"   }" +
			"   window.__textHookInjected = true;" +
			"   " +
			"   var pathBase = '';" +
			"   var pathDump = '';" +
			"   var pathTranslate = '';" +
			"   " +
			"   var kamusGame = {};" +
			"   var autoSaveEnabled = true;" +
			"   " +
			"   var dumpPending = {};" +
			"   var hasUnsavedDump = false;" +
			"   " +
			"   function initPaths() {" +
			"       if (typeof Android !== 'undefined' && typeof Android.getBasePath === 'function') {" +
			"           pathBase = Android.getBasePath();" +
			"       } else {" +
			"           pathBase = '/storage/emulated/0/HTMLTools';" +
			"       }" +
			"       pathDump = pathBase + '/dump.json';" +
			"       pathTranslate = pathBase + '/translate.json';" +
			"   }" +
			"   " +
			"   function muatSemuaKamus() {" +
			"       initPaths();" +
			"       kamusGame = {};" +
			"       " +
			"       try {" +
			"           if (typeof Android !== 'undefined' && typeof Android.fileExists === 'function') {" +
			"               if (Android.fileExists(pathTranslate)) {" +
			"                   var isi = Android.readFile(pathTranslate);" +
			"                   if (isi && isi.trim() !== '') kamusGame = JSON.parse(isi);" +
			"               }" +
			"           }" +
			"       } catch(e) {}" +
			"   }" +
			"   " +
			"   function paksaSaveDump() {" +
			"       if (!autoSaveEnabled || !hasUnsavedDump) return;" +
			"       try {" +
			"           if (typeof WebApp !== 'undefined' && typeof WebApp.writeFile === 'function') {" +
			"               var existing = {};" +
			"               if (typeof Android !== 'undefined' && typeof Android.fileExists === 'function' && Android.fileExists(pathDump)) {" +
			"                   var isi = Android.readFile(pathDump);" +
			"                   if (isi && isi.trim() !== '') {" +
			"                       try { existing = JSON.parse(isi); } catch(err) { existing = {}; }" +
			"                   }" +
			"               }" +
			"               " +
			"               Object.assign(existing, dumpPending);" +
			"               WebApp.writeFile(pathDump, JSON.stringify(existing, null, 2));" +
			"               " +
			"               dumpPending = {};" +
			"               hasUnsavedDump = false;" +
			"           }" +
			"       } catch(e) {}" +
			"   }" +
			"   " +
			"   if (window.__dumpTimer) clearInterval(window.__dumpTimer);" +
			"   window.__dumpTimer = setInterval(paksaSaveDump, 500);" +
			"   " +
			"   window.addEventListener('beforeunload', paksaSaveDump);" +
			"   document.addEventListener('visibilitychange', function() {" +
			"       if (document.visibilityState === 'hidden') paksaSaveDump();" +
			"   });" +
			"   " +
			"   function prosesText(teks) {" +
			"       if (!teks || typeof teks !== 'string') return teks;" +
			"       var trimmed = teks.trim();" +
			"       " +
			"       if (trimmed === '' || trimmed.length <= 1 || /^\\d+$/.test(trimmed)) return teks;" +
			"       " +
			"       if (kamusGame[trimmed] !== undefined) {" +
			"           if (dumpPending[trimmed]) delete dumpPending[trimmed];" +
			"           return teks.replace(trimmed, kamusGame[trimmed]);" +
			"       }" +
			"       " +
			"       for (var k in kamusGame) {" +
			"           if (kamusGame[k] === trimmed) return teks;" +
			"       }" +
			"       " +
			"       if (autoSaveEnabled) {" +
			"           for (var key in dumpPending) {" +
			"               if (trimmed.length > key.length && trimmed.indexOf(key) !== -1) {" +
			"                   delete dumpPending[key];" +
			"               }" +
			"           }" +
			"           " +
			"           var isSubText = false;" +
			"           for (var key in dumpPending) {" +
			"               if (key.length >= trimmed.length && key.indexOf(trimmed) !== -1) {" +
			"                   isSubText = true;" +
			"                   break;" +
			"               }" +
			"           }" +
			"           " +
			"           if (!isSubText) {" +
			"               dumpPending[trimmed] = trimmed;" +
			"               hasUnsavedDump = true;" +
			"           }" +
			"       }" +
			"       return teks;" +
			"   }" +
			"   " +
			"   if (typeof CanvasRenderingContext2D !== 'undefined') {" +
			"       var origFill = CanvasRenderingContext2D.prototype.fillText;" +
			"       CanvasRenderingContext2D.prototype.fillText = function(t, x, y, m) {" +
			"           var tb = prosesText(String(t));" +
			"           var rx = Math.round(x);" +
			"           var ry = Math.round(y);" +
			"           return m !== undefined ? origFill.call(this, tb, rx, ry, m) : origFill.call(this, tb, rx, ry);" +
			"       };" +
			"   }" +
			"   " +
			"   if (typeof Native !== 'undefined' && typeof J2ME !== 'undefined') {" +
			"       var sigDrawString = 'javax/microedition/lcdui/Graphics.drawString.(Ljava/lang/String;III)V';" +
			"       if (typeof Native[sigDrawString] === 'function') {" +
			"           var origDS = Native[sigDrawString];" +
			"           Native[sigDrawString] = function(addr, strAddr, x, y, anchor) {" +
			"               var str = J2ME.fromStringAddr(strAddr);" +
			"               var teksBaru = prosesText(str);" +
			"               return (teksBaru !== str) ? origDS.call(this, addr, J2ME.newString(teksBaru), x, y, anchor) : origDS.call(this, addr, strAddr, x, y, anchor);" +
			"           };" +
			"       }" +
			"   }" +
			"   " +
			"   if (typeof window.drawString === 'function') {" +
			"       var origWDS = window.drawString;" +
			"       window.drawString = function(info, str, x, y, anchor) {" +
			"           if (!str || typeof str !== 'string') return origWDS.call(this, info, str, x, y, anchor);" +
			"           var teksBaru = prosesText(str);" +
			"           if (teksBaru.indexOf('\\n') !== -1) {" +
			"               var fontSize = 14;" +
			"               if (typeof NativeMap !== 'undefined' && info && info.currentFont) {" +
			"                   var fc = NativeMap.get(info.currentFont);" +
			"                   if (fc && fc.fontSize) fontSize = fc.fontSize;" +
			"               }" +
			"               var lh = fontSize * 1.15;" +
			"               var baris = teksBaru.split('\\n');" +
			"               for (var i = 0; i < baris.length; i++) {" +
			"                   origWDS.call(this, info, baris[i], Math.round(x), Math.round(y + (i * lh)), anchor);" +
			"               }" +
			"               return;" +
			"           }" +
			"           return origWDS.call(this, info, teksBaru, Math.round(x), Math.round(y), anchor);" +
			"       };" +
			"   }" +
			"   " +
			"   (function() {" +
			"       if (document.getElementById('__toggle_btn')) return;" +
			"       var btn = document.createElement('button');" +
			"       btn.id = '__toggle_btn'; btn.textContent = '💾 ON';" +
			"       btn.style.cssText = 'position:fixed!important;bottom:10px!important;right:60px!important;z-index:999999!important;background:rgba(0,0,0,0.85)!important;color:#00ff88!important;border:2px solid #00ff88!important;border-radius:8px!important;padding:6px 12px!important;font-size:12px!important;font-weight:bold!important;cursor:pointer!important;font-family:monospace!important;pointer-events:auto!important;box-shadow:0 0 20px rgba(0,255,136,0.3)!important';" +
			"       btn.onclick = function() {" +
			"           autoSaveEnabled = !autoSaveEnabled;" +
			"           btn.textContent = autoSaveEnabled ? '💾 ON' : '💾 OFF';" +
			"           btn.style.color = autoSaveEnabled ? '#00ff88' : '#ff4444';" +
			"           btn.style.borderColor = autoSaveEnabled ? '#00ff88' : '#ff4444';" +
			"       };" +
			"       (document.body || document.documentElement).appendChild(btn);" +
			"   })();" +
			"   " +
			"   muatSemuaKamus();" +
			"   window.__translator = {" +
			"       reload: function() { muatSemuaKamus(); }," +
			"       toggleSave: function() { autoSaveEnabled = !autoSaveEnabled; return autoSaveEnabled; }" +
			"   };" +
			"})()";

		view.loadUrl(translatorJS);
	}
	
	
	
	
	
	
    // ============================================
    // 4. MODERN POLYFILLS TAMBAHAN
    // ============================================
    private void injectModernPolyfills(WebView view) {
        String polyfills = 
            "javascript:(function() {" +
            "   // === 5. MODERN POLYFILLS TAMBAHAN === " +
            "   " +
            "   // 5.1 Array.find" +
            "   if (!Array.prototype.find) {" +
            "       Array.prototype.find = function(predicate) {" +
            "           for (var i = 0; i < this.length; i++) {" +
            "               if (predicate(this[i], i, this)) {" +
            "                   return this[i];" +
            "               }" +
            "           }" +
            "           return undefined;" +
            "       };" +
            "   }" +
            "   " +
            "   // 5.2 Array.findIndex" +
            "   if (!Array.prototype.findIndex) {" +
            "       Array.prototype.findIndex = function(predicate) {" +
            "           for (var i = 0; i < this.length; i++) {" +
            "               if (predicate(this[i], i, this)) {" +
            "                   return i;" +
            "               }" +
            "           }" +
            "           return -1;" +
            "       };" +
            "   }" +
            "   " +
            "   // 5.3 Array.fill" +
            "   if (!Array.prototype.fill) {" +
            "       Array.prototype.fill = function(value, start, end) {" +
            "           start = start || 0;" +
            "           end = end || this.length;" +
            "           for (var i = start; i < end; i++) {" +
            "               this[i] = value;" +
            "           }" +
            "           return this;" +
            "       };" +
            "   }" +
            "   " +
            "   // 5.4 Number.isInteger" +
            "   if (!Number.isInteger) {" +
            "       Number.isInteger = function(value) {" +
            "           return typeof value === 'number' && isFinite(value) && Math.floor(value) === value;" +
            "       };" +
            "   }" +
            "   " +
            "   // 5.5 Number.isNaN" +
            "   if (!Number.isNaN) {" +
            "       Number.isNaN = function(value) {" +
            "           return typeof value === 'number' && isNaN(value);" +
            "       };" +
            "   }" +
            "   " +
            "   // 5.6 Math.sign" +
            "   if (!Math.sign) {" +
            "       Math.sign = function(x) {" +
            "           return (x > 0) ? 1 : (x < 0) ? -1 : 0;" +
            "       };" +
            "   }" +
            "   " +
            "   // 5.7 Object.keys" +
            "   if (!Object.keys) {" +
            "       Object.keys = function(object) {" +
            "           var keys = [];" +
            "           for (var key in object) {" +
            "               if (Object.prototype.hasOwnProperty.call(object, key)) {" +
            "                   keys.push(key);" +
            "               }" +
            "           }" +
            "           return keys;" +
            "       };" +
            "   }" +
            "   " +
            "   // 5.8 Object.values" +
            "   if (!Object.values) {" +
            "       Object.values = function(object) {" +
            "           var values = [];" +
            "           for (var key in object) {" +
            "               if (Object.prototype.hasOwnProperty.call(object, key)) {" +
            "                   values.push(object[key]);" +
            "               }" +
            "           }" +
            "           return values;" +
            "       };" +
            "   }" +
            "   " +
            "   // 5.9 Object.entries" +
            "   if (!Object.entries) {" +
            "       Object.entries = function(object) {" +
            "           var entries = [];" +
            "           for (var key in object) {" +
            "               if (Object.prototype.hasOwnProperty.call(object, key)) {" +
            "                   entries.push([key, object[key]]);" +
            "               }" +
            "           }" +
            "           return entries;" +
            "       };" +
            "   }" +
            "   " +
            "   // 5.10 Set" +
            "   if (typeof Set === 'undefined') {" +
            "       window.Set = function(iterable) {" +
            "           this._data = [];" +
            "           if (iterable) {" +
            "               for (var i = 0; i < iterable.length; i++) {" +
            "                   this.add(iterable[i]);" +
            "               }" +
            "           }" +
            "       };" +
            "       Set.prototype.add = function(value) {" +
            "           if (!this.has(value)) {" +
            "               this._data.push(value);" +
            "           }" +
            "           return this;" +
            "       };" +
            "       Set.prototype.has = function(value) {" +
            "           return this._data.indexOf(value) !== -1;" +
            "       };" +
            "       Set.prototype.delete = function(value) {" +
            "           var index = this._data.indexOf(value);" +
            "           if (index !== -1) {" +
            "               this._data.splice(index, 1);" +
            "               return true;" +
            "           }" +
            "           return false;" +
            "       };" +
            "       Set.prototype.clear = function() {" +
            "           this._data = [];" +
            "       };" +
            "       Set.prototype.forEach = function(callback) {" +
            "           for (var i = 0; i < this._data.length; i++) {" +
            "               callback(this._data[i], this._data[i], this);" +
            "           }" +
            "       };" +
            "       Object.defineProperty(Set.prototype, 'size', {" +
            "           get: function() { return this._data.length; }" +
            "       });" +
            "   }" +
            "   " +
            "   // 5.11 Map" +
            "   if (typeof Map === 'undefined') {" +
            "       window.Map = function(iterable) {" +
            "           this._keys = [];" +
            "           this._values = [];" +
            "           if (iterable) {" +
            "               for (var i = 0; i < iterable.length; i++) {" +
            "                   this.set(iterable[i][0], iterable[i][1]);" +
            "               }" +
            "           }" +
            "       };" +
            "       Map.prototype.set = function(key, value) {" +
            "           var index = this._keys.indexOf(key);" +
            "           if (index !== -1) {" +
            "               this._values[index] = value;" +
            "           } else {" +
            "               this._keys.push(key);" +
            "               this._values.push(value);" +
            "           }" +
            "           return this;" +
            "       };" +
            "       Map.prototype.get = function(key) {" +
            "           var index = this._keys.indexOf(key);" +
            "           return index !== -1 ? this._values[index] : undefined;" +
            "       };" +
            "       Map.prototype.has = function(key) {" +
            "           return this._keys.indexOf(key) !== -1;" +
            "       };" +
            "       Map.prototype.delete = function(key) {" +
            "           var index = this._keys.indexOf(key);" +
            "           if (index !== -1) {" +
            "               this._keys.splice(index, 1);" +
            "               this._values.splice(index, 1);" +
            "               return true;" +
            "           }" +
            "           return false;" +
            "       };" +
            "       Map.prototype.clear = function() {" +
            "           this._keys = [];" +
            "           this._values = [];" +
            "       };" +
            "       Map.prototype.forEach = function(callback) {" +
            "           for (var i = 0; i < this._keys.length; i++) {" +
            "               callback(this._values[i], this._keys[i], this);" +
            "           }" +
            "       };" +
            "       Object.defineProperty(Map.prototype, 'size', {" +
            "           get: function() { return this._keys.length; }" +
            "       });" +
            "   }" +
            "   " +
            "   // 5.12 console.log replacement dengan filter" +
            "   if (!window.__debug) {" +
            "       window.__debug = false;" +
            "   }" +
            "   " +
            "   console.log('Modern polyfills injected');" +
            "})()";

        view.loadUrl(polyfills);
    }

    // ============================================
    // VIEWPORT FIX
    // ============================================
    private void fixViewport(WebView view) {
        String viewportFix = 
            "javascript:(function() {" +
            "   var viewport = document.querySelector('meta[name=viewport]');" +
            "   if (!viewport) {" +
            "       viewport = document.createElement('meta');" +
            "       viewport.name = 'viewport';" +
            "       viewport.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';" +
            "       document.head.appendChild(viewport);" +
            "   } else {" +
            "       if (viewport.content.indexOf('user-scalable') === -1) {" +
            "           viewport.content = viewport.content + ', user-scalable=yes';" +
            "       }" +
            "       if (viewport.content.indexOf('width=') === -1) {" +
            "           viewport.content = 'width=device-width, ' + viewport.content;" +
            "       }" +
            "   }" +
            "   console.log('Viewport fixed');" +
            "})()";

        view.loadUrl(viewportFix);
    }

    // ============================================
    // STORAGE SUPPORT INJECTION
    // ============================================
    private void injectStorageSupport(WebView view) {
        String script =
            "javascript:(function() {" +
            "   if (typeof window.Android === 'undefined') {" +
            "       console.warn('Android interface not available');" +
            "       return;" +
            "   }" +
            "   window.getBasePath = function() { return window.Android.getBasePath(); };" +
            "   window.listFiles = function(path) { return window.Android.listFiles(path); };" +
            "   window.readFile = function(path) { return window.Android.readFile(path); };" +
            "   window.writeFile = function(path, content) { return window.Android.writeFile(path, content); };" +
            "   window.fileExists = function(path) { return window.Android.fileExists(path); };" +
            "   window.deleteFile = function(path) { return window.Android.deleteFile(path); };" +
            "   window.getFileSize = function(path) { return window.Android.getFileSize(path); };" +
            "   console.log('StorageSupport helpers injected');" +
            "})()";
        view.loadUrl(script);
    }

    // ============================================
    // GAMING OPTIMIZATION INJECTION
    // ============================================
    private void injectGamingOptimization(WebView view) {
        String script =
            "javascript:(function() {" +
            "   if (typeof requestAnimationFrame !== 'undefined') {" +
            "       var originalRAF = window.requestAnimationFrame;" +
            "       window.requestAnimationFrame = function(callback) {" +
            "           return originalRAF(function(timestamp) {" +
            "               callback(timestamp);" +
            "           });" +
            "       };" +
            "   }" +
            "   var style = document.createElement('style');" +
            "   style.type = 'text/css';" +
            "   style.textContent = '" +
            "       * { -webkit-tap-highlight-color: transparent; }" +
            "       html, body { overflow-y: auto; -webkit-overflow-scrolling: touch; }" +
            "       canvas { image-rendering: optimizeSpeed; -webkit-image-rendering: optimizeSpeed; " +
            "                image-rendering: -moz-crisp-edges; image-rendering: crisp-edges; transform: translateZ(0); } " +
            "       img { image-rendering: optimizeSpeed; -webkit-image-rendering: optimizeSpeed; } " +
            "   ';" +
            "   document.head.appendChild(style);" +
            "   console.log('Gaming & ESM optimization injected');" +
            "})()";
        view.loadUrl(script);
    }

    // ============================================
    // 5. MODERN WEBVIEWCLIENT - CSS/JS INJECTION
    // ============================================
    private class ModernWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            // 1. INJECT MODERN CSS FIX
            injectModernCSSFix(view);

            // 2. INJECT MODERN JAVASCRIPT POLYFILLS
            injectModernJavaScript(view);

            // 3. INJECT MODERN POLYFILLS TAMBAHAN
            injectModernPolyfills(view);

            // INJECT VIEWPORT FIX
            fixViewport(view);

            // === INJECT TRANSLATOR HOOK (CANVAS 2D) ===
            injectTextTranslator(view);

            // INJECT GAMING OPTIMIZATION
            if (gameOptimizer != null) {
                gameOptimizer.optimizeForChromeLikePerformance();
                gameOptimizer.optimizeRenderingPipeline();
                gameOptimizer.optimizeRenderer();
                gameOptimizer.forceGPUCanvas();
                gameOptimizer.memoryCleanup();
            }

            // INJECT STORAGE SUPPORT
            injectStorageSupport(view);

            // INJECT GAMING OPTIMIZATION
            injectGamingOptimization(view);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.util.Log.e("WebView", "Error: " + error.getDescription());
            }
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (Build.VERSION.SDK_INT >= 21) {
                return processIntercept(request.getUrl().toString());
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return processIntercept(url);
        }

        private WebResourceResponse processIntercept(String urlString) {
            try {
                String decodedUrl = java.net.URLDecoder.decode(urlString, "UTF-8");

                if (decodedUrl.startsWith("file://")) {
                    String filePath = decodedUrl.replace("file://", "");
                    File file = new File(filePath);

                    if (file.exists() && file.isFile()) {
                        String mimeType = "*/*";
                        String encoding = null;

                        if (filePath.endsWith(".js")) {
                            mimeType = "application/javascript";
                            encoding = "UTF-8";
                        } else if (filePath.endsWith(".json")) {
                            mimeType = "application/json";
                            encoding = "UTF-8";
                        } else if (filePath.endsWith(".wasm")) {
                            mimeType = "application/wasm";
                            encoding = null;
                        } else if (filePath.endsWith(".onnx")) {
                            mimeType = "application/octet-stream";
                            encoding = null;
                        } else if (filePath.endsWith(".html")) {
                            mimeType = "text/html";
                            encoding = "UTF-8";
                        }

                        InputStream is = new BufferedInputStream(new FileInputStream(file));
                        return new WebResourceResponse(mimeType, encoding, is);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    // ============================================
    // TOGGLE ROTATE WINDOW
    // ============================================
    private void toggleRotateWindow() {
        if (isMinimized) return;

        isRotated = !isRotated;
        Button btnRotate = (Button) floatingView.findViewById(R.id.btnRotate);

        if (isRotated) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;

            params.width = (int) (screenWidth * 0.92);
            params.height = (int) (screenHeight * 0.45);

            params.x = (screenWidth - params.width) / 2;
            params.y = (screenHeight - params.height) / 2;

            lnrHeader.setPadding(2, 2, 2, 2);

            TextView txtHeader = (TextView) floatingView.findViewById(R.id.txtHeaderDrag);
            txtHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

            LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            spinnerPilihApp.setLayoutParams(spinnerParams);

            int btnSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 26, getResources().getDisplayMetrics());

            Button btnMinimize = (Button) floatingView.findViewById(R.id.btnMinimizeApp);
            Button btnFull = (Button) floatingView.findViewById(R.id.btnFullWindow);
            Button btnClose = (Button) floatingView.findViewById(R.id.btnCloseApp);

            btnMinimize.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
            btnFull.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
            btnClose.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
            btnRotate.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));

            btnMinimize.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            btnFull.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            btnRotate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

            btnRotate.setText("⟳");
        } else {
            restoreWindowSize();
        }

        rootLayout.setRotation(0f);
        params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        windowManager.updateViewLayout(floatingView, params);
    }

    // ============================================
    // RESTORE WINDOW SIZE
    // ============================================
    private void restoreWindowSize() {
        originalWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 380, getResources().getDisplayMetrics());
        originalHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 520, getResources().getDisplayMetrics());

        params.width = originalWidth;
        params.height = originalHeight;
        params.x = originalX;
        params.y = originalY;

        isRotated = false;
        rootLayout.setRotation(0f);

        lnrHeader.setPadding(6, 6, 6, 6);

        TextView txtHeader = (TextView) floatingView.findViewById(R.id.txtHeaderDrag);
        txtHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        spinnerPilihApp.setLayoutParams(spinnerParams);

        int btnSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28, getResources().getDisplayMetrics());

        Button btnMinimize = (Button) floatingView.findViewById(R.id.btnMinimizeApp);
        Button btnFull = (Button) floatingView.findViewById(R.id.btnFullWindow);
        Button btnClose = (Button) floatingView.findViewById(R.id.btnCloseApp);
        Button btnRotate = (Button) floatingView.findViewById(R.id.btnRotate);

        btnMinimize.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
        btnFull.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
        btnClose.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
        btnRotate.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));

        btnMinimize.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnFull.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnRotate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        btnRotate.setText("↻");

        params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
    }

    private void toggleFullWindow() {
        if (!isFullWindow) {
            isFullWindow = true;
            int layarWidth = getResources().getDisplayMetrics().widthPixels;
            int layarHeight = getResources().getDisplayMetrics().heightPixels;

            params.width = layarWidth;
            params.height = layarHeight;
            params.x = 0;
            params.y = 0;
            lnrMainContent.setVisibility(View.VISIBLE);
            floatWebView.setVisibility(View.VISIBLE);
            spinnerPilihApp.setVisibility(View.VISIBLE);
            isMinimized = false;

            Button btnFull = (Button) floatingView.findViewById(R.id.btnFullWindow);
            btnFull.setText("⛶");

            if (isRotated) {
                isRotated = false;
                rootLayout.setRotation(0f);
                Button btnRotate = (Button) floatingView.findViewById(R.id.btnRotate);
                btnRotate.setText("↻");
                restoreUI();
            }

            params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            isFullWindow = false;
            restoreWindowSize();

            Button btnFull = (Button) floatingView.findViewById(R.id.btnFullWindow);
            btnFull.setText("⬜");

            params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        windowManager.updateViewLayout(floatingView, params);
    }

    private void restoreUI() {
        lnrHeader.setPadding(6, 6, 6, 6);

        TextView txtHeader = (TextView) floatingView.findViewById(R.id.txtHeaderDrag);
        txtHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        txtHeader.setText("≡");

        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        spinnerPilihApp.setLayoutParams(spinnerParams);

        int btnSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28, getResources().getDisplayMetrics());

        Button btnMinimize = (Button) floatingView.findViewById(R.id.btnMinimizeApp);
        Button btnFull = (Button) floatingView.findViewById(R.id.btnFullWindow);
        Button btnClose = (Button) floatingView.findViewById(R.id.btnCloseApp);
        Button btnRotate = (Button) floatingView.findViewById(R.id.btnRotate);

        btnMinimize.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
        btnFull.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
        btnClose.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
        btnRotate.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));

        btnMinimize.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnFull.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnRotate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        rootLayout.setRotation(0f);
    }

    private void scanDaftarProgramRealtime() {
        namaAplikasi.clear();
        pathIndexHtml.clear();

        if (folderUtama.exists() && folderUtama.isDirectory()) {
            File[] subFolders = folderUtama.listFiles();
            if (subFolders != null) {
                for (int i = 0; i < subFolders.length; i++) {
                    File folderTarget = subFolders[i];
                    if (folderTarget.isDirectory()) {
                        File fileHtml = new File(folderTarget, "index.html");
                        if (fileHtml.exists()) {
                            namaAplikasi.add(folderTarget.getName());
                            pathIndexHtml.add(fileHtml.getAbsolutePath());
                        }
                    }
                }
            }
        }

        if (namaAplikasi.isEmpty()) {
            namaAplikasi.add("Tidak ada program");
            pathIndexHtml.add("");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, namaAplikasi);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPilihApp.setAdapter(adapter);

        if (!pathHtmlDijalankan.equals("")) {
            for (int k = 0; k < pathIndexHtml.size(); k++) {
                if (pathIndexHtml.get(k).equals(pathHtmlDijalankan)) {
                    isAwalDimuat = true;
                    spinnerPilihApp.setSelection(k);
                    break;
                }
            }
        }

        spinnerPilihApp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isAwalDimuat) {
                        isAwalDimuat = false;
                        if (pathHtmlDijalankan.equals("")) {
                            String pathAwal = pathIndexHtml.get(position);
                            if (pathAwal != null && !pathAwal.equals("")) {
                                pathHtmlDijalankan = pathAwal;
                                floatWebView.loadUrl("file://" + pathHtmlDijalankan);
                                if (gameOptimizer != null) {
                                    gameOptimizer.optimizeRenderer();
                                    gameOptimizer.optimizeWebGL();
                                }
                            }
                        }
                        return;
                    }

                    String pathBaru = pathIndexHtml.get(position);
                    if (pathBaru != null && !pathBaru.equals("")) {
                        pathHtmlDijalankan = pathBaru;
                        floatWebView.loadUrl("file://" + pathBaru);
                        if (gameOptimizer != null) {
                            gameOptimizer.optimizeRenderer();
                            gameOptimizer.optimizeWebGL();
                            gameOptimizer.memoryCleanup();
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("PATH_HTML")) {
            pathHtmlDijalankan = intent.getStringExtra("PATH_HTML");
            if (floatWebView != null && pathHtmlDijalankan != null && !pathHtmlDijalankan.equals("")) {
                floatWebView.loadUrl("file://" + pathHtmlDijalankan);
                scanDaftarProgramRealtime();
                if (gameOptimizer != null) {
                    gameOptimizer.optimizeRenderer();
                    gameOptimizer.optimizeWebGL();
                    gameOptimizer.memoryCleanup();
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (udpManager != null) {
        }
        if (gameOptimizer != null) {
            gameOptimizer.clearMemory();
        }
        if (storageSupport != null) {
            storageSupport.optimizeMemory();
        }
        if (floatingView != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
            }
        }
        if (floatWebView != null) {
            floatWebView.clearCache(true);
            floatWebView.clearHistory();
            floatWebView.clearFormData();
            floatWebView.destroy();
            floatWebView = null;
        }
        System.gc();
    }

    private void runOnUiThread(Runnable action) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            action.run();
        } else {
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(action);
        }
    }
}
