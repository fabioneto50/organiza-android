package com.organiza.app

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (24 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        container.addView(TextView(this).apply {
            text = "Organiza — utilização de dados de saúde"
            textSize = 22f
        })
        container.addView(TextView(this).apply {
            text = "\nA Organiza pede apenas acesso de leitura às sessões de sono, quando ativares esta opção. A duração do sono é usada localmente no dispositivo para ajustar o nível de energia e proteger períodos de recuperação. A app não envia estes dados para servidores nem os partilha com terceiros nesta versão. Podes revogar a permissão a qualquer momento no Health Connect."
            textSize = 16f
        })
        setContentView(container)
    }
}
