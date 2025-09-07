package com.example.smartshopping.vorlag

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.smartshopping.R
import com.example.smartshopping.artikel.Artikel
import com.example.smartshopping.rezept.RezeptAuswahlActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class VorlageBearbeitenActivity : AppCompatActivity() {

    private lateinit var aktuelleVorlage: EinkaufslisteVorlage
    private lateinit var layoutContainer: LinearLayout
    private lateinit var buttonSpeichern: Button
    private val ARTIKEL_AUSWAHL_REQUEST = 2001
    private var datenGeändert = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vorlage_bearbeiten)

        layoutContainer = findViewById(R.id.lvVorlageArtikel)
        buttonSpeichern = findViewById(R.id.btnSpeichern)
        val titelTextView: TextView = findViewById(R.id.tvVorlagenTitel)
        val buttonArtikelHinzufügen: Button = findViewById(R.id.btnArtikelHinzufügen)

        val json = intent.getStringExtra("vorlage_json")
        if (json == null) {
            Toast.makeText(this, "Vorlage konnte nicht geladen werden", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val gson = Gson()
        aktuelleVorlage = gson.fromJson(json, EinkaufslisteVorlage::class.java)

        titelTextView.text = "Vorlage: ${aktuelleVorlage.name}"
        zeigeArtikel()

        // Dialog mit zwei Optionen
        buttonArtikelHinzufügen.setOnClickListener {
            val optionen = arrayOf("Neuen Artikel erstellen", "Artikel aus Vorlage hinzufügen")
            AlertDialog.Builder(this)
                .setTitle("Aktion wählen")
                .setItems(optionen) { _, which ->
                    when (which) {
                        0 -> zeigeArtikelHinzufügenDialog()
                        1 -> {
                            val intent = Intent(this, RezeptAuswahlActivity::class.java)
                            startActivityForResult(intent, ARTIKEL_AUSWAHL_REQUEST)
                        }
                    }
                }
                .show()
        }

        buttonSpeichern.setOnClickListener {
            speichereVorlage()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ARTIKEL_AUSWAHL_REQUEST && resultCode == Activity.RESULT_OK) {
            val neueArtikel = data?.getSerializableExtra("ausgewaehlteZutaten") as? ArrayList<Artikel>
            if (neueArtikel == null || neueArtikel.isEmpty()) return

            var hinzugefügt = 0
            for (neuer in neueArtikel) {
                val index = aktuelleVorlage.artikel.indexOfFirst {
                    it.name.trim().equals(neuer.name.trim(), ignoreCase = true)
                }
                if (index >= 0) {
                    val alt = aktuelleVorlage.artikel[index]
                    val neueMenge = erhöheMengeMitZiel(alt.menge, neuer.menge)
                    alt.menge = neueMenge
                } else {
                    aktuelleVorlage.artikel.add(neuer)
                    hinzugefügt++
                }
            }

            if (hinzugefügt > 0) {
                zeigeArtikel()
                zeigeSpeichernButton()
            } else {
                zeigeHinweisDialog("Artikel ist bereits vorhanden")
            }
        }
    }

    private fun zeigeArtikel() {
        layoutContainer.removeAllViews()
        for (artikel in aktuelleVorlage.artikel) {
            val zeile = LayoutInflater.from(this).inflate(R.layout.activity_einkaufsliste_artikel_item, layoutContainer, false)

            val cbAbgehakt = zeile.findViewById<CheckBox>(R.id.cbAbgehakt)
            val tvName = zeile.findViewById<TextView>(R.id.tvArtikelName)
            val tvMenge = zeile.findViewById<TextView>(R.id.tvMenge)
            val btnPlus = zeile.findViewById<Button>(R.id.btnPlus)
            val btnMinus = zeile.findViewById<Button>(R.id.btnMinus)

            cbAbgehakt.visibility = View.GONE
            tvName.text = artikel.name
            tvMenge.text = artikel.menge

            btnPlus.setOnClickListener {
                artikel.menge = erhöheMenge(artikel.menge)
                tvMenge.text = artikel.menge
                zeigeSpeichernButton()
            }

            btnMinus.setOnClickListener {
                artikel.menge = verringereMenge(artikel.menge)
                tvMenge.text = artikel.menge
                zeigeSpeichernButton()
            }

            layoutContainer.addView(zeile)
        }
    }

    private fun zeigeArtikelHinzufügenDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_vorlage_dialog_artikel_erstellen, null)
        val etArtikelName = dialogView.findViewById<EditText>(R.id.etArtikelName)
        val spEinheit = dialogView.findViewById<Spinner>(R.id.spEinheit)

        val einheiten = listOf("kg", "l", "Stück")
        spEinheit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, einheiten)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Artikel hinzufügen")
            .setView(dialogView)
            .setPositiveButton("Hinzufügen") { _, _ ->
                val name = etArtikelName.text.toString().trim()
                val einheit = spEinheit.selectedItem.toString()

                if (name.isNotEmpty()) {
                    val existiert = aktuelleVorlage.artikel.any { it.name.equals(name, ignoreCase = true) }
                    if (existiert) {
                        zeigeHinweisDialog("Artikel bereits vorhanden")
                    } else {
                        val artikel = Artikel(
                            name = name,
                            kategorie = "",
                            kuehlung = false,
                            haltbarkeit = 0,
                            menge = "1 $einheit",
                            rechnungsart = einheit
                        )
                        aktuelleVorlage.artikel.add(artikel)
                        zeigeArtikel()
                        zeigeSpeichernButton()
                    }
                }
            }
            .setNegativeButton("Abbrechen", null)
            .create()

        dialog.show()

