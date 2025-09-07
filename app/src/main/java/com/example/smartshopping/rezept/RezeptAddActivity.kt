package com.example.smartshopping.rezept

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.smartshopping.R
import com.example.smartshopping.artikel.Artikel

class RezeptAddActivity : AppCompatActivity() {

    private lateinit var spKategorie: Spinner
    private lateinit var etName: EditText
    private lateinit var etBeschreibung: EditText
    private lateinit var seekBarDauer: SeekBar
    private lateinit var seekBarPersonen: SeekBar
    private lateinit var tvDauerWert: TextView
    private lateinit var tvPersonenWert: TextView
    private lateinit var btnZutaten: Button
    private lateinit var btnSpeichern: Button
    private lateinit var containerZutaten: LinearLayout

    private var rezeptIndex: Int = -1
    private var ausgewaehlteZutaten: MutableList<Artikel> = mutableListOf()
    private val kategorien = listOf("Vorspeisen", "Hauptspeisen", "Beilagen", "Nachtisch")

    companion object {
        const val ZUTATEN_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rezept_add)

        spKategorie = findViewById(R.id.spKategorie)
        etName = findViewById(R.id.etRezeptName)
        etBeschreibung = findViewById(R.id.etRezeptBeschreibung)
        seekBarDauer = findViewById(R.id.seekBarDauer)
        seekBarPersonen = findViewById(R.id.seekBarPersonen)
        tvDauerWert = findViewById(R.id.tvDauerWert)
        tvPersonenWert = findViewById(R.id.tvPersonenWert)
        btnZutaten = findViewById(R.id.btnZutatenAuswaehlen)
        btnSpeichern = findViewById(R.id.btnSpeichern)
        containerZutaten = findViewById(R.id.containerZutaten)

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kategorien)
        spKategorie.adapter = spinnerAdapter

        val rezept = intent.getSerializableExtra("rezept") as? Rezept
        rezeptIndex = intent.getIntExtra("rezeptIndex", -1)

        rezept?.let {
            etName.setText(it.name)
            etBeschreibung.setText(it.beschreibung)
            seekBarDauer.progress = it.dauer
            seekBarPersonen.progress = (it.personen - 1).coerceAtLeast(0)
            ausgewaehlteZutaten = it.zutaten.toMutableList()
            aktualisiereZutatenListe()
            val index = kategorien.indexOf(it.kategorie)
            if (index >= 0) spKategorie.setSelection(index)
        }

        tvDauerWert.text = "${seekBarDauer.progress * 15} Minuten"
        tvPersonenWert.text = "${seekBarPersonen.progress + 1} Personen"

        seekBarDauer.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDauerWert.text = "${progress * 15} Minuten"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarPersonen.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val personen = progress + 1
                tvPersonenWert.text = if (personen == 1) "1 Person" else "$personen Personen"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnZutaten.setOnClickListener {
            val intent = Intent(this, RezeptAuswahlActivity::class.java)
            intent.putExtra("ausgewaehlteZutaten", ArrayList(ausgewaehlteZutaten))
            startActivityForResult(intent, ZUTATEN_REQUEST_CODE)
        }

        btnSpeichern.setOnClickListener {
            speichernUndZurueck()
        }
    }

    private fun aktualisiereZutatenListe() {
        containerZutaten.removeAllViews()
        val artikelMap = mutableMapOf<String, Pair<Double, String>>()

        ausgewaehlteZutaten.forEach { artikel ->
            val name = artikel.name
            val mengeText = artikel.menge.lowercase()
            val (wert, einheit) = when {
                mengeText.contains("kg") -> mengeText.replace("kg", "").trim().toDoubleOrNull() to "kg"
                mengeText.contains("l") -> mengeText.replace("l", "").trim().toDoubleOrNull() to "L"
                mengeText.contains("stk") -> mengeText.replace("stk", "").trim().toDoubleOrNull() to "Stk"
                else -> null to ""
            }
            if (wert != null) {
                val key = "$name|$einheit"
                val aktuellerEintrag = artikelMap[key]
                artikelMap[key] = if (aktuellerEintrag != null) {
                    (aktuellerEintrag.first + wert) to einheit
                } else {
                    wert to einheit
                }
            }
        }

        artikelMap.forEach { (key, pair) ->
            val (name, einheit) = key.split("|")
            val (gesamtMenge, _) = pair
            val zeile = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(8, 8, 8, 8)
            }

            val textView = TextView(this).apply {
                text = String.format("%s %.1f %s", name, gesamtMenge, einheit)
                textSize = 16f
                setTextColor(resources.getColor(R.color.black, theme))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val btnLoeschen = Button(this).apply {
                text = "✕"
                setOnClickListener {
                    ausgewaehlteZutaten.removeAll { it.name == name && it.menge.contains(einheit) }
                    aktualisiereZutatenListe()
                }
            }

            zeile.addView(textView)
            zeile.addView(btnLoeschen)
            containerZutaten.addView(zeile)
        }
    }

    private fun speichernUndZurueck() {
        val name = etName.text.toString().trim()
        val beschreibung = etBeschreibung.text.toString().trim()
        val kategorie = spKategorie.selectedItem.toString()
        val dauer = seekBarDauer.progress * 15
        val personen = seekBarPersonen.progress + 1

        if (name.isBlank()) {
            AlertDialog.Builder(this)
                .setTitle("Hinweis")
                .setMessage("Bitte einen Namen eingeben")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        if (ausgewaehlteZutaten.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Hinweis")
                .setMessage("Bitte mindestens eine Zutat hinzufügen")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val rezept = Rezept(name, beschreibung, ausgewaehlteZutaten, dauer, personen, kategorie)

        val prefs = getSharedPreferences("rezepte", Context.MODE_PRIVATE)
        val rezepte = ladeAlleGespeichertenRezepte().toMutableList()

        if (rezeptIndex == -1) {
            val nameBereitsVerwendet = rezepte.any { it.name.equals(name, ignoreCase = true) }
            if (nameBereitsVerwendet) {
                AlertDialog.Builder(this)
                    .setTitle("Name bereits vergeben")
                    .setMessage("Ein Rezept mit diesem Namen existiert bereits. Bitte einen anderen Namen wählen.")
                    .setPositiveButton("OK", null)
                    .show()
                return
            }
            rezepte.add(rezept)
            rezeptIndex = rezepte.lastIndex
        } else {
            rezepte[rezeptIndex] = rezept
        }

        // ❗ Richtiger Key verwenden!
        val gespeichert = rezepte.joinToString(";;") {
            val zutatenStr = it.zutaten.joinToString("|") { zutat -> "${zutat.name},${zutat.menge}" }
            "${it.name}::${it.beschreibung}::${zutatenStr}::${it.dauer}::${it.personen}::${it.kategorie}"
        }

        prefs.edit().putString("rezeptListe", gespeichert).apply()

        // ❗ Jetzt: Rezept sofort an Detailansicht übergeben
        val detailIntent = Intent(this, RezeptDetailActivity::class.java).apply {
            putExtra("rezept", rezept)
            putExtra("rezeptIndex", rezeptIndex)
        }

        // ❗ UND Ergebnis auch an RezeptActivity zurückgeben!
        val resultIntent = Intent().apply {
            putExtra("neuesRezept", rezept)
            putExtra("rezeptIndex", rezeptIndex)
        }
        setResult(Activity.RESULT_OK, resultIntent)

        startActivity(detailIntent)
        finish()
    }




    private fun ladeAlleGespeichertenRezepte(): List<Rezept> {
        val prefs = getSharedPreferences("rezepte", Context.MODE_PRIVATE)
        val gespeichert = prefs.getString("rezeptListe", null) ?: return emptyList()
        return gespeichert.split(";;").mapNotNull { zeile ->
            val teile = zeile.split("::")
            if (teile.size < 6) return@mapNotNull null
            val name = teile[0]
            val beschreibung = teile[1]
            val zutaten = teile[2].split("|").mapNotNull {
                val infos = it.split(",")
                if (infos.size >= 2)
                    Artikel(
                        name = infos[0],
                        kategorie = "",
                        kuehlung = false,
                        haltbarkeit = 0,
                        menge = infos[1],
                        rechnungsart = "",
                        bildPfad = null
                    ) else null
            }
            val dauer = teile[3].toIntOrNull() ?: 0
            val personen = teile[4].toIntOrNull() ?: 1
            val kategorie = teile[5]
            Rezept(name, beschreibung, zutaten, dauer, personen, kategorie)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ZUTATEN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val neueZutaten = data?.getSerializableExtra("ausgewaehlteZutaten") as? ArrayList<Artikel>
            if (neueZutaten != null) {
                val neueNamen = neueZutaten.map { it.name }.toSet()
                ausgewaehlteZutaten = ausgewaehlteZutaten.filterNot { it.name in neueNamen }.toMutableList()
                ausgewaehlteZutaten.addAll(neueZutaten)
                aktualisiereZutatenListe()
            }
        }
    }

    private inline fun <T> Iterable<T>.anyIndexed(predicate: (index: Int, T) -> Boolean): Boolean {
        var index = 0
        for (element in this) {
            if (predicate(index, element)) return true
            index++
        }
        return false
    }

    override fun onBackPressed() {
        if (etName.text.isNotBlank() || etBeschreibung.text.isNotBlank() || ausgewaehlteZutaten.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Rezept nicht gespeichert")
                .setMessage("Möchtest du das Rezept speichern?")
                .setPositiveButton("ja") { _, _ -> speichernUndZurueck() }
                .setNegativeButton("nein") { _, _ -> super.onBackPressed() }
                .setNeutralButton("abbrechen", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}
