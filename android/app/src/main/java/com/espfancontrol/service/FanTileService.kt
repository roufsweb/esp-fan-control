package com.espfancontrol.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class FanTileService : TileService() {

    private var isFanActive = true

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        isFanActive = !isFanActive
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.state = if (isFanActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Fan"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isFanActive) "Active" else "Off"
        }
        tile.updateTile()
    }
}