// Text nicht in Großbuchstaben darstellen
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isAllCaps = false
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.isAllCaps = false

    }

    private fun erhöheMenge(menge: String): String {
        val (wert, einheit) = parseMenge(menge)
        val neuerWert = if (einheit == "kg" || einheit == "l") wert + 0.5 else wert + 1
        return formatMenge(neuerWert, einheit)
    }

    private fun erhöheMengeMitZiel(alt: String, plus: String): String {
        val (wertAlt, einheitAlt) = parseMenge(alt)
        val (wertPlus, einheitPlus) = parseMenge(plus)
        return if (einheitAlt == einheitPlus) {
            formatMenge(wertAlt + wertPlus, einheitAlt)
        } else alt
    }

    private fun verringereMenge(menge: String): String {
        val (wert, einheit) = parseMenge(menge)
        val neuerWert = if (einheit == "kg" || einheit == "l") maxOf(0.0, wert - 0.5) else maxOf(0.0, wert - 1)
        return formatMenge(neuerWert, einheit)
    }

    private fun parseMenge(menge: String): Pair<Double, String> {
        val regex = Regex("([0-9,.]+)\\s*(\\D*)")
        val match = regex.find(menge.trim())
        val wert = match?.groups?.get(1)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val einheit = match?.groups?.get(2)?.value?.trim() ?: ""
        return Pair(wert, einheit)
    }

    private fun formatMenge(wert: Double, einheit: String): String {
        val formatiert = if (einheit.isNotEmpty()) {
            String.format("%.1f %s", wert, einheit).replace(".0", "")
        } else {
            String.format("%.1f", wert).replace(".0", "")
        }
        return formatiert
    }

    private fun zeigeHinweisDialog(nachricht: String) {
        AlertDialog.Builder(this)
            .setMessage(nachricht)
            .setPositiveButton("ok", null)
            .show()
    }

    private fun zeigeSpeichernButton() {
        datenGeändert = true
        buttonSpeichern.visibility = View.VISIBLE
    }

    private fun speichereVorlage() {
        val prefs = getSharedPreferences("vorlagen_pref", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("vorlagenListe", "[]")
        val typ = object : TypeToken<MutableList<EinkaufslisteVorlage>>() {}.type
        val vorlagen = gson.fromJson<MutableList<EinkaufslisteVorlage>>(json, typ)

        val index = vorlagen.indexOfFirst { it.name.equals(aktuelleVorlage.name, ignoreCase = true) }
        if (index != -1) {
            vorlagen[index] = aktuelleVorlage
            prefs.edit().putString("vorlagenListe", gson.toJson(vorlagen)).apply()
            Toast.makeText(this, "Vorlage gespeichert", Toast.LENGTH_SHORT).show()
            datenGeändert = false
            buttonSpeichern.visibility = View.GONE
        }
    }
    override fun onBackPressed() {
        if (datenGeändert) {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Änderungen speichern?")
                .setMessage("Möchtest du die Änderungen an der Vorlage speichern?")
                .setPositiveButton("Ja") { _, _ ->
                    speichereVorlage()
                    super.onBackPressed()
                }
                .setNegativeButton("Nein") { _, _ ->
                    super.onBackPressed()
                }
                .setNeutralButton("Abbrechen", null)
                .create()

            dialog.show()

            // Button-Texte klein anzeigen
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isAllCaps = false
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.isAllCaps = false
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.isAllCaps = false
        } else {
            super.onBackPressed()
        }
    }

}
