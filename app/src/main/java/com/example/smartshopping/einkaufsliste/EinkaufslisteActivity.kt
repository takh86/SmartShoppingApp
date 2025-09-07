package com.example.smartshopping.einkaufsliste

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.smartshopping.R
import com.example.smartshopping.artikel.Artikel
import com.example.smartshopping.rezept.Rezept
import com.example.smartshopping.speiseplan.SpeiseplanTag
import com.example.smartshopping.vorlag.EinkaufslisteVorlage
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*
import android.view.View
import android.view.ViewGroup


class EinkaufslisteActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var buttonNeueListe: FloatingActionButton
    private lateinit var buttonVorlageVerwenden: FloatingActionButton
    private lateinit var adapter: ArrayAdapter<String>
    private val einkaufslisten = mutableListOf<Einkaufsliste>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_einkaufsliste)

        listView = findViewById(R.id.lvEinkaufslisten)
        buttonNeueListe = findViewById(R.id.btnListeHinzufuegen)
        buttonVorlageVerwenden = findViewById(R.id.btnVorlageVerwenden)

        ladeEinkaufslisten()

        adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            einkaufslisten.map { it.titel }
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val context = this@EinkaufslisteActivity

                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }

                val textView = TextView(context).apply {
                    text = getItem(position)
                    setTextAppearance(context, android.R.style.TextAppearance_Medium)
                    setPadding(24, 70, 24, 70)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }

                val line = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        2 // Höhe der Linie in px
                    )
                    setBackgroundColor(android.graphics.Color.parseColor("#80BA27"))
                }

                layout.addView(textView)
                layout.addView(line)

                return layout
            }
        }




        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, EinkaufslisteDetailActivity::class.java)
            intent.putExtra("listeIndex", position)
            startActivity(intent)
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            AlertDialog.Builder(this)
                .setTitle("Liste löschen?")
                .setMessage("Willst du die Einkaufsliste „${einkaufslisten[position].titel}“ wirklich löschen?")
                .setPositiveButton("Ja") { _, _ ->
                    einkaufslisten.removeAt(position)
                    speichereEinkaufslisten()
                    aktualisiereListe()
                }
                .setNegativeButton("Abbrechen", null)
                .show()
            true
        }

        buttonNeueListe.setOnClickListener {
            zeigeDialogNeueListe()
        }

        buttonVorlageVerwenden.setOnClickListener {
            val intent = Intent(this, com.example.smartshopping.vorlag.VorlageAuswaehlenActivity::class.java)
            startActivityForResult(intent, 1234)
        }
    }

    override fun onResume() {
        super.onResume()
        ladeEinkaufslisten()

        val prefs = getSharedPreferences("speiseplan", Context.MODE_PRIVATE)
        val daten = prefs.getString("wochenplan", null)
        val aktuellerHash = daten?.hashCode() ?: 0

        val metaPrefs = getSharedPreferences("einkauf_meta", Context.MODE_PRIVATE)
        val letzterHash = metaPrefs.getInt("speiseplan_hash", 0)

        if (einkaufslisten.isEmpty() || aktuellerHash != letzterHash) {
            println("DEBUG: Einkaufslisten leer oder Speiseplan geändert → Liste neu generieren")
            generiereListeAusSpeiseplan()
            metaPrefs.edit().putInt("speiseplan_hash", aktuellerHash).apply()
            speichereEinkaufslisten()
        } else {
            println("DEBUG: Keine Änderung im Speiseplan und Listen vorhanden → keine Neugenerierung")
        }

        aktualisiereListe()
    }

    private fun zeigeDialogNeueListe() {
        val dialogView = layoutInflater.inflate(R.layout.activity_vorlage_dialog_neue_liste, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etDatum = dialogView.findViewById<EditText>(R.id.etDatum)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerVorlagen)

        val prefs = getSharedPreferences("vorlagen_pref", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("vorlagenListe", null)

        val vorlagenListe = mutableListOf("Keine Vorlage")
        val vorlagenMap = mutableMapOf<String, EinkaufslisteVorlage>()

        if (!json.isNullOrEmpty()) {
            val typ = object : TypeToken<MutableList<EinkaufslisteVorlage>>() {}.type
            val vorlagen = gson.fromJson<MutableList<EinkaufslisteVorlage>>(json, typ)
            for (vorlage in vorlagen) {
                vorlagenListe.add(vorlage.name)
                vorlagenMap[vorlage.name] = vorlage
            }
        }

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, vorlagenListe)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        AlertDialog.Builder(this)
            .setTitle("Neue Einkaufsliste")
            .setView(dialogView)
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Anlegen") { _, _ ->
                val datumEingabe = etDatum.text.toString().trim()
                val nameEingabe = etName.text.toString().trim()

                val finalName = if (nameEingabe.isEmpty()) {
                    var baseName = generiereNächstenTitel()
                    if (datumEingabe.isNotEmpty()) baseName += " – $datumEingabe"
                    baseName
                } else {
                    if (datumEingabe.isNotEmpty()) "$nameEingabe – $datumEingabe" else nameEingabe
                }

                val neueListe = Einkaufsliste(finalName)

                val ausgewählt = spinner.selectedItem?.toString()
                if (!ausgewählt.isNullOrEmpty() && ausgewählt != "Keine Vorlage") {
                    vorlagenMap[ausgewählt]?.artikel?.let { artikelListe ->
                        neueListe.artikel.addAll(artikelListe.map { it.copy() })
                    }
                }

                einkaufslisten.add(neueListe)
                sortiereListenNachDatum()
                speichereEinkaufslisten()
                aktualisiereListe()
            }
            .show()
    }

    private fun sortiereListenNachDatum() {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        einkaufslisten.sortWith(compareBy { liste ->
            val match = Regex("""\b(\d{2}\.\d{2}\.\d{4})\b""").find(liste.titel)
            val datumString = match?.groupValues?.get(1)
            try {
                LocalDate.parse(datumString, formatter)
            } catch (e: Exception) {
                LocalDate.MAX
            }
        })
    }

    private fun generiereNächstenTitel(): String {
        val kw = berechneAktuelleKW()
        val nummer = einkaufslisten.count { it.titel.startsWith("Einkaufsliste KW$kw") } + 1
        return "Einkaufsliste KW$kw – $nummer"
    }

    private fun berechneAktuelleKW(): Int {
        val heute = LocalDate.now()
        val feld = WeekFields.of(Locale.getDefault())
        return heute.get(feld.weekOfWeekBasedYear())
    }

    private fun ladeEinkaufslisten() {
        einkaufslisten.clear()
        try {
            val fis = openFileInput("einkaufslisten.dat")
            val ois = ObjectInputStream(fis)
            @Suppress("UNCHECKED_CAST")
            einkaufslisten.addAll(ois.readObject() as List<Einkaufsliste>)
            ois.close()
        } catch (e: Exception) {
            println("DEBUG: Keine gespeicherten Einkaufslisten geladen.")
        }
    }

    private fun speichereEinkaufslisten() {
        try {
            val fos = openFileOutput("einkaufslisten.dat", Context.MODE_PRIVATE)
            val oos = ObjectOutputStream(fos)
            oos.writeObject(einkaufslisten)
            oos.close()
            println("DEBUG: Einkaufslisten gespeichert.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun aktualisiereListe() {
        adapter.clear()
        adapter.addAll(einkaufslisten.map { it.titel })
        adapter.notifyDataSetChanged()
    }

    private fun generiereListeAusSpeiseplan() {
        val prefs = getSharedPreferences("speiseplan", Context.MODE_PRIVATE)
        val daten = prefs.getString("wochenplan", null) ?: return

        val rezeptPrefs = getSharedPreferences("rezepte", Context.MODE_PRIVATE)
        val rezeptRohdaten = rezeptPrefs.getString("rezeptListe", null) ?: return

        val artikelPrefs = getSharedPreferences("artikel_pref", Context.MODE_PRIVATE)
        val artikelRohdaten = artikelPrefs.getString("artikelListe", null) ?: return

        val artikelMap = artikelRohdaten.split(";;").mapNotNull { zeile ->
            val teile = zeile.split("::")
            if (teile.size >= 6) {
                val haltbarkeit = teile[3].toIntOrNull() ?: 1
                val artikel = Artikel(
                    name = teile[0],
                    kategorie = teile[1],
                    kuehlung = teile[2].toBoolean(),
                    haltbarkeit = haltbarkeit,
                    menge = teile[4],
                    rechnungsart = teile[5],
                    bildPfad = if (teile.size >= 7) teile[6] else null
                )
                artikel.name to artikel
            } else null
        }.toMap()

        val rezeptListe = rezeptRohdaten.split(";;").mapNotNull { zeile ->
            val teile = zeile.split("::")
            if (teile.size >= 6) {
                val name = teile[0]
                val beschreibung = teile[1]
                val zutatenText = teile[2]
                val zutaten = zutatenText.split("|").mapNotNull {
                    val infos = it.split(",")
                    val artikelName = infos.getOrNull(0)?.trim() ?: return@mapNotNull null
                    val menge = infos.getOrNull(1)?.trim() ?: ""
                    val base = artikelMap[artikelName] ?: Artikel(
                        name = artikelName,
                        kategorie = "",
                        kuehlung = false,
                        haltbarkeit = 1,
                        menge = menge,
                        rechnungsart = "",
                        bildPfad = null
                    )
                    base.copy(menge = menge)
                }
                val dauer = teile[3].toIntOrNull() ?: 0
                val personen = teile[4].toIntOrNull() ?: 1
                val kategorie = teile[5]
                Rezept(name, beschreibung, zutaten, dauer, personen, kategorie)
            } else null
        }

        val speiseplan = mutableListOf<SpeiseplanTag>()
        daten.split(";;").forEach { zeile ->
            val teile = zeile.split("::")
            if (teile.size >= 6) {
                val tagName = teile[0]
                fun rezeptByName(name: String) = rezeptListe.find { it.name == name }
                val tag = SpeiseplanTag(
                    name = tagName,
                    vorspeise = rezeptByName(teile[1]),
                    hauptspeise = rezeptByName(teile[2]),
                    beilage1 = rezeptByName(teile[3]),
                    beilage2 = rezeptByName(teile[4]),
                    nachspeise = rezeptByName(teile[5])
                )
                if (listOf(tag.vorspeise, tag.hauptspeise, tag.beilage1, tag.beilage2, tag.nachspeise).any { it != null }) {
                    speiseplan.add(tag)
                }
            }
        }

        val neueListen = EinkaufslistenGenerator.generiereAusSpeiseplan(speiseplan)
        einkaufslisten.clear()
        einkaufslisten.addAll(neueListen)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1234 && resultCode == RESULT_OK && data != null) {
            val vorlage = data.getSerializableExtra("vorlage") as? EinkaufslisteVorlage
            if (vorlage != null) {
                val name = generiereNächstenTitel()
                val neueListe = Einkaufsliste(name)
                neueListe.artikel.addAll(vorlage.artikel.map { it.copy() })
                einkaufslisten.add(neueListe)
                speichereEinkaufslisten()
                aktualisiereListe()
                Toast.makeText(this, "Vorlage '${vorlage.name}' übernommen", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
