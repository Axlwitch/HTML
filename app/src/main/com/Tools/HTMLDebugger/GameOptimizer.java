package com.Tools.HTMLDebugger;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebSettings;
import java.lang.reflect.Method;

public class GameOptimizer {
    private Context context;
    private WebView webView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isOptimized = false;

    public GameOptimizer(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
    }

    public void applyAllOptimizations() {
        if (isOptimized) return;
        isOptimized = true;

        setProcessPriority();
        optimizeWebViewSettings();
        enableHardwareAcceleration();
        optimizeMemory();
        startPerformanceMonitor();
    }

    private void setProcessPriority() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
            android.os.Process.setThreadPriority(Process.myTid(), Process.THREAD_PRIORITY_URGENT_DISPLAY);
        } catch (Exception e) {}
    }

    private void optimizeWebViewSettings() {
        if (webView == null) return;
        try {
            WebSettings settings = webView.getSettings();

            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setAppCacheEnabled(false);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(false);
            settings.setJavaScriptEnabled(true);
            settings.setSupportZoom(false);
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

            if (Build.VERSION.SDK_INT >= 21) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }

            if (Build.VERSION.SDK_INT >= 19) {
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            webView.setWillNotDraw(false);
            webView.setDrawingCacheEnabled(false);
            webView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

        } catch (Exception e) {}
    }

    private void enableHardwareAcceleration() {
        if (webView == null) return;
        try {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            if (Build.VERSION.SDK_INT >= 21) {
                try {
                    Method setRenderMode = webView.getClass().getMethod("setRenderMode", int.class);
                    setRenderMode.invoke(webView, 0);
                } catch (Exception e) {}
            }

            try {
                Method setNetworkAvailable = webView.getClass().getMethod("setNetworkAvailable", boolean.class);
                setNetworkAvailable.invoke(webView, true);
            } catch (Exception e) {}

        } catch (Exception e) {}
    }

    private void optimizeMemory() {
        try {
            Runtime.getRuntime().gc();
            System.gc();

            if (webView != null) {
                webView.freeMemory();
                webView.clearCache(true);
            }

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Runtime.getRuntime().gc();
                    }
                }, 3000);

        } catch (Exception e) {}
    }

    private void startPerformanceMonitor() {
        handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (webView != null) {
                        memoryCleanup();
                    }
                    handler.postDelayed(this, 10000);
                }
            }, 5000);
    }

    public void optimizeRenderer() {
        if (webView == null) return;
        try {
            String js = "javascript:(function() {" +
                "   var canvases = document.getElementsByTagName('canvas');" +
                "   for(var i = 0; i < canvases.length; i++) {" +
                "       canvases[i].style.transform = 'translateZ(0)';" +
                "       canvases[i].style.willChange = 'transform, opacity';" +
                "   }" +
                "   var style = document.createElement('style');" +
                "   style.type = 'text/css';" +
                "   style.textContent = '" +
                "       html, body { overflow-y: auto !important; -webkit-overflow-scrolling: touch !important; } " +
                "       * { -webkit-tap-highlight-color: transparent !important; } " +
                "       canvas { image-rendering: optimizeSpeed !important; -webkit-image-rendering: optimizeSpeed !important; } " +
                "   ';" +
                "   document.head.appendChild(style);" +
                "   console.log('Optimization applied');" +
                "})()";
            webView.loadUrl(js);
        } catch (Exception e) {}
    }

    public void memoryCleanup() {
        try {
            Runtime.getRuntime().gc();
            System.gc();
            if (webView != null) {
                webView.clearCache(true);
                webView.freeMemory();
            }
        } catch (Exception e) {}
    }

    public void setLowQualityMode() {
        if (webView == null) return;
        try {
            String js = "javascript:(function() {" +
                "   var style = document.createElement('style');" +
                "   style.type = 'text/css';" +
                "   style.textContent = '" +
                "       * { image-rendering: optimizeSpeed; -webkit-image-rendering: optimizeSpeed; } " +
                "       canvas { image-rendering: optimizeSpeed; -webkit-image-rendering: optimizeSpeed; " +
                "                image-rendering: -moz-crisp-edges; image-rendering: -o-crisp-edges; " +
                "                image-rendering: crisp-edges; } " +
                "       img { image-rendering: optimizeSpeed; -webkit-image-rendering: optimizeSpeed; " +
                "             image-rendering: -moz-crisp-edges; image-rendering: -o-crisp-edges; " +
                "             image-rendering: crisp-edges; } " +
                "   ';" +
                "   document.head.appendChild(style);" +
                "})()";
            webView.loadUrl(js);
        } catch (Exception e) {}
    }

    public void forceGPUCanvas() {
        if (webView == null) return;
        try {
            String js = "javascript:(function() {" +
                "   var canvases = document.getElementsByTagName('canvas');" +
                "   for(var i = 0; i < canvases.length; i++) {" +
                "       canvases[i].style.transform = 'translateZ(0)';" +
                "       canvases[i].style.webkitTransform = 'translateZ(0)';" +
                "       canvases[i].style.willChange = 'transform, opacity';" +
                "   }" +
                "})()";
            webView.loadUrl(js);
        } catch (Exception e) {}
    }

    public void optimizeTextureCompression() {
        if (webView == null) return;
        try {
            String js = "javascript:(function() {" +
                "   if (window.WebGLRenderingContext) {" +
                "       var canvas = document.createElement('canvas');" +
                "       var gl = canvas.getContext('webgl', { " +
                "           alpha: false, " +
                "           antialias: false, " +
                "           powerPreference: 'high-performance', " +
                "           failIfMajorPerformanceCaveat: false " +
                "       });" +
                "       if (gl) {" +
                "           var extensions = gl.getSupportedExtensions();" +
                "           var compExts = ['WEBGL_compressed_texture_s3tc', 'WEBGL_compressed_texture_etc1'," +
                "                           'WEBGL_compressed_texture_pvrtc', 'WEBGL_compressed_texture_atc']; " +
                "           for(var i = 0; i < compExts.length; i++) {" +
                "               if(extensions && extensions.includes(compExts[i])) {" +
                "                   gl.getExtension(compExts[i]);" +
                "               }" +
                "           }" +
                "           var ext = gl.getExtension('WEBGL_debug_renderer_info');" +
                "           if(ext) {" +
                "               var renderer = gl.getParameter(ext.UNMASKED_RENDERER_WEBGL);" +
                "               console.log('GPU: ' + renderer);" +
                "           }" +
                "       }" +
                "   }" +
                "})()";
            webView.loadUrl(js);
        } catch (Exception e) {}
    }

    public void disableAnimations() {
        if (webView == null) return;
        try {
            String js = "javascript:(function() {" +
                "   var style = document.createElement('style');" +
                "   style.type = 'text/css';" +
                "   style.textContent = '" +
                "       * { transition-duration: 0s !important; " +
                "           animation-duration: 0s !important; " +
                "           animation-delay: 0s !important; } " +
                "   ';" +
                "   document.head.appendChild(style);" +
                "})()";
            webView.loadUrl(js);
        } catch (Exception e) {}
    }

    public void reduceDrawCalls() {
        if (webView == null) return;
        try {
            String js = "javascript:(function() {" +
                "   var style = document.createElement('style');" +
                "   style.type = 'text/css';" +
                "   style.textContent = '" +
                "       .hidden { display: none !important; } " +
                "       .invisible { visibility: hidden !important; } " +
                "       .no-shadow { box-shadow: none !important; text-shadow: none !important; } " +
                "   ';" +
                "   document.head.appendChild(style);" +
                "})()";
            webView.loadUrl(js);
        } catch (Exception e) {}
    }

    public void enableGameMode() {
        applyAllOptimizations();

        optimizeRenderer();
        memoryCleanup();
        setLowQualityMode();
        forceGPUCanvas();
        optimizeTextureCompression();
        disableAnimations();
        reduceDrawCalls();

        if (webView != null) {
            webView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        memoryCleanup();
                    }
                }, 3000);

            webView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        memoryCleanup();
                    }
                }, 10000);
        }
    }

    public void optimizeFPS() {
        if (webView == null) return;
        try {
            String js = "javascript:(function() {" +
                "   var fps = 0;" +
                "   var frameCount = 0;" +
                "   var startTime = performance.now();" +
                "   var oldRAF = window.requestAnimationFrame || function(cb) { setTimeout(cb, 16); };" +
                "   window.requestAnimationFrame = function(callback) {" +
                "       frameCount++;" +
                "       var now = performance.now();" +
                "       if (now - startTime >= 1000) {" +
                "           fps = frameCount;" +
                "           frameCount = 0;" +
                "           startTime = now;" +
                "       }" +
                "       return oldRAF(function(timestamp) {" +
                "           callback(timestamp);" +
                "       });" +
                "   };" +
                "})()";
            webView.loadUrl(js);
        } catch (Exception e) {}
    }

    public void optimizeWebGL() {
        if (webView == null) return;
        try {
            String js = "javascript:(function() {" +
                "   if (window.WebGLRenderingContext) {" +
                "       var originalGetContext = HTMLCanvasElement.prototype.getContext;" +
                "       HTMLCanvasElement.prototype.getContext = function(type, attributes) {" +
                "           if (type === 'webgl' || type === 'experimental-webgl') {" +
                "               attributes = attributes || {};" +
                "               attributes.alpha = false;" +
                "               attributes.antialias = false;" +
                "               attributes.premultipliedAlpha = false;" +
                "               attributes.preserveDrawingBuffer = false;" +
                "               attributes.powerPreference = 'high-performance';" +
                "               attributes.failIfMajorPerformanceCaveat = false;" +
                "           }" +
                "           return originalGetContext.call(this, type, attributes);" +
                "       };" +
                "   }" +
                "})()";
            webView.loadUrl(js);
        } catch (Exception e) {}
    }

    public void resizeViewport() {
        if (webView == null) return;
        try {
            int width = context.getResources().getDisplayMetrics().widthPixels;
            int height = context.getResources().getDisplayMetrics().heightPixels;

            String js = "javascript:(function() {" +
                "   var viewport = document.querySelector('meta[name=viewport]');" +
                "   if (viewport) {" +
                "       viewport.content = 'width=" + width + ", height=" + height + ", user-scalable=no';" +
                "   }" +
                "})()";
            webView.loadUrl(js);
        } catch (Exception e) {}
    }

    public void clearMemory() {
        try {
            if (webView != null) {
                webView.clearCache(true);
                webView.clearHistory();
                webView.clearFormData();
                webView.freeMemory();
            }
            Runtime.getRuntime().gc();
            System.gc();
        } catch (Exception e) {}
    }

    // ============================================
    // 1. MODERN CSS FIX - UNTUK HALAMAN CSS MODERN
    // ============================================
    public void injectModernCSSFix() {
        if (webView == null) return;
        try {
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
                "       .justify-evenly { -webkit-justify-content: space-evenly !important; -ms-flex-pack: evenly !important; justify-content: space-evenly !important; }" +
                "       " +
                "       .items-start { -webkit-align-items: flex-start !important; -ms-flex-align: start !important; align-items: flex-start !important; }" +
                "       .items-center { -webkit-align-items: center !important; -ms-flex-align: center !important; align-items: center !important; }" +
                "       .items-end { -webkit-align-items: flex-end !important; -ms-flex-align: end !important; align-items: flex-end !important; }" +
                "       .items-stretch { -webkit-align-items: stretch !important; -ms-flex-align: stretch !important; align-items: stretch !important; }" +
                "       .items-baseline { -webkit-align-items: baseline !important; -ms-flex-align: baseline !important; align-items: baseline !important; }" +
                "       " +
                "       .self-start { -webkit-align-self: flex-start !important; -ms-flex-item-align: start !important; align-self: flex-start !important; }" +
                "       .self-center { -webkit-align-self: center !important; -ms-flex-item-align: center !important; align-self: center !important; }" +
                "       .self-end { -webkit-align-self: flex-end !important; -ms-flex-item-align: end !important; align-self: flex-end !important; }" +
                "       .self-stretch { -webkit-align-self: stretch !important; -ms-flex-item-align: stretch !important; align-self: stretch !important; }" +
                "       .self-baseline { -webkit-align-self: baseline !important; -ms-flex-item-align: baseline !important; align-self: baseline !important; }" +
                "       " +
                "       .flex-grow-0 { -webkit-flex-grow: 0 !important; -ms-flex-positive: 0 !important; flex-grow: 0 !important; }" +
                "       .flex-grow-1 { -webkit-flex-grow: 1 !important; -ms-flex-positive: 1 !important; flex-grow: 1 !important; }" +
                "       .flex-shrink-0 { -webkit-flex-shrink: 0 !important; -ms-flex-negative: 0 !important; flex-shrink: 0 !important; }" +
                "       .flex-shrink-1 { -webkit-flex-shrink: 1 !important; -ms-flex-negative: 1 !important; flex-shrink: 1 !important; }" +
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
                "       /* === GRID PROPERTIES === */" +
                "       .grid-cols-1 { -ms-grid-columns: 1fr !important; grid-template-columns: repeat(1, 1fr) !important; }" +
                "       .grid-cols-2 { -ms-grid-columns: 1fr 1fr !important; grid-template-columns: repeat(2, 1fr) !important; }" +
                "       .grid-cols-3 { -ms-grid-columns: 1fr 1fr 1fr !important; grid-template-columns: repeat(3, 1fr) !important; }" +
                "       .grid-cols-4 { -ms-grid-columns: 1fr 1fr 1fr 1fr !important; grid-template-columns: repeat(4, 1fr) !important; }" +
                "       .grid-cols-5 { -ms-grid-columns: 1fr 1fr 1fr 1fr 1fr !important; grid-template-columns: repeat(5, 1fr) !important; }" +
                "       .grid-cols-6 { -ms-grid-columns: 1fr 1fr 1fr 1fr 1fr 1fr !important; grid-template-columns: repeat(6, 1fr) !important; }" +
                "       " +
                "       .gap-1 { gap: 0.25rem !important; }" +
                "       .gap-2 { gap: 0.5rem !important; }" +
                "       .gap-3 { gap: 0.75rem !important; }" +
                "       .gap-4 { gap: 1rem !important; }" +
                "       .gap-5 { gap: 1.25rem !important; }" +
                "       .gap-6 { gap: 1.5rem !important; }" +
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
                "           --white: #ffffff;" +
                "           --black: #000000;" +
                "           --gray-100: #f8f9fa;" +
                "           --gray-200: #e9ecef;" +
                "           --gray-300: #dee2e6;" +
                "           --gray-400: #ced4da;" +
                "           --gray-500: #adb5bd;" +
                "           --gray-600: #6c757d;" +
                "           --gray-700: #495057;" +
                "           --gray-800: #343a40;" +
                "           --gray-900: #212529;" +
                "       }" +
                "       " +
                "       /* === MODERN FONT RENDERING === */" +
                "       body {" +
                "           font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif !important;" +
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
                "           min-height: 100vh !important;" +
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
                "       button {" +
                "           cursor: pointer !important;" +
                "           background: #007bff !important;" +
                "           color: #ffffff !important;" +
                "           border: none !important;" +
                "           padding: 8px 16px !important;" +
                "           border-radius: 4px !important;" +
                "           transition: all 0.3s ease !important;" +
                "       }" +
                "       button:hover {" +
                "           background: #0056b3 !important;" +
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
                "       /* === MODERN LINKS === */" +
                "       a {" +
                "           color: #007bff !important;" +
                "           text-decoration: none !important;" +
                "           transition: color 0.2s ease !important;" +
                "       }" +
                "       a:hover {" +
                "           color: #0056b3 !important;" +
                "           text-decoration: underline !important;" +
                "       }" +
                "       " +
                "       /* === MODERN CARD === */" +
                "       .card {" +
                "           background: #ffffff !important;" +
                "           border-radius: 8px !important;" +
                "           box-shadow: 0 2px 4px rgba(0,0,0,0.1) !important;" +
                "           padding: 16px !important;" +
                "           margin-bottom: 16px !important;" +
                "           transition: box-shadow 0.3s ease !important;" +
                "       }" +
                "       .card:hover {" +
                "           box-shadow: 0 4px 8px rgba(0,0,0,0.15) !important;" +
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
                "           transition: background 0.3s ease !important;" +
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
                "       .transition-fast { -webkit-transition: all 0.15s ease !important; -moz-transition: all 0.15s ease !important; -ms-transition: all 0.15s ease !important; -o-transition: all 0.15s ease !important; transition: all 0.15s ease !important; }" +
                "       .transition-slow { -webkit-transition: all 0.6s ease !important; -moz-transition: all 0.6s ease !important; -ms-transition: all 0.6s ease !important; -o-transition: all 0.6s ease !important; transition: all 0.6s ease !important; }" +
                "       " +
                "       /* === CSS TRANSFORMS === */" +
                "       .rotate-90 { -webkit-transform: rotate(90deg) !important; -moz-transform: rotate(90deg) !important; -ms-transform: rotate(90deg) !important; -o-transform: rotate(90deg) !important; transform: rotate(90deg) !important; }" +
                "       .rotate-180 { -webkit-transform: rotate(180deg) !important; -moz-transform: rotate(180deg) !important; -ms-transform: rotate(180deg) !important; -o-transform: rotate(180deg) !important; transform: rotate(180deg) !important; }" +
                "       .scale-110 { -webkit-transform: scale(1.1) !important; -moz-transform: scale(1.1) !important; -ms-transform: scale(1.1) !important; -o-transform: scale(1.1) !important; transform: scale(1.1) !important; }" +
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
                "       .fade-out {" +
                "           -webkit-animation: fadeOut 0.3s ease-out !important;" +
                "           -moz-animation: fadeOut 0.3s ease-out !important;" +
                "           -ms-animation: fadeOut 0.3s ease-out !important;" +
                "           -o-animation: fadeOut 0.3s ease-out !important;" +
                "           animation: fadeOut 0.3s ease-out !important;" +
                "       }" +
                "       " +
                "       .slide-in {" +
                "           -webkit-animation: slideIn 0.3s ease-in !important;" +
                "           -moz-animation: slideIn 0.3s ease-in !important;" +
                "           -ms-animation: slideIn 0.3s ease-in !important;" +
                "           -o-animation: slideIn 0.3s ease-in !important;" +
                "           animation: slideIn 0.3s ease-in !important;" +
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
                "       " +
                "       @-webkit-keyframes fadeOut {" +
                "           from { opacity: 1; }" +
                "           to { opacity: 0; }" +
                "       }" +
                "       @-moz-keyframes fadeOut {" +
                "           from { opacity: 1; }" +
                "           to { opacity: 0; }" +
                "       }" +
                "       @-ms-keyframes fadeOut {" +
                "           from { opacity: 1; }" +
                "           to { opacity: 0; }" +
                "       }" +
                "       @-o-keyframes fadeOut {" +
                "           from { opacity: 1; }" +
                "           to { opacity: 0; }" +
                "       }" +
                "       @keyframes fadeOut {" +
                "           from { opacity: 1; }" +
                "           to { opacity: 0; }" +
                "       }" +
                "       " +
                "       @-webkit-keyframes slideIn {" +
                "           from { -webkit-transform: translateY(20px); opacity: 0; }" +
                "           to { -webkit-transform: translateY(0); opacity: 1; }" +
                "       }" +
                "       @-moz-keyframes slideIn {" +
                "           from { -moz-transform: translateY(20px); opacity: 0; }" +
                "           to { -moz-transform: translateY(0); opacity: 1; }" +
                "       }" +
                "       @-ms-keyframes slideIn {" +
                "           from { -ms-transform: translateY(20px); opacity: 0; }" +
                "           to { -ms-transform: translateY(0); opacity: 1; }" +
                "       }" +
                "       @-o-keyframes slideIn {" +
                "           from { -o-transform: translateY(20px); opacity: 0; }" +
                "           to { -o-transform: translateY(0); opacity: 1; }" +
                "       }" +
                "       @keyframes slideIn {" +
                "           from { transform: translateY(20px); opacity: 0; }" +
                "           to { transform: translateY(0); opacity: 1; }" +
                "       }" +
                "       " +
                "       /* === CSS FILTERS === */" +
                "       .blur-sm { -webkit-filter: blur(2px) !important; -moz-filter: blur(2px) !important; -ms-filter: blur(2px) !important; -o-filter: blur(2px) !important; filter: blur(2px) !important; }" +
                "       .blur-md { -webkit-filter: blur(4px) !important; -moz-filter: blur(4px) !important; -ms-filter: blur(4px) !important; -o-filter: blur(4px) !important; filter: blur(4px) !important; }" +
                "       .blur-lg { -webkit-filter: blur(8px) !important; -moz-filter: blur(8px) !important; -ms-filter: blur(8px) !important; -o-filter: blur(8px) !important; filter: blur(8px) !important; }" +
                "       .brightness-50 { -webkit-filter: brightness(0.5) !important; -moz-filter: brightness(0.5) !important; -ms-filter: brightness(0.5) !important; -o-filter: brightness(0.5) !important; filter: brightness(0.5) !important; }" +
                "       .brightness-150 { -webkit-filter: brightness(1.5) !important; -moz-filter: brightness(1.5) !important; -ms-filter: brightness(1.5) !important; -o-filter: brightness(1.5) !important; filter: brightness(1.5) !important; }" +
                "   ';" +
                "   document.head.appendChild(style);" +
                "   console.log('Modern CSS Fix injected from GameOptimizer');" +
                "})()";
            webView.loadUrl(cssFix);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================
    // 2. JAVASCRIPT MODERN SUPPORT - ES6+ POLYFILLS
    // ============================================
    public void injectModernJavaScript() {
        if (webView == null) return;
        try {
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
                "       " +
                "       Promise.prototype.finally = function(onFinally) {" +
                "           return this.then(" +
                "               function(value) { return Promise.resolve(onFinally()).then(function() { return value; }); }," +
                "               function(reason) { return Promise.resolve(onFinally()).then(function() { throw reason; }); }" +
                "           );" +
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
                "   // 2.9 Array.find" +
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
                "   // 2.10 Array.findIndex" +
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
                "   // 2.11 String.includes" +
                "   if (!String.prototype.includes) {" +
                "       String.prototype.includes = function(search, start) {" +
                "           return this.indexOf(search, start || 0) !== -1;" +
                "       };" +
                "   }" +
                "   " +
                "   // 2.12 String.startsWith" +
                "   if (!String.prototype.startsWith) {" +
                "       String.prototype.startsWith = function(search, pos) {" +
                "           pos = pos || 0;" +
                "           return this.substr(pos, search.length) === search;" +
                "       };" +
                "   }" +
                "   " +
                "   // 2.13 String.endsWith" +
                "   if (!String.prototype.endsWith) {" +
                "       String.prototype.endsWith = function(search, this_len) {" +
                "           if (this_len === undefined || this_len > this.length) {" +
                "               this_len = this.length;" +
                "           }" +
                "           return this.substring(this_len - search.length, this_len) === search;" +
                "       };" +
                "   }" +
                "   " +
                "   // 2.14 Object.assign" +
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
                "   // 2.15 Object.values" +
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
                "   // 2.16 Object.entries" +
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
                "   // 2.17 Set" +
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
                "   // 2.18 Map" +
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
                "   // 2.19 Number.isInteger" +
                "   if (!Number.isInteger) {" +
                "       Number.isInteger = function(value) {" +
                "           return typeof value === 'number' && isFinite(value) && Math.floor(value) === value;" +
                "       };" +
                "   }" +
                "   " +
                "   // 2.20 Number.isNaN" +
                "   if (!Number.isNaN) {" +
                "       Number.isNaN = function(value) {" +
                "           return typeof value === 'number' && isNaN(value);" +
                "       };" +
                "   }" +
                "   " +
                "   // 2.21 Math.sign" +
                "   if (!Math.sign) {" +
                "       Math.sign = function(x) {" +
                "           return (x > 0) ? 1 : (x < 0) ? -1 : 0;" +
                "       };" +
                "   }" +
                "   " +
                "   console.log('Modern JavaScript ES6+ polyfills injected from GameOptimizer');" +
                "})()";
            webView.loadUrl(modernJS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================
    // 5. OPTIMASI CHROME-LIKE PERFORMANCE
    // ============================================
    public void optimizeForChromeLikePerformance() {
        if (webView == null) return;

        try {
            WebSettings settings = webView.getSettings();

            settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.setOffscreenPreRaster(true);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.setEnableSmoothTransition(true);
            }

            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setAppCacheEnabled(true);
            settings.setAppCacheMaxSize(1024 * 1024 * 20);

            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);

            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                settings.setMediaPlaybackRequiresUserGesture(false);
            }

            settings.setAllowFileAccess(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);

            settings.setJavaScriptEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);

            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);

            settings.setDefaultFontSize(16);
            settings.setDefaultFixedFontSize(13);
            settings.setMinimumFontSize(8);
            settings.setMinimumLogicalFontSize(8);

            settings.setSaveFormData(true);
            settings.setSavePassword(false);

            settings.setGeolocationEnabled(true);

            settings.setSupportMultipleWindows(false);

            settings.setDefaultTextEncodingName("UTF-8");

            settings.setPluginState(WebSettings.PluginState.OFF);

            settings.setMediaPlaybackRequiresUserGesture(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================
    // OPTIMASI RENDERING PIPELINE
    // ============================================
    public void optimizeRenderingPipeline() {
        if (webView == null) return;

        try {
            String renderJS = 
                "javascript:(function() {" +
                "   var style = document.createElement('style');" +
                "   style.type = 'text/css';" +
                "   style.textContent = '" +
                "       * { -webkit-transform: translateZ(0); -moz-transform: translateZ(0); -ms-transform: translateZ(0); -o-transform: translateZ(0); transform: translateZ(0); }" +
                "       html { scroll-behavior: smooth; }" +
                "       body { -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale; text-rendering: optimizeLegibility; }" +
                "       canvas { image-rendering: auto; -webkit-image-rendering: auto; }" +
                "   ';" +
                "   document.head.appendChild(style);" +
                "})()";

            webView.loadUrl(renderJS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================
    // INJECT ALL MODERN FEATURES
    // ============================================
    public void injectAllModernFeatures() {
        injectModernCSSFix();
        injectModernJavaScript();
        optimizeForChromeLikePerformance();
        optimizeRenderingPipeline();
        optimizeRenderer();
        forceGPUCanvas();
        memoryCleanup();
    }
}
