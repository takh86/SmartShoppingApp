package com.example.smartshopping.artikel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ExpandableListView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.smartshopping.R

class ArtikelActivity : AppCompatActivity() {

    private lateinit var expandableListView: ExpandableListView
    private lateinit var adapter: ArtikelExpandableAdapter
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private val artikelListeData = mutableListOf<Artikel>()
    private val sharedPref by lazy { getSharedPreferences("artikel_pref", Context.MODE_PRIVATE) }

    private var kategorien: List<String> = listOf()
    private var artikelMap: Map<String, List<Artikel>> = mapOf()

    private val bekannteKategorien = listOf(
        "Obst", "Gemüse", "Milchprodukte", "Fleisch", "Getränke", "Haushalt", "Sonstige"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artikel)

        expandableListView = findViewById(R.id.expandableArtikelListe)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddArtikel)

        loadArtikelListe()
        aktualisiereDaten()

        adapter = ArtikelExpandableAdapter(this, kategorien, artikelMap)
        expandableListView.setAdapter(adapter)

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val index = result.data?.getIntExtra("artikelIndex", -1) ?: -1
                val artikel = result.data?.getSerializableExtra("artikel") as? Artikel
                val loeschen = result.data?.getBooleanExtra("loeschen", false) ?: false
                val nurAnzeigen = result.data?.getBooleanExtra("nurAnzeigen", false) ?: false

                when {
                    loeschen && index >= 0 -> {
                        artikelListeData.removeAt(index)
                        saveArtikelListe()
                    }
                    artikel != null -> {
                        if (index >= 0 && index < artikelListeData.size) {
                            artikelListeData[index] = artikel
                        } else {
                            artikelListeData.add(artikel)
                        }
                        saveArtikelListe()

                        // 👉 Direkt weiter zur Detailansicht nach Neuanlage
                        if (nurAnzeigen) {
                            val detailIntent = Intent(this, ArtikelDetailActivity::class.java).apply {
                                putExtra("artikel", artikel)
                                putExtra("artikelIndex", artikelListeData.indexOf(artikel))
                            }
                            launcher.launch(detailIntent)
                            return@registerForActivityResult
                        }
                    }
                }

                aktualisiereDaten()
                adapter.updateData(kategorien, artikelMap)
            }
        }

        // 🟢 Kurz-Klick für Detailansicht
        expandableListView.setOnChildClickListener { _, _, groupPos, childPos, _ ->
            val artikel = artikelMap[kategorien[groupPos]]?.get(childPos)
            val index = artikelListeData.indexOfFirst { it.name == artikel?.name }
            if (artikel != null && index >= 0) {
                val intent = Intent(this, ArtikelDetailActivity::class.java).apply {
                    putExtra("artikel", artikel)
                    putExtra("artikelIndex", index)
                }
                launcher.launch(intent)
            }
            true
        }

        // 🟡 Long-Klick für Bearbeitung
        expandableListView.setOnItemLongClickListener { _, _, flatPos, _ ->
            val packedPos = expandableListView.getExpandableListPosition(flatPos)
            val groupPos = ExpandableListView.getPackedPositionGroup(packedPos)
            val childPos = ExpandableListView.getPackedPositionChild(packedPos)

            if (childPos >= 0 && groupPos >= 0) {
                val artikel = artikelMap[kategorien[groupPos]]?.get(childPos)
                val index = artikelListeData.indexOfFirst { it.name == artikel?.name }
                if (artikel != null && index >= 0) {
                    val intent = Intent(this, ArtikelAddActivity::class.java).apply {
                        putExtra("artikel", artikel)
                        putExtra("artikelIndex", index)
                    }
                    launcher.launch(intent)
                    return@setOnItemLongClickListener true
                }
            }
            false
        }

        // ➕ Neuer Artikel
        fabAdd.setOnClickListener {
            launcher.launch(Intent(this, ArtikelAddActivity::class.java))
        }
    }

    private fun aktualisiereDaten() {
        val gruppiert = artikelListeData.groupBy { it.kategorie.ifBlank { "Unbekannt" } }

        val mapMitLeeren = mutableMapOf<String, List<Artikel>>()
        for (kategorie in bekannteKategorien) {
            mapMitLeeren[kategorie] = gruppiert[kategorie] ?: emptyList()
        }

        artikelMap = mapMitLeeren
        kategorien = bekannteKategorien
    }

    private fun saveArtikelListe() {
        val daten = artikelListeData.joinToString(";;") { artikel ->
            listOf(
                artikel.name,
                artikel.kategorie,
                artikel.kuehlung.toString(),
                artikel.haltbarkeit,
                artikel.menge,
                artikel.rechnungsart,
                artikel.bildPfad ?: ""
            ).joinToString("::")
        }
        sharedPref.edit().putString("artikelListe", daten).apply()
    }

    private fun loadArtikelListe() {
        artikelListeData.clear()
        val daten = sharedPref.getString("artikelListe", null)
        if (!daten.isNullOrEmpty()) {
            daten.split(";;").forEach { eintrag ->
                val teile = eintrag.split("::")
                if (teile.size >= 6) {
                    val artikel = Artikel(
                        name = teile[0],
                        kategorie = teile[1],
                        kuehlung = teile[2].toBoolean(),
                        haltbarkeit = teile[3].toIntOrNull() ?: 0,
                        menge = teile[4],
                        rechnungsart = teile[5],
                        bildPfad = if (teile.size > 6) teile[6] else null
                    )
                    artikelListeData.add(artikel)
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        loadArtikelListe()
        aktualisiereDaten()
        adapter.updateData(kategorien, artikelMap)
    }

}
