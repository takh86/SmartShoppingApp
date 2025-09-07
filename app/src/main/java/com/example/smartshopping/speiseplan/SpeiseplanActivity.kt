package com.example.smartshopping.speiseplan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartshopping.R
import com.example.smartshopping.rezept.Rezept
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import androidx.core.content.ContextCompat

class SpeiseplanActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SpeiseplanAdapter

    private val wochentage = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
    private val speiseplan = mutableListOf<SpeiseplanTag>()
    private val rezeptListe = mutableListOf<Rezept>()
    private var wurdeGeaendert = false
    private lateinit var originalSpeiseplan: List<SpeiseplanTag>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speiseplan)

        recyclerView = findViewById(R.id.recyclerSpeiseplan)

        ladeRezepte()
        ladeSpeiseplan()

        adapter = SpeiseplanAdapter(this, speiseplan, rezeptListe) { tag, index ->
            val intent = Intent(this, SpeiseplanTagBearbeitenActivity::class.java).apply {
                putExtra("tagDaten", tag)
                putExtra("tagIndex", index)
            }
            launcher.launch(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            private val paint = Paint().apply {
                color = ContextCompat.getColor(this@SpeiseplanActivity, R.color.fab_green) // oder ein anderer Farbwert
                strokeWidth = 2f
            }

            override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                val left = parent.paddingLeft.toFloat()
                val right = parent.width - parent.paddingRight.toFloat()

                for (i in 0 until parent.childCount - 1) {
                    val child = parent.getChildAt(i)
                    val params = child.layoutParams as RecyclerView.LayoutParams
                    val top = child.bottom + params.bottomMargin + 4
                    val bottom = top + 2

                    c.drawLine(left, top.toFloat(), right, top.toFloat(), paint)
                }
            }
        })

        recyclerView.adapter = adapter
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val tag = result.data!!.getSerializableExtra("tagDaten") as? SpeiseplanTag ?: return@registerForActivityResult
            val index = result.data!!.getIntExtra("tagIndex", -1)
            if (index in speiseplan.indices) {
                speiseplan[index] = tag
                adapter.notifyItemChanged(index)
                wurdeGeaendert = true
            }
        }
    }

    private fun ladeRezepte() {
        val prefs = getSharedPreferences("rezepte", Context.MODE_PRIVATE)
        val daten = prefs.getString("rezeptListe", null) ?: return
        rezeptListe.clear()
        rezeptListe.addAll(
            daten.split(";;").mapNotNull { zeile ->
                val teile = zeile.split("::")
                if (teile.size >= 6) Rezept(
                    name = teile[0],
                    beschreibung = teile[1],
                    zutaten = listOf(),
                    dauer = teile[3].toIntOrNull() ?: 0,
                    personen = teile[4].toIntOrNull() ?: 1,
                    kategorie = teile[5]
                ) else null
            }
        )
    }

    private fun ladeSpeiseplan() {
        speiseplan.clear()
        val rezeptMap = rezeptListe.associateBy { it.name }
        val prefs = getSharedPreferences("speiseplan", Context.MODE_PRIVATE)
        val daten = prefs.getString("wochenplan", null)

        if (daten.isNullOrBlank()) {
            speiseplan.addAll(wochentage.map { SpeiseplanTag(it) })
        } else {
            val zeilen = daten.split(";;")
            for (zeile in zeilen) {
                val teile = zeile.split("::")
                val tagName = teile.getOrNull(0) ?: continue
                if (tagName !in wochentage) continue

                val tag = SpeiseplanTag(
                    name = tagName,
                    vorspeise = rezeptMap[teile.getOrNull(1) ?: ""],
                    hauptspeise = rezeptMap[teile.getOrNull(2) ?: ""],
                    beilage1 = rezeptMap[teile.getOrNull(3) ?: ""],
                    beilage2 = rezeptMap[teile.getOrNull(4) ?: ""],
                    nachspeise = rezeptMap[teile.getOrNull(5) ?: ""]
                )
                speiseplan.add(tag)
            }

            if (speiseplan.size < wochentage.size) {
                val vorhandeneTage = speiseplan.map { it.name }.toSet()
                val fehlende = wochentage.filterNot { it in vorhandeneTage }
                speiseplan.addAll(fehlende.map { SpeiseplanTag(it) })
            }
        }

        originalSpeiseplan = speiseplan.map { it.copy() }
    }

    private fun speichereSpeiseplan() {
        val prefs = getSharedPreferences("speiseplan", Context.MODE_PRIVATE).edit()
        val daten = speiseplan.joinToString(";;") { tag ->
            listOf(
                tag.name,
                tag.vorspeise?.name ?: "",
                tag.hauptspeise?.name ?: "",
                tag.beilage1?.name ?: "",
                tag.beilage2?.name ?: "",
                tag.nachspeise?.name ?: ""
            ).joinToString("::")
        }
        prefs.putString("wochenplan", daten)
        prefs.apply()
    }

    override fun onPause() {
        super.onPause()
        if (wurdeGeaendert) {
            speichereSpeiseplan()
        }
    }
}
