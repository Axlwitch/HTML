package com.Tools.HTMLDebugger;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

public class StorageSupport {
    private Context context;
    private ExecutorService downloadExecutor;
    private ExecutorService inferenceExecutor;
    private ConcurrentHashMap<String, ParallelDownloadTask> downloadTasks;
    private ConcurrentHashMap<String, Long> lastCheckMap;
    private ConcurrentHashMap<String, ModelCache> modelCache;
    private DownloadCallback webViewCallback;
    private long lastProgressUpdate;

    private static final int MAX_MODEL_CACHE_SIZE = 500;
    private static final long MAX_TOTAL_CACHE_SIZE = 2 * 1024 * 1024 * 1024L;

    public interface DownloadCallback {
        void onProgress(String jsonData);
        void onComplete(String taskId, String filePath);
        void onError(String taskId, String error);
    }

    public StorageSupport(Context context) {
        this.context = context;
        this.downloadExecutor = Executors.newFixedThreadPool(5);
        this.inferenceExecutor = Executors.newFixedThreadPool(2);
        this.downloadTasks = new ConcurrentHashMap<String, ParallelDownloadTask>();
        this.lastCheckMap = new ConcurrentHashMap<String, Long>();
        this.modelCache = new ConcurrentHashMap<String, ModelCache>();
        this.lastProgressUpdate = 0;
    }

    public void setDownloadCallback(DownloadCallback callback) {
        this.webViewCallback = callback;
    }

    // ============================================
    // WASM SUPPORT - TAMBAHAN BARU
    // ============================================

