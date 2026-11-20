package com.shamim.privatedns;

import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

public class PrivateDnsTileService extends TileService {

    private static final String MODE_OFF = "off";
    private static final String MODE_HOSTNAME = "hostname";
    private static final String MODE_OPPORTUNISTIC = "opportunistic";

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        try {
            String currentMode = Settings.Global.getString(getContentResolver(), "private_dns_mode");
            String targetMode = MODE_HOSTNAME.equalsIgnoreCase(currentMode) ? MODE_OFF : MODE_HOSTNAME;
            
            try {
                Settings.Global.putString(getContentResolver(), "private_dns_mode", targetMode);
            } catch (SecurityException se) {
                // Fallback to su if WRITE_SECURE_SETTINGS wasn't granted yet
                Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "settings put global private_dns_mode " + targetMode});
                p.waitFor();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to toggle Private DNS", Toast.LENGTH_SHORT).show();
        }
        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        try {
            String mode = Settings.Global.getString(getContentResolver(), "private_dns_mode");
            String specifier = Settings.Global.getString(getContentResolver(), "private_dns_specifier");

            if (MODE_HOSTNAME.equalsIgnoreCase(mode)) {
                tile.setState(Tile.STATE_ACTIVE);
                if (specifier != null && !specifier.trim().isEmpty()) {
                    tile.setSubtitle(specifier.trim());
                } else {
                    tile.setSubtitle("Active");
                }
            } else if (MODE_OPPORTUNISTIC.equalsIgnoreCase(mode)) {
                tile.setState(Tile.STATE_INACTIVE);
                tile.setSubtitle("Automatic");
            } else {
                tile.setState(Tile.STATE_INACTIVE);
                tile.setSubtitle("Off");
            }
        } catch (Exception e) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setSubtitle("Error");
        }

        tile.updateTile();
    }
}
