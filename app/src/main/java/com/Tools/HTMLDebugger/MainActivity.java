package com.Tools.HTMLDebugger;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import org.json.JSONObject;

public class MainActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;
    private static final int STORAGE_PERMISSION_REQ_CODE = 5678;
    private static final int MANAGE_STORAGE_PERMISSION_REQ_CODE = 9012;

    private ListView listViewTarget;
    private ArrayList<String> namaAplikasi;
    private ArrayList<String> pathIndexHtml;
    private File folderUtama;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        namaAplikasi = new ArrayList<String>();
        pathIndexHtml = new ArrayList<String>();

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(20, 20, 20, 20);
        rootLayout.setBackgroundColor(0xFF1E1E2E);

        TextView txtJudul = new TextView(this);
        txtJudul.setText("DAFTAR APLIKASI HTML / FFOS:");
        txtJudul.setTextColor(0xFF89B4FA);
        txtJudul.setTextSize(18);
        txtJudul.setPadding(0, 0, 0, 20);
        rootLayout.addView(txtJudul);

        listViewTarget = new ListView(this);
        listViewTarget.setOnItemClickListener(this);
        rootLayout.addView(listViewTarget);

        setContentView(rootLayout);

        folderUtama = new File(Environment.getExternalStorageDirectory(), "HTMLTools");
        if (!folderUtama.exists()) {
            folderUtama = new File("/storage/emulated/0/HTMLTools/");
        }

        mintaIzinStorageDanOverlay();
    }

    private void mintaIzinStorageDanOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_REQ_CODE);
                Toast.makeText(this, "Izinkan akses ke semua file!", Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
									   Manifest.permission.READ_EXTERNAL_STORAGE,
									   Manifest.permission.WRITE_EXTERNAL_STORAGE
								   }, STORAGE_PERMISSION_REQ_CODE);
                return;
            }
        }
        buatFolderDanCekOverlay();
    }

    private void buatFolderDanCekOverlay() {
        if (!folderUtama.exists()) {
            if (folderUtama.mkdirs()) {
                Toast.makeText(this, "Folder HTMLTools dibuat!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Gagal membuat folder HTMLTools!", Toast.LENGTH_LONG).show();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
										   Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
                Toast.makeText(this, "Izinkan aplikasi untuk tampil di atas aplikasi lain!", Toast.LENGTH_LONG).show();
                return;
            }
        }
        scanDanMuatAplikasiHtml();
    }

    private void scanDanMuatAplikasiHtml() {
        namaAplikasi.clear();
        pathIndexHtml.clear();

        if (folderUtama.exists() && folderUtama.isDirectory()) {
            File[] subFolders = folderUtama.listFiles();
            if (subFolders != null) {
                for (int i = 0; i < subFolders.length; i++) {
                    File subFolder = subFolders[i];
                    if (subFolder.isDirectory()) {
                        File manifestFile = new File(subFolder, "manifest.webapp");
                        File indexFile = new File(subFolder, "index.html");

                        if (manifestFile.exists()) {
                            String launchPath = getLaunchPathFromManifest(manifestFile);
                            File targetFile = new File(subFolder, launchPath);
                            if (targetFile.exists()) {
                                namaAplikasi.add("[FFOS] " + subFolder.getName());
                                pathIndexHtml.add(targetFile.getAbsolutePath());
                            }
                        } else if (indexFile.exists()) {
                            namaAplikasi.add(subFolder.getName());
                            pathIndexHtml.add(indexFile.getAbsolutePath());
                        }
                    }
                }
            }
        }

        if (namaAplikasi.isEmpty()) {
            namaAplikasi.add("Tiada aplikasi dijumpai di /HTMLTools/");
            pathIndexHtml.add("");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, namaAplikasi);
        listViewTarget.setAdapter(adapter);
    }

    private String getLaunchPathFromManifest(File manifestFile) {
        String launchPath = "index.html";
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(manifestFile);
            byte[] data = new byte[(int) manifestFile.length()];
            fis.read(data);
            fis.close();

            String jsonContent = new String(data, "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonContent);

            if (jsonObject.has("launch_path")) {
                launchPath = jsonObject.getString("launch_path");
                if (launchPath.startsWith("/")) {
                    launchPath = launchPath.substring(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return launchPath;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                buatFolderDanCekOverlay();
            } else {
                Toast.makeText(this, "Aplikasi membutuhkan izin penyimpanan!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MANAGE_STORAGE_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    buatFolderDanCekOverlay();
                } else {
                    Toast.makeText(this, "Akses penyimpanan diperlukan!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        } else if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    scanDanMuatAplikasiHtml();
                } else {
                    Toast.makeText(this, "Izin overlay diperlukan!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        } else if (requestCode == STORAGE_PERMISSION_REQ_CODE) {
            buatFolderDanCekOverlay();
        }
    }

    @Override
    public void onClick(View v) {
        // Tidak digunakan, tapi diperlukan karena implement View.OnClickListener
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String selectedPath = pathIndexHtml.get(position);
        if (selectedPath != null && !selectedPath.equals("")) {
            Intent serviceIntent = new Intent(MainActivity.this, FloatingService.class);
            serviceIntent.putExtra("PATH_HTML", selectedPath);
            startService(serviceIntent);
            finish();
        }
    }
}