    @JavascriptInterface
    public boolean downloadWasmFiles() {
        try {
            String[] wasmFiles = {
                "ort-wasm-simd-threaded.wasm",
                "ort-wasm-simd.wasm",
                "ort-wasm.wasm"
            };

            String baseUrl = "https://cdn.jsdelivr.net/npm/onnxruntime-web@1.14.0/dist/";
            String wasmDir = getBasePath() + "/wasm/";

            File dir = new File(wasmDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            for (int i = 0; i < wasmFiles.length; i++) {
                String file = wasmFiles[i];
                String url = baseUrl + file;
                String destPath = wasmDir + file;

                // Download file
                URL urlObj = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    conn.disconnect();
                    return false;
                }

                InputStream in = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(destPath);

                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    fos.write(buffer, 0, read);
                }

                fos.close();
                in.close();
                conn.disconnect();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @JavascriptInterface
    public boolean isWasmAvailable() {
        try {
            String wasmDir = getBasePath() + "/wasm/";
            String[] files = {
                "ort-wasm-simd-threaded.wasm",
                "ort-wasm-simd.wasm", 
                "ort-wasm.wasm"
            };
            for (int i = 0; i < files.length; i++) {
                File f = new File(wasmDir + files[i]);
                if (!f.exists()) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public String getWasmPath() {
        return getBasePath() + "/wasm/";
    }

    @JavascriptInterface
    public boolean checkWasmFile(String fileName) {
        try {
            String wasmDir = getBasePath() + "/wasm/";
            File f = new File(wasmDir + fileName);
            return f.exists();
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public String getWasmFileSize(String fileName) {
        try {
            String wasmDir = getBasePath() + "/wasm/";
            File f = new File(wasmDir + fileName);
            if (f.exists()) {
                return formatSize(f.length());
            }
            return "0 B";
        } catch (Exception e) {
            return "0 B";
        }
    }

    @JavascriptInterface
    public String getWasmFileInfo(String fileName) {
        try {
            String wasmDir = getBasePath() + "/wasm/";
            File f = new File(wasmDir + fileName);
            if (!f.exists()) {
                return "{}";
            }
            JSONObject json = new JSONObject();
            json.put("name", fileName);
            json.put("path", f.getAbsolutePath());
            json.put("size", f.length());
            json.put("sizeFormatted", formatSize(f.length()));
            json.put("lastModified", f.lastModified());
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @JavascriptInterface
    public boolean deleteWasmFiles() {
        try {
            String wasmDir = getBasePath() + "/wasm/";
            String[] files = {
                "ort-wasm-simd-threaded.wasm",
                "ort-wasm-simd.wasm", 
                "ort-wasm.wasm"
            };
            for (int i = 0; i < files.length; i++) {
                File f = new File(wasmDir + files[i]);
                if (f.exists()) {
                    f.delete();
                }
            }
            File dir = new File(wasmDir);
            if (dir.exists()) {
                dir.delete();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public int getWasmFileCount() {
        try {
            String wasmDir = getBasePath() + "/wasm/";
            File dir = new File(wasmDir);
            if (!dir.exists()) {
                return 0;
            }
            File[] files = dir.listFiles();
            if (files == null) {
                return 0;
            }
            int count = 0;
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile() && files[i].getName().endsWith(".wasm")) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    // ============================================
    // ONNX / TRANSFORMERS.JS SUPPORT
    // ============================================

    @JavascriptInterface
    public String loadOnnxModel(String modelPath, String modelId) {
        try {
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                return "{\"status\":\"error\",\"message\":\"Model not found\"}";
            }

            if (modelCache.containsKey(modelId)) {
                ModelCache cache = modelCache.get(modelId);
                if (cache.modelPath.equals(modelPath)) {
                    return "{\"status\":\"cached\",\"modelId\":\"" + modelId + "\",\"size\":" + modelFile.length() + "}";
                }
            }

            JSONObject result = new JSONObject();
            result.put("status", "loaded");
            result.put("modelId", modelId);
            result.put("path", modelPath);
            result.put("size", modelFile.length());
            result.put("sizeFormatted", getFileSizeFormatted(modelPath));
            result.put("lastModified", modelFile.lastModified());

            String header = readOnnxHeader(modelPath);
            result.put("header", header);

            ModelCache cache = new ModelCache();
            cache.modelId = modelId;
            cache.modelPath = modelPath;
            cache.fileSize = modelFile.length();
            cache.lastAccess = System.currentTimeMillis();
            modelCache.put(modelId, cache);

            cleanupModelCache();

            return result.toString();
        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    @JavascriptInterface
    public String getOnnxModelInfo(String modelPath) {
        try {
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                return "{}";
            }

            JSONObject result = new JSONObject();
            result.put("path", modelPath);
            result.put("size", modelFile.length());
            result.put("sizeFormatted", getFileSizeFormatted(modelPath));
            result.put("lastModified", modelFile.lastModified());
            result.put("exists", true);
            result.put("isFile", modelFile.isFile());
            result.put("canRead", modelFile.canRead());

            String header = readOnnxHeader(modelPath);
            if (header != null && !header.isEmpty()) {
                result.put("onnxHeader", header);
            }

            return result.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private String readOnnxHeader(String path) {
        try {
            RandomAccessFile raf = new RandomAccessFile(path, "r");
            byte[] buffer = new byte[1024];
            int read = raf.read(buffer);
            raf.close();

            if (read <= 0) return "";

            if (read >= 3 && buffer[0] == 0x4F && buffer[1] == 0x4E && buffer[2] == 0x58) {
                StringBuilder sb = new StringBuilder();
                sb.append("ONNX");

                if (read >= 8) {
                    int version = ((buffer[4] & 0xFF) << 24) | ((buffer[5] & 0xFF) << 16) | 
                        ((buffer[6] & 0xFF) << 8) | (buffer[7] & 0xFF);
                    sb.append(" v").append(version);
                }

                for (int i = 0; i < read - 20; i++) {
                    if (buffer[i] == 'i' && buffer[i+1] == 'r' && buffer[i+2] == '_') {
                        int start = i;
                        int end = start;
                        while (end < read && buffer[end] != 0) end++;
                        if (end > start) {
                            byte[] strBytes = new byte[end - start];
                            System.arraycopy(buffer, start, strBytes, 0, end - start);
                            String str = new String(strBytes, "UTF-8");
                            sb.append(" ").append(str);
                        }
                        break;
                    }
                }
                return sb.toString();
            }
            return "Unknown format";
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public String readOnnxTensor(String modelPath, String tensorName) {
        try {
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                return "{\"error\":\"Model not found\"}";
            }

            JSONObject result = new JSONObject();
            result.put("status", "ready");
            result.put("tensorName", tensorName);
            result.put("modelPath", modelPath);
            result.put("modelSize", modelFile.length());

            return result.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @JavascriptInterface
    public String prepareModelForInference(String modelPath, String modelId) {
        try {
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                return "{\"status\":\"error\",\"message\":\"Model not found\"}";
            }

            JSONObject result = new JSONObject();
            result.put("status", "ready");
            result.put("modelId", modelId);
            result.put("path", modelPath);
            result.put("size", modelFile.length());
            result.put("availableMemory", Runtime.getRuntime().maxMemory());
            result.put("usedMemory", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

            ModelCache cache = modelCache.get(modelId);
            if (cache == null) {
                cache = new ModelCache();
                cache.modelId = modelId;
                cache.modelPath = modelPath;
                cache.fileSize = modelFile.length();
                modelCache.put(modelId, cache);
            }
            cache.lastAccess = System.currentTimeMillis();
            cache.isLoaded = true;

            cleanupModelCache();

            return result.toString();
        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    @JavascriptInterface
    public String getModelCacheStatus() {
        try {
            JSONObject result = new JSONObject();
            JSONArray models = new JSONArray();

            long totalSize = 0;
            ConcurrentHashMap.KeySetView<String, ModelCache> keys = (ConcurrentHashMap.KeySetView<String, StorageSupport.ModelCache>) modelCache.keySet();
            for (String key : keys) {
                ModelCache cache = modelCache.get(key);
                if (cache != null) {
                    JSONObject model = new JSONObject();
                    model.put("modelId", cache.modelId);
                    model.put("path", cache.modelPath);
                    model.put("size", cache.fileSize);
                    model.put("sizeFormatted", formatSize(cache.fileSize));
                    model.put("lastAccess", cache.lastAccess);
                    model.put("isLoaded", cache.isLoaded);
                    models.put(model);
                    totalSize += cache.fileSize;
                }
            }

            result.put("models", models);
            result.put("totalCacheSize", totalSize);
            result.put("totalCacheSizeFormatted", formatSize(totalSize));
            result.put("maxCacheSize", MAX_TOTAL_CACHE_SIZE);
            result.put("maxCacheSizeFormatted", formatSize(MAX_TOTAL_CACHE_SIZE));
            result.put("availableMemory", Runtime.getRuntime().maxMemory());
            result.put("usedMemory", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

            return result.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @JavascriptInterface
    public boolean clearModelCache(String modelId) {
        try {
            if (modelId == null || modelId.isEmpty()) {
                modelCache.clear();
                System.gc();
                return true;
            }

            ModelCache cache = modelCache.remove(modelId);
            if (cache != null) {
                System.gc();
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public String getMemoryInfo() {
        try {
            JSONObject result = new JSONObject();
            Runtime runtime = Runtime.getRuntime();

            result.put("maxMemory", runtime.maxMemory());
            result.put("totalMemory", runtime.totalMemory());
            result.put("freeMemory", runtime.freeMemory());
            result.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            result.put("maxMemoryFormatted", formatSize(runtime.maxMemory()));
            result.put("totalMemoryFormatted", formatSize(runtime.totalMemory()));
            result.put("freeMemoryFormatted", formatSize(runtime.freeMemory()));
            result.put("usedMemoryFormatted", formatSize(runtime.totalMemory() - runtime.freeMemory()));

            return result.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @JavascriptInterface
    public boolean optimizeMemory() {
        try {
            System.gc();
            System.runFinalization();
            System.gc();
            cleanupModelCache();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void cleanupModelCache() {
        try {
            long totalSize = 0;
            ConcurrentHashMap.KeySetView<String, ModelCache> keys = (ConcurrentHashMap.KeySetView<String, StorageSupport.ModelCache>) modelCache.keySet();
            for (String key : keys) {
                ModelCache cache = modelCache.get(key);
                if (cache != null) {
                    totalSize += cache.fileSize;
                }
            }

            if (totalSize > MAX_TOTAL_CACHE_SIZE) {
                java.util.List<ModelCache> sorted = new java.util.ArrayList<ModelCache>();
                keys = (ConcurrentHashMap.KeySetView<String, StorageSupport.ModelCache>) modelCache.keySet();
                for (String key : keys) {
                    ModelCache cache = modelCache.get(key);
                    if (cache != null) {
                        sorted.add(cache);
                    }
                }

                java.util.Collections.sort(sorted, new java.util.Comparator<ModelCache>() {
						@Override
						public int compare(ModelCache a, ModelCache b) {
							return Long.compare(a.lastAccess, b.lastAccess);
						}
					});

                long sizeToRemove = totalSize - MAX_TOTAL_CACHE_SIZE;
                for (int i = 0; i < sorted.size() && sizeToRemove > 0; i++) {
                    ModelCache cache = sorted.get(i);
                    modelCache.remove(cache.modelId);
                    sizeToRemove -= cache.fileSize;
                }
                System.gc();
            }
        } catch (Exception e) {}
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.2f %cB", bytes / Math.pow(1024, exp), pre);
    }

    private static class ModelCache {
        public String modelId;
        public String modelPath;
        public long fileSize;
        public long lastAccess;
        public boolean isLoaded;

        public ModelCache() {
            this.isLoaded = false;
            this.lastAccess = System.currentTimeMillis();
        }
    }

    // ============================================
    // PARALLEL DOWNLOAD
    // ============================================

    @JavascriptInterface
    public String startParallelDownload(String urlStr, String destPath, int numThreads, String callbackId) {
        String taskId = callbackId + "_" + System.currentTimeMillis();
        ParallelDownloadTask task = new ParallelDownloadTask(urlStr, destPath, numThreads, callbackId, taskId);
        downloadTasks.put(taskId, task);
        downloadExecutor.execute(task);
        return taskId;
    }

    @JavascriptInterface
    public String startDownload(String urlStr, String destPath, String callbackId) {
        return startParallelDownload(urlStr, destPath, 4, callbackId);
    }

    @JavascriptInterface
    public boolean pauseDownload(String taskId) {
        ParallelDownloadTask task = downloadTasks.get(taskId);
        if (task != null) {
            task.pause();
            return true;
        }
        return false;
    }

    @JavascriptInterface
    public boolean resumeDownload(String taskId) {
        ParallelDownloadTask task = downloadTasks.get(taskId);
        if (task != null) {
            task.resume();
            return true;
        }
        return false;
    }

    @JavascriptInterface
    public boolean cancelDownload(String taskId) {
        ParallelDownloadTask task = downloadTasks.remove(taskId);
        if (task != null) {
            task.cancel();
            return true;
        }
        return false;
    }

    @JavascriptInterface
    public String getDownloadStatus(String taskId) {
        ParallelDownloadTask task = downloadTasks.get(taskId);
        if (task == null) {
            return "{\"status\":\"not_found\"}";
        }
        return task.getStatusJson();
    }

    @JavascriptInterface
    public String getDownloadProgress(String taskId) {
        return getDownloadStatus(taskId);
    }

    @JavascriptInterface
    public String listDownloads() {
        JSONObject result = new JSONObject();
        try {
            ConcurrentHashMap.KeySetView<String, ParallelDownloadTask> keys = (ConcurrentHashMap.KeySetView<String, StorageSupport.ParallelDownloadTask>) downloadTasks.keySet();
            for (String key : keys) {
                ParallelDownloadTask task = downloadTasks.get(key);
                if (task != null) {
                    result.put(key, new JSONObject(task.getStatusJson()));
                }
            }
        } catch (Exception e) {}
        return result.toString();
    }

    // ============================================
    // PARALLEL DOWNLOAD TASK CLASS
    // ============================================

    private class ParallelDownloadTask implements Runnable {
        private String url;
        private String destPath;
        private int numThreads;
        private String callbackId;
        private String taskId;
        private String tempDir;
        private String lockFilePath;

        private volatile boolean isPaused;
        private volatile boolean isCancelled;
        private volatile boolean isRunning;
        private volatile boolean isComplete;

        private AtomicLong totalDownloaded;
        private long totalSize;
        private String status;
        private String errorMessage;
        private long startTime;
        private long speed;
        private long lastSpeedCheck;
        private long lastSpeedBytes;

        private Thread[] workerThreads;
        private boolean[] threadCompleted;
        private long[] threadDownloaded;

        public ParallelDownloadTask(String url, String destPath, int numThreads, String callbackId, String taskId) {
            this.url = url;
            this.destPath = destPath;
            this.numThreads = numThreads;
            this.callbackId = callbackId;
            this.taskId = taskId;
            this.tempDir = destPath + ".parts";
            this.lockFilePath = destPath + ".lock";
            this.isPaused = false;
            this.isCancelled = false;
            this.isRunning = false;
            this.isComplete = false;
            this.totalDownloaded = new AtomicLong(0);
            this.totalSize = -1;
            this.status = "waiting";
            this.errorMessage = "";
            this.startTime = 0;
            this.speed = 0;
            this.lastSpeedCheck = 0;
            this.lastSpeedBytes = 0;
            this.workerThreads = new Thread[numThreads];
            this.threadCompleted = new boolean[numThreads];
            this.threadDownloaded = new long[numThreads];
        }

        public void pause() {
            this.isPaused = true;
            this.status = "paused";
            sendProgress();
        }

        public void resume() {
            this.isPaused = false;
            this.status = "resuming";
            synchronized (this) {
                this.notifyAll();
            }
            sendProgress();
        }

        public void cancel() {
            this.isCancelled = true;
            this.isPaused = false;
            this.status = "cancelled";
            synchronized (this) {
                this.notifyAll();
            }
            deleteTempFiles();
            sendProgress();
        }

        public String getStatusJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("taskId", this.taskId);
                json.put("url", this.url);
                json.put("destPath", this.destPath);
                json.put("downloaded", this.totalDownloaded.get());
                json.put("totalSize", this.totalSize);
                json.put("status", this.status);
                json.put("errorMessage", this.errorMessage);
                json.put("numThreads", this.numThreads);
                json.put("isPaused", this.isPaused);
                json.put("isCancelled", this.isCancelled);
                json.put("isComplete", this.isComplete);

                long downloaded = this.totalDownloaded.get();
                if (this.totalSize > 0) {
                    json.put("progress", (double) downloaded / this.totalSize);
                    json.put("progressPercent", (downloaded * 100) / this.totalSize);
                } else {
                    json.put("progress", 0);
                    json.put("progressPercent", 0);
                }

                json.put("speed", this.speed);

                if (this.startTime > 0 && downloaded > 0) {
                    long elapsed = (System.currentTimeMillis() - this.startTime) / 1000;
                    if (elapsed > 0 && this.totalSize > downloaded) {
                        long eta = (this.totalSize - downloaded) / (downloaded / elapsed);
                        json.put("eta", eta);
                    }
                }
            } catch (Exception e) {}
            return json.toString();
        }

        private void deleteTempFiles() {
            try {
                File dir = new File(this.tempDir);
                if (dir.exists()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (int i = 0; i < files.length; i++) {
                            files[i].delete();
                        }
                    }
                    dir.delete();
                }
                new File(this.lockFilePath).delete();
            } catch (Exception e) {}
        }

        private void sendProgress() {
            if (System.currentTimeMillis() - StorageSupport.this.lastProgressUpdate < 100) {
                return;
            }
            StorageSupport.this.lastProgressUpdate = System.currentTimeMillis();

            final String jsonData = this.getStatusJson();
            if (StorageSupport.this.webViewCallback != null) {
                StorageSupport.this.webViewCallback.onProgress(jsonData);
            }
        }

        @Override
        public void run() {
            this.isRunning = true;
            this.status = "starting";
            this.startTime = System.currentTimeMillis();

            try {
                File tempDirFile = new File(this.tempDir);
                if (!tempDirFile.exists()) {
                    tempDirFile.mkdirs();
                }

                File lockFile = new File(this.lockFilePath);
                boolean isResume = lockFile.exists();

                if (isResume) {
                    loadProgress();
                } else {
                    this.totalSize = getFileSize(url);
                    if (this.totalSize <= 0) {
                        throw new IOException("Cannot determine file size");
                    }
                    writeLockFile();
                }

                long chunkSize = this.totalSize / this.numThreads;
                if (chunkSize < 1024 * 1024) {
                    chunkSize = 1024 * 1024;
                }

                this.status = "downloading";
                this.lastSpeedCheck = System.currentTimeMillis();
                this.lastSpeedBytes = 0;

                for (int i = 0; i < this.numThreads; i++) {
                    final int threadIndex = i;
                    long startByte = i * chunkSize;
                    long endByte = (i == this.numThreads - 1) ? this.totalSize - 1 : (i + 1) * chunkSize - 1;

                    if (this.threadCompleted[i]) {
                        continue;
                    }

                    final long fStartByte = startByte;
                    final long fEndByte = endByte;

                    Runnable worker = new Runnable() {
                        @Override
                        public void run() {
                            downloadChunk(threadIndex, fStartByte, fEndByte);
                        }
                    };

                    this.workerThreads[i] = new Thread(worker);
                    this.workerThreads[i].start();
                }

                waitForAllThreads();

                if (!this.isCancelled && !this.isPaused) {
                    mergeFiles();
                    deleteTempFiles();
                    this.isComplete = true;
                    this.status = "complete";
                    sendProgress();
                    if (StorageSupport.this.webViewCallback != null) {
                        StorageSupport.this.webViewCallback.onComplete(this.taskId, this.destPath);
                    }
                }

            } catch (Exception e) {
                this.status = "error";
                this.errorMessage = e.getMessage();
                deleteTempFiles();
                sendProgress();
                if (StorageSupport.this.webViewCallback != null) {
                    StorageSupport.this.webViewCallback.onError(this.taskId, this.errorMessage);
                }
            } finally {
                this.isRunning = false;
                new File(this.lockFilePath).delete();
                StorageSupport.this.downloadTasks.remove(this.taskId);
            }
        }

        private void downloadChunk(int threadIndex, long startByte, long endByte) {
            String partFile = this.tempDir + "/part_" + String.format("%05d", threadIndex) + ".tmp";
            long downloaded = 0;
            RandomAccessFile raf = null;
            HttpURLConnection conn = null;
            InputStream in = null;

            try {
                File part = new File(partFile);
                if (part.exists()) {
                    downloaded = part.length();
                    startByte += downloaded;
                }

                if (startByte > endByte) {
                    this.threadCompleted[threadIndex] = true;
                    this.threadDownloaded[threadIndex] = endByte - (startByte - downloaded);
                    updateTotalDownloaded();
                    return;
                }

                raf = new RandomAccessFile(partFile, "rw");
                raf.seek(downloaded);

                while (startByte <= endByte && !this.isCancelled && !this.isPaused) {
                    while (this.isPaused && !this.isCancelled) {
                        synchronized (ParallelDownloadTask.this) {
                            ParallelDownloadTask.this.wait(100);
                        }
                    }
                    if (this.isCancelled) break;

                    long chunkEnd = Math.min(startByte + (4 * 1024 * 1024), endByte);
                    conn = createRangeConnection(url, startByte, chunkEnd);
                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                        throw new IOException("Server returned: " + responseCode);
                    }

                    in = conn.getInputStream();
                    byte[] buffer = new byte[16384];
                    int bytesRead;
                    long bytesWritten = 0;

                    while ((bytesRead = in.read(buffer)) != -1 && !this.isCancelled) {
                        while (this.isPaused && !this.isCancelled) {
                            synchronized (ParallelDownloadTask.this) {
                                ParallelDownloadTask.this.wait(100);
                            }
                        }
                        if (this.isCancelled) break;

                        raf.write(buffer, 0, bytesRead);
                        bytesWritten += bytesRead;
                        downloaded += bytesRead;
                        startByte += bytesRead;

                        if (bytesWritten % (1024 * 1024) < 8192) {
                            this.threadDownloaded[threadIndex] = downloaded;
                            updateTotalDownloaded();
                            sendProgress();
                        }
                    }

                    if (in != null) {
                        in.close();
                        in = null;
                    }
                    if (conn != null) {
                        conn.disconnect();
                        conn = null;
                    }

                    saveThreadProgress(threadIndex, downloaded);
                }

                if (!this.isCancelled && startByte > endByte) {
                    this.threadCompleted[threadIndex] = true;
                }
                this.threadDownloaded[threadIndex] = downloaded;
                updateTotalDownloaded();

            } catch (Exception e) {
                try {
                    if (raf != null) raf.close();
                    if (in != null) in.close();
                    if (conn != null) conn.disconnect();
                    saveThreadProgress(threadIndex, downloaded);
                } catch (Exception ex) {}
            } finally {
                try {
                    if (raf != null) raf.close();
                    if (in != null) in.close();
                    if (conn != null) conn.disconnect();
                } catch (Exception e) {}
            }
        }

        private HttpURLConnection createRangeConnection(String urlStr, long start, long end) throws IOException {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept-Encoding", "identity");
            return conn;
        }

        private long getFileSize(String urlStr) throws IOException {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(30000);
            conn.connect();

            long size = conn.getContentLengthLong();

            if (size <= 0) {
                conn.disconnect();
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Range", "bytes=0-0");
                conn.connect();
                String range = conn.getHeaderField("Content-Range");
                if (range != null && range.contains("/")) {
                    String[] parts = range.split("/");
                    if (parts.length > 1) {
                        size = Long.parseLong(parts[1]);
                    }
                } else {
                    size = conn.getContentLengthLong();
                }
            }

            conn.disconnect();
            return size;
        }

        private void writeLockFile() throws IOException {
            File lockFile = new File(this.lockFilePath);
            FileWriter fw = new FileWriter(lockFile);
            fw.write(this.totalSize + "\n");
            fw.write(this.numThreads + "\n");
            for (int i = 0; i < this.numThreads; i++) {
                fw.write("0\n");
            }
            fw.close();
        }

        private void loadProgress() throws IOException {
            File lockFile = new File(this.lockFilePath);
            BufferedReader br = new BufferedReader(new FileReader(lockFile));
            this.totalSize = Long.parseLong(br.readLine().trim());
            int threads = Integer.parseInt(br.readLine().trim());
            this.numThreads = threads;

            for (int i = 0; i < threads; i++) {
                long downloaded = Long.parseLong(br.readLine().trim());
                this.threadDownloaded[i] = downloaded;
                if (downloaded >= getChunkSize(i)) {
                    this.threadCompleted[i] = true;
                }
                this.totalDownloaded.addAndGet(downloaded);
            }
            br.close();
        }

        private void saveThreadProgress(int threadIndex, long downloaded) throws IOException {
            File lockFile = new File(this.lockFilePath);
            String[] lines = new String[this.numThreads + 2];
            BufferedReader br = new BufferedReader(new FileReader(lockFile));
            lines[0] = br.readLine();
            lines[1] = br.readLine();
            for (int i = 0; i < this.numThreads; i++) {
                lines[i + 2] = br.readLine();
            }
            br.close();

            lines[threadIndex + 2] = String.valueOf(downloaded);

            FileWriter fw = new FileWriter(lockFile);
            for (int i = 0; i < lines.length; i++) {
                if (lines[i] != null) {
                    fw.write(lines[i] + "\n");
                }
            }
            fw.close();
        }

        private long getChunkSize(int threadIndex) {
            long chunkSize = this.totalSize / this.numThreads;
            if (threadIndex == this.numThreads - 1) {
                return this.totalSize - (threadIndex * chunkSize);
            }
            return chunkSize;
        }

        private void updateTotalDownloaded() {
            long total = 0;
            for (int i = 0; i < this.numThreads; i++) {
                total += this.threadDownloaded[i];
            }
            this.totalDownloaded.set(total);

            long now = System.currentTimeMillis();
            if (now - this.lastSpeedCheck >= 1000) {
                this.speed = (total - this.lastSpeedBytes) * 1000 / (now - this.lastSpeedCheck);
                this.lastSpeedCheck = now;
                this.lastSpeedBytes = total;
            }
        }

        private void waitForAllThreads() {
            boolean allDone = false;
            while (!allDone && !this.isCancelled) {
                allDone = true;
                for (int i = 0; i < this.numThreads; i++) {
                    Thread t = this.workerThreads[i];
                    if (t != null && t.isAlive()) {
                        allDone = false;
                        try {
                            t.join(100);
                        } catch (InterruptedException e) {}
                    }
                }
                while (this.isPaused && !this.isCancelled) {
                    try {
                        synchronized (this) {
                            this.wait(100);
                        }
                    } catch (InterruptedException e) {}
                }
            }
        }

        private void mergeFiles() throws IOException {
            FileOutputStream fos = new FileOutputStream(this.destPath);
            byte[] buffer = new byte[8192];
            int read;

            for (int i = 0; i < this.numThreads; i++) {
                String partFile = this.tempDir + "/part_" + String.format("%05d", i) + ".tmp";
                File part = new File(partFile);
                if (part.exists()) {
                    FileInputStream fis = new FileInputStream(part);
                    while ((read = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, read);
                    }
                    fis.close();
                }
            }
            fos.close();
        }
    }

    // ============================================
    // STORAGE METHODS
    // ============================================

    @JavascriptInterface
    public String getBasePath() {
        File folderUtama = new File(Environment.getExternalStorageDirectory(), "HTMLTools");
        if (!folderUtama.exists()) {
            folderUtama = new File("/storage/emulated/0/HTMLTools");
        }
        if (!folderUtama.exists()) {
            folderUtama.mkdirs();
        }
        return folderUtama.getAbsolutePath();
    }

    @JavascriptInterface
    public String getAppsList() {
        StringBuilder sb = new StringBuilder();
        File baseDir = new File(getBasePath());
        File[] files = baseDir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isDirectory()) {
                    File indexHtml = new File(file, "index.html");
                    if (indexHtml.exists()) {
                        sb.append(file.getName()).append("|").append(file.getAbsolutePath()).append(";");
                    }
                }
            }
        }
        return sb.toString();
    }

    @JavascriptInterface
    public String listFiles(String path) {
        StringBuilder sb = new StringBuilder();
        File dir = new File(path);
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                String type = file.isDirectory() ? "folder" : "file";
                sb.append(file.getName()).append("|").append(type).append(";");
            }
        }
        return sb.toString();
    }

    @JavascriptInterface
    public String getFileInfo(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) return "{}";
            JSONObject json = new JSONObject();
            json.put("name", file.getName());
            json.put("path", file.getAbsolutePath());
            json.put("isDirectory", file.isDirectory());
            json.put("size", file.length());
            json.put("lastModified", file.lastModified());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            json.put("lastModifiedFormatted", sdf.format(new Date(file.lastModified())));
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @JavascriptInterface
    public boolean fileExists(String path) {
        return new File(path).exists();
    }

    @JavascriptInterface
    public long getFileSize(String path) {
        File file = new File(path);
        return file.exists() ? file.length() : 0;
    }

    @JavascriptInterface
    public String getFileSizeFormatted(String path) {
        File file = new File(path);
        if (!file.exists()) return "0 B";
        return formatSize(file.length());
    }

    @JavascriptInterface
    public long getFileModified(String path) {
        File file = new File(path);
        return file.exists() ? file.lastModified() : 0;
    }

    @JavascriptInterface
    public String readFile(String path) {
        try {
            File file = new File(path);
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public boolean writeFile(String path, String content) {
        try {
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            FileWriter fw = new FileWriter(file, false);
            fw.write(content);
            fw.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean appendFile(String path, String content) {
        try {
            File file = new File(path);
            FileWriter fw = new FileWriter(file, true);
            fw.write(content);
            fw.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean createFolder(String path) {
        File file = new File(path);
        return file.mkdirs() || file.exists();
    }

    @JavascriptInterface
    public boolean deleteFile(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    deleteFile(files[i].getAbsolutePath());
                }
            }
        }
        return file.delete();
    }

    @JavascriptInterface
    public boolean renameFile(String oldPath, String newPath) {
        File oldFile = new File(oldPath);
        File newFile = new File(newPath);
        return oldFile.renameTo(newFile);
    }

    @JavascriptInterface
    public boolean copyFile(String srcPath, String destPath) {
        try {
            InputStream in = new FileInputStream(srcPath);
            OutputStream out = new FileOutputStream(destPath);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean moveFile(String srcPath, String destPath) {
        if (copyFile(srcPath, destPath)) {
            return new File(srcPath).delete();
        }
        return false;
    }

    @JavascriptInterface
    public String readBinary(String path) {
        try {
            File file = new File(path);
            byte[] buffer = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(buffer);
            fis.close();
            return Base64.encodeToString(buffer, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public boolean writeBinary(String path, String base64Data) {
        try {
            byte[] data = Base64.decode(base64Data, Base64.NO_WRAP);
            FileOutputStream fos = new FileOutputStream(path);
            fos.write(data);
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public String readBytes(String path, String offsetHex, int length) {
        try {
            int offset = Integer.parseInt(offsetHex.replace("0x", ""), 16);
            RandomAccessFile raf = new RandomAccessFile(path, "r");
            raf.seek(offset);
            byte[] buffer = new byte[length];
            int read = raf.read(buffer);
            raf.close();
            if (read <= 0) return "";
            byte[] actualData = new byte[read];
            System.arraycopy(buffer, 0, actualData, 0, read);
            return Base64.encodeToString(actualData, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public boolean writeBytes(String path, String offsetHex, String base64Data) {
        try {
            int offset = Integer.parseInt(offsetHex.replace("0x", ""), 16);
            byte[] data = Base64.decode(base64Data, Base64.NO_WRAP);
            RandomAccessFile raf = new RandomAccessFile(path, "rw");
            raf.seek(offset);
            raf.write(data);
            raf.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public String readHex(String path, String offsetHex, int length) {
        try {
            int offset = Integer.parseInt(offsetHex.replace("0x", ""), 16);
            RandomAccessFile raf = new RandomAccessFile(path, "r");
            raf.seek(offset);
            byte[] buffer = new byte[length];
            int read = raf.read(buffer);
            raf.close();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < read; i++) {
                sb.append(String.format("%02X", buffer[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "ERROR";
        }
    }

    @JavascriptInterface
    public boolean patchHex(String path, String offsetHex, String hexValue) {
        try {
            int offset = Integer.parseInt(offsetHex.replace("0x", ""), 16);
            byte[] valueBytes = new byte[hexValue.length() / 2];
            for (int i = 0; i < valueBytes.length; i++) {
                valueBytes[i] = (byte) Integer.parseInt(hexValue.substring(i * 2, i * 2 + 2), 16);
            }
            RandomAccessFile raf = new RandomAccessFile(path, "rw");
            raf.seek(offset);
            raf.write(valueBytes);
            raf.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public long getFileLength(String path) {
        return getFileSize(path);
    }

    @JavascriptInterface
    public boolean extractChunk(String src, String startHex, String endHex, String dest) {
        try {
            long start = Long.parseLong(startHex.replace("0x", ""), 16);
            long end = Long.parseLong(endHex.replace("0x", ""), 16);
            long length = end - start;
            if (length <= 0) return false;

            RandomAccessFile rafSrc = new RandomAccessFile(src, "r");
            FileOutputStream fosDst = new FileOutputStream(dest);
            rafSrc.seek(start);

            byte[] buffer = new byte[8192];
            long bytesReadTotal = 0;
            int read;
            while (bytesReadTotal < length && (read = rafSrc.read(buffer, 0, (int) Math.min(buffer.length, length - bytesReadTotal))) > 0) {
                fosDst.write(buffer, 0, read);
                bytesReadTotal += read;
            }
            rafSrc.close();
            fosDst.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean appendBytes(String src, String dest, String startHex, int length) {
        try {
            long start = Long.parseLong(startHex.replace("0x", ""), 16);
            RandomAccessFile rafSrc = new RandomAccessFile(src, "r");
            RandomAccessFile rafDst = new RandomAccessFile(dest, "rw");
            rafSrc.seek(start);
            rafDst.seek(rafDst.length());

            byte[] buffer = new byte[length];
            int read = rafSrc.read(buffer);
            if (read > 0) {
                rafDst.write(buffer, 0, read);
            }
            rafSrc.close();
            rafDst.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public String encrypt(String teks, String password) {
        try {
            byte[] txtBytes = teks.getBytes("UTF-8");
            byte[] keyBytes = password.getBytes("UTF-8");
            byte[] result = new byte[txtBytes.length];
            for (int i = 0; i < txtBytes.length; i++) {
                result[i] = (byte) (txtBytes[i] ^ keyBytes[i % keyBytes.length]);
            }
            return Base64.encodeToString(result, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public String decrypt(String teksEnkripsi, String password) {
        try {
            byte[] encBytes = Base64.decode(teksEnkripsi, Base64.NO_WRAP);
            byte[] keyBytes = password.getBytes("UTF-8");
            byte[] result = new byte[encBytes.length];
            for (int i = 0; i < encBytes.length; i++) {
                result[i] = (byte) (encBytes[i] ^ keyBytes[i % keyBytes.length]);
            }
            return new String(result, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public boolean xorFile(String path, String keyHex, String outputPath) {
        try {
            byte[] key = new byte[keyHex.length() / 2];
            for (int i = 0; i < key.length; i++) {
                key[i] = (byte) Integer.parseInt(keyHex.substring(i * 2, i * 2 + 2), 16);
            }
            FileInputStream fis = new FileInputStream(path);
            FileOutputStream fos = new FileOutputStream(outputPath);
            byte[] buffer = new byte[8192];
            int read;
            int keyIndex = 0;
            while ((read = fis.read(buffer)) > 0) {
                for (int i = 0; i < read; i++) {
                    buffer[i] ^= key[keyIndex % key.length];
                    keyIndex++;
                }
                fos.write(buffer, 0, read);
            }
            fis.close();
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public String getMD5(String path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            InputStream is = new FileInputStream(path);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < md5sum.length; i++) {
                sb.append(Integer.toString((md5sum[i] & 0xff) + 0x100, 16).substring(1));
            }
            is.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public String getSHA1(String path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            InputStream is = new FileInputStream(path);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] sha1sum = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sha1sum.length; i++) {
                sb.append(Integer.toString((sha1sum[i] & 0xff) + 0x100, 16).substring(1));
            }
            is.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public String getFileHash(String path) {
        return getMD5(path);
    }

    @JavascriptInterface
    public boolean zip(String srcPath, String destZipPath) {
        try {
            FileOutputStream fos = new FileOutputStream(destZipPath);
            ZipOutputStream zos = new ZipOutputStream(fos);
            File srcFile = new File(srcPath);
            addFileToZip(zos, srcFile, "");
            zos.close();
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void addFileToZip(ZipOutputStream zos, File fileToZip, String baseName) throws IOException {
        if (fileToZip.isDirectory()) {
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    addFileToZip(zos, children[i], baseName + fileToZip.getName() + "/");
                }
            }
        } else {
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(baseName + fileToZip.getName());
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[4096];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            fis.close();
        }
    }

    @JavascriptInterface
    public boolean unzip(String zipPath, String destDirPath) {
        try {
            File destDir = new File(destDirPath);
            if (!destDir.exists()) destDir.mkdirs();
            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipPath));
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String filePath = destDirPath + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    File file = new File(filePath);
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] bytesIn = new byte[4096];
                    int read;
                    while ((read = zipIn.read(bytesIn)) != -1) {
                        fos.write(bytesIn, 0, read);
                    }
                    fos.close();
                } else {
                    File dir = new File(filePath);
                    dir.mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean unpackJar(String jarPath, String destPath) {
        return unzip(jarPath, destPath);
    }

    @JavascriptInterface
    public boolean repackJar(String folderPath, String jarPath) {
        return zip(folderPath, jarPath);
    }

    @JavascriptInterface
    public String zlibInflate(String base64Data) {
        try {
            byte[] input = Base64.decode(base64Data, Base64.NO_WRAP);
            Inflater decompressor = new Inflater();
            decompressor.setInput(input);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
            byte[] buf = new byte[1024];
            while (!decompressor.finished()) {
                int count = decompressor.inflate(buf);
                bos.write(buf, 0, count);
            }
            bos.close();
            return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public String zlibDeflate(String base64Data) {
        try {
            byte[] input = Base64.decode(base64Data, Base64.NO_WRAP);
            Deflater compressor = new Deflater();
            compressor.setInput(input);
            compressor.finish();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
            byte[] buf = new byte[1024];
            while (!compressor.finished()) {
                int count = compressor.deflate(buf);
                bos.write(buf, 0, count);
            }
            bos.close();
            return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public String gzipCompress(String base64Data) {
        try {
            byte[] data = Base64.decode(base64Data, Base64.NO_WRAP);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(bos);
            gos.write(data);
            gos.close();
            bos.close();
            return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public String gzipDecompress(String base64Data) {
        try {
            byte[] data = Base64.decode(base64Data, Base64.NO_WRAP);
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            GZIPInputStream gis = new GZIPInputStream(bis);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = gis.read(buf)) > 0) {
                bos.write(buf, 0, len);
            }
            gis.close();
            bos.close();
            return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public String extractStringsFromBinary(String path) {
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream fis = new FileInputStream(path);
            byte[] buffer = new byte[8192];
            int read;
            StringBuilder currentWord = new StringBuilder();
            while ((read = fis.read(buffer)) > 0) {
                for (int i = 0; i < read; i++) {
                    char c = (char) buffer[i];
                    if (c >= 32 && c <= 126) {
                        currentWord.append(c);
                    } else {
                        if (currentWord.length() >= 4) {
                            sb.append(currentWord.toString()).append("\n");
                        }
                        currentWord.setLength(0);
                    }
                }
            }
            fis.close();
        } catch (Exception ignored) {}
        return sb.toString();
    }

    @JavascriptInterface
    public String imageToBase64(String path) {
        try {
            File file = new File(path);
            String ext = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
            String mime = "image/png";
            if (ext.equals("jpg") || ext.equals("jpeg")) mime = "image/jpeg";
            else if (ext.equals("gif")) mime = "image/gif";
            else if (ext.equals("webp")) mime = "image/webp";

            byte[] buf = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(buf);
            fis.close();
            return "data:" + mime + ";base64," + Base64.encodeToString(buf, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public String readCSV(String path, String separator) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            JSONArray array = new JSONArray();
            String headerLine = br.readLine();
            if (headerLine == null) {
                br.close();
                return "[]";
            }
            String[] headers = headerLine.split(separator);
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(separator);
                JSONObject obj = new JSONObject();
                for (int i = 0; i < headers.length; i++) {
                    if (i < values.length) obj.put(headers[i], values[i]);
                }
                array.put(obj);
            }
            br.close();
            return array.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    @JavascriptInterface
    public boolean dbInsert(String path, String jsonString) {
        try {
            String content = readFile(path);
            JSONArray array;
            if (content.startsWith("ERROR") || content.trim().isEmpty()) {
                array = new JSONArray();
            } else {
                array = new JSONArray(content);
            }
            array.put(new JSONObject(jsonString));
            return writeFile(path, array.toString());
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public String dbQuery(String path, String key, String value) {
        try {
            JSONArray array = new JSONArray(readFile(path));
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.has(key) && obj.getString(key).equalsIgnoreCase(value)) {
                    return obj.toString();
                }
            }
        } catch (Exception ignored) {}
        return "{}";
    }

    @JavascriptInterface
    public boolean dbUpdateField(String path, String idKey, String idValue, String targetKey, String newValue) {
        try {
            JSONArray array = new JSONArray(readFile(path));
            boolean changed = false;
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.has(idKey) && obj.getString(idKey).equalsIgnoreCase(idValue)) {
                    obj.put(targetKey, newValue);
                    changed = true;
                }
            }
            if (changed) return writeFile(path, array.toString());
        } catch (Exception ignored) {}
        return false;
    }

    @JavascriptInterface
    public boolean splitFile(String srcPath, String destDir, long chunkSize) {
        try {
            File srcFile = new File(srcPath);
            if (!srcFile.exists()) return false;

            File destFolder = new File(destDir);
            if (!destFolder.exists()) destFolder.mkdirs();

            RandomAccessFile raf = new RandomAccessFile(srcFile, "r");
            long fileSize = raf.length();
            long totalChunks = (fileSize + chunkSize - 1) / chunkSize;
            byte[] buffer = new byte[(int) Math.min(8192, chunkSize)];

            for (int i = 0; i < totalChunks; i++) {
                long offset = i * chunkSize;
                long remaining = Math.min(chunkSize, fileSize - offset);
                File partFile = new File(destDir, srcFile.getName() + ".part" + String.format("%05d", i));
                FileOutputStream fos = new FileOutputStream(partFile);
                raf.seek(offset);

                while (remaining > 0) {
                    int toRead = (int) Math.min(buffer.length, remaining);
                    int read = raf.read(buffer, 0, toRead);
                    if (read <= 0) break;
                    fos.write(buffer, 0, read);
                    remaining -= read;
                }
                fos.close();
            }
            raf.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean joinFiles(String[] parts, String outputPath) {
        try {
            FileOutputStream fos = new FileOutputStream(outputPath);
            byte[] buffer = new byte[8192];
            int read;
            for (int i = 0; i < parts.length; i++) {
                FileInputStream fis = new FileInputStream(parts[i]);
                while ((read = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, read);
                }
                fis.close();
            }
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean streamCopy(String srcPath, String destPath, long startOffset, long length) {
        try {
            RandomAccessFile rafSrc = new RandomAccessFile(srcPath, "r");
            RandomAccessFile rafDst = new RandomAccessFile(destPath, "rw");

            rafSrc.seek(startOffset);
            rafDst.seek(rafDst.length());

            byte[] buffer = new byte[8192];
            long remaining = length;
            int read;

            while (remaining > 0 && (read = rafSrc.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
                rafDst.write(buffer, 0, read);
                remaining -= read;
            }

            rafSrc.close();
            rafDst.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public long getFileOffset(String path) {
        File f = new File(path);
        return f.exists() ? f.length() : 0;
    }

    @JavascriptInterface
    public boolean setFileLength(String path, long length) {
        try {
            RandomAccessFile raf = new RandomAccessFile(path, "rw");
            raf.setLength(length);
            raf.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean hasFileChanged(String path) {
        File f = new File(path);
        if (!f.exists()) return false;
        long currentMod = f.lastModified();
        if (!lastCheckMap.containsKey(path)) {
            lastCheckMap.put(path, currentMod);
            return false;
        }
        long lastMod = lastCheckMap.get(path);
        if (currentMod > lastMod) {
            lastCheckMap.put(path, currentMod);
            return true;
        }
        return false;
    }

    @JavascriptInterface
    public long getFolderSize(String path) {
        long size = 0;
        File dir = new File(path);
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isDirectory()) {
                    size += getFolderSize(file.getAbsolutePath());
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }

    @JavascriptInterface
    public boolean createFile(String path, String content) {
        return writeFile(path, content);
    }

    @JavascriptInterface
    public String downloadChunk(String urlStr, String destPath, long startByte, long endByte) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            String rangeEnd = (endByte > 0) ? String.valueOf(endByte) : "";
            conn.setRequestProperty("Range", "bytes=" + startByte + "-" + rangeEnd);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                return "ERROR: Response code " + responseCode;
            }

            File destFile = new File(destPath);
            File parent = destFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
            raf.seek(startByte);

            InputStream in = conn.getInputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;
            long expected = endByte > 0 ? (endByte - startByte + 1) : -1;

            while ((bytesRead = in.read(buffer)) != -1) {
                raf.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                if (expected > 0 && totalRead >= expected) break;
            }

            in.close();
            raf.close();
            conn.disconnect();

            return "SUCCESS:" + totalRead;
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public boolean mergeFilesArray(String[] fileParts, String outputPath) {
        return joinFiles(fileParts, outputPath);
    }
}
