package com.moto.pstar.nrswitcher.lsposed

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class Sim2NetworkTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        renderInitialTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        // 若正在点击切换中，不执行轮询覆盖，防止 UI 闪烁回弹
        if (!NetworkHelper.isSwitchingSim2 && System.currentTimeMillis() - NetworkHelper.lastSwitchTimeSim2 > 2000L) {
            refreshTileAsync()
        }
    }

    override fun onClick() {
        super.onClick()
        try {
            NetworkHelper.isSwitchingSim2 = true
            NetworkHelper.lastSwitchTimeSim2 = System.currentTimeMillis()

            val willBeActive = qsTile?.state != Tile.STATE_ACTIVE
            qsTile?.let { tile ->
                tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_sim2)
                tile.state = if (willBeActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.label = "SIM 2 5G/4G"
                tile.subtitle = if (willBeActive) "切换中: 5G[数据]..." else "切换中: 4G..."
                tile.updateTile()
            }

            // SIM 2 对应 slotIndex 1 (Phone 1)
            NetworkHelper.toggleSimNetwork(applicationContext, 1) { is5G, _ ->
                qsTile?.let { tile ->
                    tile.state = if (is5G) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    tile.label = "SIM 2 5G/4G"
                    tile.subtitle = if (is5G) "5G 优先 [数据]" else "4G 优先 (LTE)"
                    tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_sim2)
                    tile.updateTile()
                }
            }
        } catch (e: Exception) {
            Log.e("Sim2Tile", "onClick 异常", e)
        }
    }

    private fun renderInitialTile() {
        val tile = qsTile ?: return
        tile.label = "SIM 2 5G/4G"
        tile.subtitle = "SIM 2"
        tile.state = Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_sim2)
        tile.updateTile()
    }

    private fun refreshTileAsync() {
        Thread {
            try {
                val is5G = NetworkHelper.is5GEnabled(1)
                qsTile?.let { tile ->
                    tile.state = if (is5G) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    tile.label = "SIM 2 5G/4G"
                    tile.subtitle = if (is5G) "5G 优先 [数据]" else "4G 优先 (LTE)"
                    tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_sim2)
                    tile.updateTile()
                }
            } catch (e: Exception) {
                Log.e("Sim2Tile", "refreshTileAsync 异常", e)
            }
        }.start()
    }
}