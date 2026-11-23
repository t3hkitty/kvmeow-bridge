package dev.neko.kvmeow.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

/**
 * ☕ Android Quick Settings Tile for 1-Tap Caffeine Keep-Alive Jitter ☕
 */
@RequiresApi(Build.VERSION_CODES.N)
class CaffeineTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent(KvmeowBridgeService.ACTION_TOGGLE_CAFFEINE)
        sendBroadcast(intent)
        updateTileState()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = "Caffeine Jitter"
        tile.updateTile()
    }
}
