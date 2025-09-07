package com.example.smartshopping.einkaufsliste

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.smartshopping.R
import com.example.smartshopping.artikel.Artikel
import com.example.smartshopping.rezept.RezeptAuswahlActivity
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import com.example.smartshopping.vorlag.EinkaufslisteVorlage


class EinkaufslisteDetailActivity : AppCompatActivity() {

    private lateinit var tvTitel: TextView
    private lateinit var artikelContainer: LinearLayout
    private lateinit var buttonSpeichern: Button
    private lateinit var buttonArtikelHinzufügen: Button
    private lateinit var buttonVorlageHinzufügen: Button
    private var listenIndex: Int = -1
    private lateinit var aktuelleListe: Einkaufsliste
    private var datenGeändert = false
    private val dateiname = "einkaufslisten.dat"
    private val ARTIKEL_AUSWAHL_REQUEST = 1001
    private val TAG = "DEBUG"
    private val abhakenStatus: MutableMap<String, Boolean> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_einkaufsliste_detail)

        tvTitel = findViewById(R.id.tvListenTitel)
        artikelContainer = findViewById(R.id.lvArtikel)
        buttonSpeichern = findViewById(R.id.btnSpeichern)
        buttonArtikelHinzufügen = findViewById(R.id.btnArtikelHinzufügen)
        buttonVorlageHinzufügen = findViewById(R.id.btnVorlageHinzufügen)

        listenIndex = intent.getIntExtra("listeIndex", -1)
        if (listenIndex == -1) {
            finish()
            return
        }

        val alleListen = ladeEinkaufslisten()
        aktuelleListe = alleListen[listenIndex]

        ladeAbhakenStatus()
        tvTitel.text = aktuelleListe.titel
        zeigeArtikelListe()

        buttonSpeichern.setOnClickListener {
            if (datenGeändert) {
                alleListen[listenIndex] = aktuelleListe
                speichereEinkaufslisten(alleListen)
                speichereAbhakenStatus()
                Toast.makeText(this, "Änderungen gespeichert", Toast.LENGTH_SHORT).show()
                datenGeändert = false
                buttonSpeichern.visibility = View.GONE
            }
        }

        buttonArtikelHinzufügen.setOnClickListener {
            val optionen = arrayOf("Neuen Artikel erstellen", "Artikel aus Vorlage hinzufügen")
            AlertDialog.Builder(this)
                .setTitle("Aktion wählen")
                .setItems(optionen) { _, which ->
                    when (which) {
                        0 -> zeigeArtikelErstellenDialog()
                        1 -> {
                            val intent = Intent(this, RezeptAuswahlActivity::class.java)
                            startActivityForResult(intent, ARTIKEL_AUSWAHL_REQUEST)
                        }
                    }
                }
                .show()
        }


        buttonVorlageHinzufügen.setOnClickListener {
            val prefs = getSharedPreferences("vorlagen_pref", Context.MODE_PRIVATE)
            val gson = com.google.gson.Gson()
            val json = prefs.getString("vorlagenListe", null)

            if (json.isNullOrEmpty()) {
                zeigeHinweisDialog("Keine Vorlagen vorhanden")
                return@setOnClickListener
            }

            val typ = object : com.google.gson.reflect.TypeToken<MutableList<EinkaufslisteVorlage>>() {}.type
            val vorlagen = gson.fromJson<MutableList<EinkaufslisteVorlage>>(json, typ)

            if (vorlagen.isEmpty()) {
                zeigeHinweisDialog("Keine Vorlagen gefunden")
                return@setOnClickListener
            }

            // Liste mit Platzhalter an erster Stelle
            val namenMitLeer = mutableListOf("-")
            namenMitLeer.addAll(vorlagen.map { it.name })

            // Spinner mit einfachem ArrayAdapter
            val spinner = Spinner(this)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, namenMitLeer)
            spinner.adapter = adapter
            spinner.setSelection(0)

            // Spinner in Layout mit Padding einfügen
            val layout = LinearLayout(this)
            layout.setPadding(48, 24, 48, 0)
            layout.orientation = LinearLayout.VERTICAL
            layout.addView(spinner)

            // Dialog anzeigen
            AlertDialog.Builder(this)
                .setTitle("Vorlage auswählen")
                .setView(layout)
                .setPositiveButton("Übernehmen") { _, _ ->
                    val index = spinner.selectedItemPosition
                    if (index == 0) {
                        zeigeHinweisDialog("Bitte eine Vorlage auswählen")
                        return@setPositiveButton
                    }

                    val ausgewählteVorlage = vorlagen[index - 1]
                    var hinzugefügt = 0
                    for (artikel in ausgewählteVorlage.artikel) {
                        val indexExistierend = aktuelleListe.artikel.indexOfFirst {
                            it.name.trim().equals(artikel.name.trim(), ignoreCase = true)
                        }

                        if (indexExistierend >= 0) {
                            val alt = aktuelleListe.artikel[indexExistierend]
                            alt.menge = erhöheMengeMitZiel(alt.menge, artikel.menge)
                        } else {
                            aktuelleListe.artikel.add(artikel)
                            hinzugefügt++
                        }
                    }

                    if (hinzugefügt > 0) {
                        zeigeArtikelListe()
                        datenGeändert = true
                        buttonSpeichern.visibility = View.VISIBLE
                        Toast.makeText(this, "$hinzugefügt Artikel übernommen", Toast.LENGTH_SHORT).show()
                    } else {
                        zeigeHinweisDialog("Alle Artikel sind bereits vorhanden")
                    }
                }
                .setNegativeButton("Abbrechen", null)
                .show()
        }



    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ARTIKEL_AUSWAHL_REQUEST && resultCode == Activity.RESULT_OK) {
            val neueArtikel = data?.getSerializableExtra("ausgewaehlteZutaten") as? ArrayList<Artikel>
            if (neueArtikel == null || neueArtikel.isEmpty()) {
                Log.d(TAG, "Keine ausgewählten Artikel empfangen")
                return
            }

            var hinzugefügt = 0
            for (neuer in neueArtikel) {
                val index = aktuelleListe.artikel.indexOfFirst {
                    it.name.trim().equals(neuer.name.trim(), ignoreCase = true)
                }
                if (index >= 0) {
                    val alt = aktuelleListe.artikel[index]
                    val neueMenge = erhöheMengeMitZiel(alt.menge, neuer.menge)
                    alt.menge = neueMenge
                } else {
                    aktuelleListe.artikel.add(neuer)
                    hinzugefügt++
                }
            }

            if (hinzugefügt > 0) {
                zeigeArtikelListe()
                datenGeändert = true
                buttonSpeichern.visibility = View.VISIBLE
            } else {
                zeigeHinweisDialog("Artikel ist bereits in der Liste")
            }
        }
    }

    private fun zeigeArtikelListe() {
        artikelContainer.removeAllViews()
        for (artikel in aktuelleListe.artikel) {
            val zeile = layoutInflater.inflate(R.layout.activity_einkaufsliste_artikel_item, artikelContainer, false)
            val cbAbgehakt = zeile.findViewById<CheckBox>(R.id.cbAbgehakt)
            val tvName = zeile.findViewById<TextView>(R.id.tvArtikelName)
            val tvMenge = zeile.findViewById<TextView>(R.id.tvMenge)
            val btnMinus = zeile.findViewById<Button>(R.id.btnMinus)
            val btnPlus = zeile.findViewById<Button>(R.id.btnPlus)

            val artikelKey = artikel.name.trim()
            cbAbgehakt.isChecked = abhakenStatus[artikelKey] == true
            cbAbgehakt.setOnCheckedChangeListener { _, isChecked ->
                abhakenStatus[artikelKey] = isChecked
                datenGeändert = true
                buttonSpeichern.visibility = View.VISIBLE
            }

            tvName.text = artikel.name
            tvMenge.text = artikel.menge

            btnMinus.setOnClickListener {
                artikel.menge = verringereMenge(artikel.menge)
                tvMenge.text = artikel.menge
                datenGeändert = true
                buttonSpeichern.visibility = View.VISIBLE
            }

            btnPlus.setOnClickListener {
                artikel.menge = erhöheMenge(artikel.menge)
                tvMenge.text = artikel.menge
                datenGeändert = true
                buttonSpeichern.visibility = View.VISIBLE
            }

            artikelContainer.addView(zeile)
        }
    }

    private fun zeigeArtikelErstellenDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_vorlage_dialog_artikel_erstellen, null)
        val etName = dialogView.findViewById<EditText>(R.id.etArtikelName)
        val spEinheit = dialogView.findViewById<Spinner>(R.id.spEinheit)

        val einheiten = listOf("kg", "l", "Stück")
        spEinheit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, einheiten).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Neuen Artikel hinzufügen")
            .setView(dialogView)
            .setPositiveButton("Hinzufügen", null)
            .setNegativeButton("Abbrechen", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val name = etName.text.toString().trim()
                val einheit = spEinheit.selectedItem.toString()

                if (name.isNotEmpty()) {
                    val existiert = aktuelleListe.artikel.any {
                        it.name.trim().equals(name, ignoreCase = true)
                    }

                    if (existiert) {
                        zeigeHinweisDialog("Artikel ist bereits in der Liste")
                    } else {
                        val artikel = Artikel(
                            name = name,
                            menge = "1 $einheit",
                            kategorie = "Sonstige",
                            kuehlung = false,
                            haltbarkeit = 1,
                            rechnungsart = "",
                            bildPfad = null
                        )
                        aktuelleListe.artikel.add(artikel)
                        zeigeArtikelListe()
                        datenGeändert = true
                        buttonSpeichern.visibility = View.VISIBLE
                        dialog.dismiss()
                    }
                }
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isAllCaps = false
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.isAllCaps = false
        }

        dialog.show()
    }

    private fun zeigeHinweisDialog(nachricht: String) {
        AlertDialog.Builder(this)
            .setMessage(nachricht)
            .setPositiveButton("OK", null)
            .setCancelable(true)
            .show()
    }

    private fun ladeAbhakenStatus() {
        val prefs = getSharedPreferences("einkaufsliste_prefs", Context.MODE_PRIVATE)
        val prefix = "abgehakt_liste_$listenIndex:"
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(prefix) && value is Boolean) {
                val artikelName = key.removePrefix(prefix)
                abhakenStatus[artikelName] = value
            }
        }
    }

    private fun speichereAbhakenStatus() {
        val prefs = getSharedPreferences("einkaufsliste_prefs", Context.MODE_PRIVATE).edit()
        val prefix = "abgehakt_liste_$listenIndex:"
        for ((artikelName, status) in abhakenStatus) {
            prefs.putBoolean("$prefix$artikelName", status)
        }
        prefs.apply()
    }

    private fun ladeEinkaufslisten(): MutableList<Einkaufsliste> {
        try {
            val fis = openFileInput(dateiname)
            val ois = ObjectInputStream(fis)
            @Suppress("UNCHECKED_CAST")
            val listen = ois.readObject() as MutableList<Einkaufsliste>
            ois.close()
            return listen
        } catch (e: Exception) {
            return mutableListOf()
        }
    }

    private fun speichereEinkaufslisten(liste: List<Einkaufsliste>) {
        try {
            val fos = openFileOutput(dateiname, Context.MODE_PRIVATE)
            val oos = ObjectOutputStream(fos)
            oos.writeObject(liste)
            oos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    override fun onBackPressed() {
        if (datenGeändert) {
            AlertDialog.Builder(this)
                .setTitle("Änderungen speichern?")
                .setMessage("Du hast Änderungen an der Einkaufsliste vorgenommen. Möchtest du sie speichern?")
                .setPositiveButton("Ja") { _, _ ->
                    val alleListen = ladeEinkaufslisten()
                    alleListen[listenIndex] = aktuelleListe
                    speichereEinkaufslisten(alleListen)
                    speichereAbhakenStatus()
                    Toast.makeText(this, "Änderungen gespeichert", Toast.LENGTH_SHORT).show()
                    super.onBackPressed()
                }
                .setNegativeButton("Nein") { _, _ ->
                    super.onBackPressed()
                }
                .setNeutralButton("Abbrechen", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

}
