package com.leitorvoz.atalhos

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.leitorvoz.leitura.Leitura
import com.leitorvoz.leitura.Situacao

/** Botão nas configurações rápidas: lê o que foi copiado ou para a leitura. */
class BotaoRapido : TileService() {

    override fun onStartListening() {
        qsTile?.apply {
            state = if (Leitura.situacao.value == Situacao.PARADO) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            updateTile()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        if (Leitura.situacao.value != Situacao.PARADO) {
            Leitura.comando(this, Leitura.ACAO_PARAR)
            qsTile?.apply { state = Tile.STATE_INACTIVE; updateTile() }
            return
        }
        val i = Intent(this, LerActivity::class.java)
            .setAction(LerActivity.ACAO_COPIADO)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(i)
        }
    }
}
