package com.moto.pstar.nrswitcher.lsposed

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class Sim1NetworkTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        renderInitialTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        // 若正在点击切换中，不执行轮询覆盖，防止 UI 闪烁回弹
        if (!NetworkHelper.isSwitchingSim1 && System.currentTimeMillis() - NetworkHelper.lastSwitchTimeSim1 > 2000L) {
            refreshTileAsync()
        }
    }

    override fun onClick() {
        super.onClick()
        try {
            NetworkHelper.isSwitchingSim1 = true
            NetworkHelper.lastSwitchTimeSim1 = System.currentTimeMillis()

            // 点击时即刻改变 UI 动画，增强反馈感
            val willBeActive = qsTile?.state != Tile.STATE_ACTIVE
            qsTile?.let { tile ->
                tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_sim1)
                tile.state = if (willBeActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.label = "SIM 1 5G/4G"
                tile.subtitle = if (willBeActive) "切换中: 5G..." else "切换中: 4G..."
                tile.updateTile()
            }

            // SIM 1 对应 slotIndex 0 (Phone 0)
            NetworkHelper.toggleSimNetwork(applicationContext, 0) { is5G, _ ->
                qsTile?.let { tile ->
                    tile.state = if (is5G) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    tile.label = "SIM 1 5G/4G"
                    tile.subtitle = if (is5G) "5G 优先 (NR)" else "4G 优先 (LTE)"
                    tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_sim1)
                    tile.updateTile()
                }
            }
        } catch (e: Exception) {
            Log.e("Sim1Tile", "onClick 异常", e)
        }
    }

    private fun renderInitialTile() {
        val tile = qsTile ?: return
        tile.label = "SIM 1 5G/4G"
        tile.subtitle = "SIM 1"
        tile.state = Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_sim1)
        tile.updateTile()
    }

    private fun refreshTileAsync() {
        Thread {
            try {
                val is5G = NetworkHelper.is5GEnabled(0)
                qsTile?.let { tile ->
                    tile.state = if (is5G) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    tile.label = "SIM 1 5G/4G"
                    tile.subtitle = if (is5G) "5G 优先 (NR)" else "4G 优先 (LTE)"
                    tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_sim1)
                    tile.updateTile()
                }
            } catch (e: Exception) {
                Log.e("Sim1Tile", "refreshTileAsync 异常", e)
            }
        }.start()
    }
}