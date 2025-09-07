package com.example.smartshopping.speiseplan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.smartshopping.R
import com.example.smartshopping.rezept.Rezept

class SpeiseplanTagBearbeitenActivity : AppCompatActivity() {

    private lateinit var rezeptListe: List<Rezept>
    private lateinit var tagDaten: SpeiseplanTag
    private var tagIndex: Int = -1

    private lateinit var spinnerVorspeise: Spinner
    private lateinit var spinnerHauptspeise: Spinner
    private lateinit var spinnerBeilage1: Spinner
    private lateinit var spinnerBeilage2: Spinner
    private lateinit var spinnerNachspeise: Spinner
    private lateinit var suchfeld: AutoCompleteTextView
    private lateinit var searchSection: LinearLayout
    private lateinit var btnSpeichern: Button

    private lateinit var originalAuswahl: List<String?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speiseplan_tag_bearbeiten)

        spinnerVorspeise = findViewById(R.id.spinnerVorspeise)
        spinnerHauptspeise = findViewById(R.id.spinnerHauptspeise)
        spinnerBeilage1 = findViewById(R.id.spinnerBeilage1)
        spinnerBeilage2 = findViewById(R.id.spinnerBeilage2)
        spinnerNachspeise = findViewById(R.id.spinnerNachspeise)
        suchfeld = findViewById(R.id.editRezeptSuche)
        searchSection = findViewById(R.id.searchSection)
        btnSpeichern = findViewById(R.id.btnSpeichern)

        tagDaten = intent.getSerializableExtra("tagDaten") as SpeiseplanTag
        tagIndex = intent.getIntExtra("tagIndex", -1)

        rezeptListe = ladeRezepte()

        if (rezeptListe.isNotEmpty()) {
            searchSection.visibility = View.VISIBLE
        }

        setupSpinner(spinnerVorspeise) { it.kategorie.contains("Vorspeise", true) }
        setupSpinner(spinnerHauptspeise) { it.kategorie.contains("Hauptspeise", true) }
        setupSpinner(spinnerBeilage1) { it.kategorie.contains("Beilage", true) }
        setupSpinner(spinnerBeilage2) { it.kategorie.contains("Beilage", true) }
        setupSpinner(spinnerNachspeise) { it.kategorie.contains("Nachspeise", true) }

        fun setSelection(spinner: Spinner, rezept: Rezept?) {
            val index = (spinner.adapter as? ArrayAdapter<String>)?.getPosition(rezept?.name ?: "–") ?: 0
            spinner.setSelection(index)
        }

        setSelection(spinnerVorspeise, tagDaten.vorspeise)
        setSelection(spinnerHauptspeise, tagDaten.hauptspeise)
        setSelection(spinnerBeilage1, tagDaten.beilage1)
        setSelection(spinnerBeilage2, tagDaten.beilage2)
        setSelection(spinnerNachspeise, tagDaten.nachspeise)

        // ursprüngliche Auswahl merken
        originalAuswahl = listOf(
            (spinnerVorspeise.selectedItem as? String)?.trim(),
            (spinnerHauptspeise.selectedItem as? String)?.trim(),
            (spinnerBeilage1.selectedItem as? String)?.trim(),
            (spinnerBeilage2.selectedItem as? String)?.trim(),
            (spinnerNachspeise.selectedItem as? String)?.trim()
        )

        val rezeptNamen = rezeptListe.map { it.name }
        val suchAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, rezeptNamen)
        suchfeld.setAdapter(suchAdapter)

        suchfeld.setOnItemClickListener { _, _, position, _ ->
            val name = suchAdapter.getItem(position) ?: return@setOnItemClickListener
            val rezept = rezeptListe.find { it.name == name } ?: return@setOnItemClickListener
            val getIndex = { s: Spinner -> (s.adapter as ArrayAdapter<String>).getPosition(rezept.name) }

            when {
                rezept.kategorie.contains("Vorspeise", true) && spinnerVorspeise.isEnabled -> spinnerVorspeise.setSelection(getIndex(spinnerVorspeise))
                rezept.kategorie.contains("Hauptspeise", true) && spinnerHauptspeise.isEnabled -> spinnerHauptspeise.setSelection(getIndex(spinnerHauptspeise))
                rezept.kategorie.contains("Beilage", true) -> {
                    if (spinnerBeilage1.isEnabled && spinnerBeilage1.selectedItemPosition == 0) {
                        spinnerBeilage1.setSelection(getIndex(spinnerBeilage1))
                    } else if (spinnerBeilage2.isEnabled) {
                        spinnerBeilage2.setSelection(getIndex(spinnerBeilage2))
                    }
                }
                rezept.kategorie.contains("Nachspeise", true) && spinnerNachspeise.isEnabled -> spinnerNachspeise.setSelection(getIndex(spinnerNachspeise))
                else -> Toast.makeText(this, "Unbekannte oder deaktivierte Kategorie", Toast.LENGTH_SHORT).show()
            }

            Toast.makeText(this, "$name übernommen", Toast.LENGTH_SHORT).show()
        }

        btnSpeichern.setOnClickListener {
            if (!hatMindestensEinRezept()) {
                Toast.makeText(this, "Bitte mindestens ein Rezept auswählen.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            speiseplanSpeichernUndBeenden()
        }
    }

    override fun onBackPressed() {
        val aktuelleAuswahl = listOf(
            (spinnerVorspeise.selectedItem as? String)?.trim(),
            (spinnerHauptspeise.selectedItem as? String)?.trim(),
            (spinnerBeilage1.selectedItem as? String)?.trim(),
            (spinnerBeilage2.selectedItem as? String)?.trim(),
            (spinnerNachspeise.selectedItem as? String)?.trim()
        )

        if (aktuelleAuswahl == originalAuswahl) {
            super.onBackPressed()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Änderungen speichern?")
            .setMessage("Du hast Änderungen an diesem Tag vorgenommen. Möchtest du sie speichern?")
            .setPositiveButton("Ja") { _, _ ->
                if (hatMindestensEinRezept()) {
                    speiseplanSpeichernUndBeenden()
                } else {
                    Toast.makeText(this, "Kein gültiges Rezept gewählt.", Toast.LENGTH_SHORT).show()
                    super.onBackPressed()
                }
            }
            .setNegativeButton("Nein") { _, _ ->
                super.onBackPressed()
            }
            .setNeutralButton("Abbrechen", null)
            .show()
    }

    private fun speiseplanSpeichernUndBeenden() {
        fun getRezept(spinner: Spinner): Rezept? {
            if (!spinner.isEnabled) return null
            val name = spinner.selectedItem as String
            return rezeptListe.find { it.name == name }
        }

        tagDaten.vorspeise = getRezept(spinnerVorspeise)
        tagDaten.hauptspeise = getRezept(spinnerHauptspeise)
        tagDaten.beilage1 = getRezept(spinnerBeilage1)
        tagDaten.beilage2 = getRezept(spinnerBeilage2)
        tagDaten.nachspeise = getRezept(spinnerNachspeise)

        val resultIntent = Intent().apply {
            putExtra("tagDaten", tagDaten)
            putExtra("tagIndex", tagIndex)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun hatMindestensEinRezept(): Boolean {
        return listOf(
            spinnerVorspeise, spinnerHauptspeise,
            spinnerBeilage1, spinnerBeilage2, spinnerNachspeise
        ).any { it.isEnabled && (it.selectedItem as? String)?.trim() != "–" }
    }

    private fun setupSpinner(spinner: Spinner, filter: (Rezept) -> Boolean) {
        val gefiltert = rezeptListe.filter(filter)
        if (gefiltert.isEmpty()) {
            spinner.adapter = null
            spinner.isEnabled = false
        } else {
            val namen = listOf("–") + gefiltert.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, namen)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            spinner.isEnabled = true
        }
    }

    private fun ladeRezepte(): List<Rezept> {
        val prefs = getSharedPreferences("rezepte", Context.MODE_PRIVATE)
        val daten = prefs.getString("rezeptListe", null) ?: return emptyList()
        return daten.split(";;").mapNotNull { zeile ->
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
    }
}
