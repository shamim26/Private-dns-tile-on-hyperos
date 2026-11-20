package com.shamim.privatedns;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private TextView tvStatus;
    private TextView tvProvider;
    private TextView tvPermissionWarning;
    private Button btnToggle;
    private Button btnAddTile;
    private Button btnOpenSettings;

    private static final String TILE_SPEC = "custom(com.shamim.privatedns/.PrivateDnsTileService)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvProvider = findViewById(R.id.tvProvider);
        tvPermissionWarning = findViewById(R.id.tvPermissionWarning);
        btnToggle = findViewById(R.id.btnToggle);
        btnAddTile = findViewById(R.id.btnAddTile);
        btnOpenSettings = findViewById(R.id.btnOpenSettings);

        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDns();
            }
        });

        btnAddTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                injectTileViaRoot();
            }
        });

        btnOpenSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDnsSettings();
            }
        });

        tvPermissionWarning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestRootSetup();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshState();
        requestRootSetup();
    }

    private void requestRootSetup() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Grant WRITE_SECURE_SETTINGS via root
                    Process p1 = Runtime.getRuntime().exec(new String[]{"su", "-c", "pm grant com.shamim.privatedns android.permission.WRITE_SECURE_SETTINGS"});
                    p1.waitFor();

                    // Read current tiles using root
                    Process pGet = Runtime.getRuntime().exec(new String[]{"su", "-c", "settings get secure sysui_qs_tiles"});
                    BufferedReader reader = new BufferedReader(new InputStreamReader(pGet.getInputStream()));
                    String currentTiles = reader.readLine();
                    pGet.waitFor();

                    if (currentTiles != null && !currentTiles.contains(TILE_SPEC)) {
                        String newTiles;
                        if (currentTiles.contains(",edit")) {
                            newTiles = currentTiles.replace(",edit", "," + TILE_SPEC + ",edit");
                        } else {
                            newTiles = currentTiles + "," + TILE_SPEC;
                        }
                        Process p2 = Runtime.getRuntime().exec(new String[]{"su", "-c", "settings put secure sysui_qs_tiles '" + newTiles + "'"});
                        p2.waitFor();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshState();
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    private void injectTileViaRoot() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process pGet = Runtime.getRuntime().exec(new String[]{"su", "-c", "settings get secure sysui_qs_tiles"});
                    BufferedReader reader = new BufferedReader(new InputStreamReader(pGet.getInputStream()));
                    String currentTiles = reader.readLine();
                    pGet.waitFor();

                    if (currentTiles == null) currentTiles = "";
                    if (!currentTiles.contains(TILE_SPEC)) {
                        String newTiles = currentTiles.contains(",edit") ? currentTiles.replace(",edit", "," + TILE_SPEC + ",edit") : currentTiles + "," + TILE_SPEC;
                        Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "settings put secure sysui_qs_tiles '" + newTiles + "'"});
                        p.waitFor();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Tile added to Control Center!", Toast.LENGTH_SHORT).show();
                            refreshState();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Root required to add tile", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void refreshState() {
        try {
            boolean hasPermission = checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED;
            tvPermissionWarning.setVisibility(hasPermission ? View.GONE : View.VISIBLE);
        } catch (Exception ignored) {
        }

        try {
            String currentTiles = null;
            try {
                currentTiles = Settings.Secure.getString(getContentResolver(), "sysui_qs_tiles");
            } catch (Exception ignored) {
            }
            boolean tilePresent = currentTiles != null && currentTiles.contains(TILE_SPEC);
            btnAddTile.setText(tilePresent ? "Tile is Active in Control Center" : "Force Add Tile to Control Center");
            btnAddTile.setEnabled(!tilePresent);
        } catch (Exception ignored) {
        }

        try {
            String mode = Settings.Global.getString(getContentResolver(), "private_dns_mode");
            String specifier = Settings.Global.getString(getContentResolver(), "private_dns_specifier");

            if ("hostname".equalsIgnoreCase(mode)) {
                tvStatus.setText("Status: ACTIVE (Custom DNS)");
                tvStatus.setTextColor(0xFF4CAF50);
                btnToggle.setText("Turn OFF Private DNS");
            } else if ("opportunistic".equalsIgnoreCase(mode)) {
                tvStatus.setText("Status: AUTOMATIC");
                tvStatus.setTextColor(0xFFFF9800);
                btnToggle.setText("Turn ON Custom DNS");
            } else {
                tvStatus.setText("Status: OFF");
                tvStatus.setTextColor(0xFFF44336);
                btnToggle.setText("Turn ON Custom DNS");
            }

            if (specifier != null && !specifier.trim().isEmpty()) {
                tvProvider.setText("Current Hostname: " + specifier.trim());
            } else {
                tvProvider.setText("Current Hostname: None configured");
            }
        } catch (Exception e) {
            tvStatus.setText("Status: Error reading settings");
        }
    }

    private void toggleDns() {
        try {
            String currentMode = Settings.Global.getString(getContentResolver(), "private_dns_mode");
            String targetMode = "hostname".equalsIgnoreCase(currentMode) ? "off" : "hostname";

            try {
                Settings.Global.putString(getContentResolver(), "private_dns_mode", targetMode);
            } catch (SecurityException se) {
                Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "settings put global private_dns_mode " + targetMode});
                p.waitFor();
            }
            refreshState();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openDnsSettings() {
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        if (intent.resolveActivity(getPackageManager()) == null) {
            intent = new Intent(Settings.ACTION_SETTINGS);
        }
        startActivity(intent);
    }
}
