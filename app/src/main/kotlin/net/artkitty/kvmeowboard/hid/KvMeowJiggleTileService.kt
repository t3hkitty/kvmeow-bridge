package net.artkitty.kvmeowboard.hid

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

/**
 * ☕ KvMeowJiggleTileService ☕
 * Android Quick Settings Tile ("KV Jiggle") to toggle zero-net-displacement
 * background mouse jiggling across all configured hosts without opening the app.
 */
class KvMeowJiggleTileService : TileService() {

    private var isJiggling = false

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        isJiggling = !isJiggling

        val action = if (isJiggling) {
            KvMeowHidService.ACTION_START_JIGGLE
        } else {
            KvMeowHidService.ACTION_STOP_JIGGLE
        }

        val intent = Intent(this, KvMeowHidService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(this, intent)

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.state = if (isJiggling) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isJiggling) "Jiggle ON" else "Jiggle OFF"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isJiggling) "Anti-Idle Active" else "KVMeowboard"
        }
        tile.updateTile()
    }
}
