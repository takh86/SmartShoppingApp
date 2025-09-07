package com.example.smartshopping.rezept

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.smartshopping.R
import com.example.smartshopping.artikel.Artikel

class RezeptAuswahlActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var btnSpeichern: Button
    private lateinit var etSuche: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var scrollUp: Button
    private lateinit var scrollDown: Button

    private val ausgewaehlteArtikel = mutableListOf<Artikel>()
    private lateinit var originalArtikelListe: List<Artikel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rezept_auswahl)

        container = findViewById(R.id.containerAusgewaehlteZutaten)
        btnSpeichern = findViewById(R.id.btnSpeichernUndZurueck)
        etSuche = findViewById(R.id.etSuche)
        scrollView = findViewById(R.id.artikelScrollView)
        scrollUp = findViewById(R.id.btnScrollUp)
        scrollDown = findViewById(R.id.btnScrollDown)

        originalArtikelListe = ladeArtikel()
        anzeigenGefiltert(originalArtikelListe)

        etSuche.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val gefiltert = originalArtikelListe.filter {
                    it.name.contains(s.toString(), ignoreCase = true)
                }
                anzeigenGefiltert(gefiltert)
            }
        })

        scrollUp.setOnClickListener { scrollView.smoothScrollBy(0, -200) }
        scrollDown.setOnClickListener { scrollView.smoothScrollBy(0, 200) }

        btnSpeichern.setOnClickListener {
            // Artikel dauerhaft speichern
            val prefs = getSharedPreferences("artikel_pref", Context.MODE_PRIVATE)
            val artikelListe = ladeArtikel().toMutableList()

            ausgewaehlteArtikel.forEach { ausgewaehlt ->
                val index = artikelListe.indexOfFirst { it.name == ausgewaehlt.name }
                if (index >= 0) {
                    artikelListe[index] = ausgewaehlt
                } else {
                    artikelListe.add(ausgewaehlt)
                }
            }

            val gespeichert = artikelListe.joinToString(";;") {
                "${it.name}::${it.kategorie}::${it.kuehlung}::${it.haltbarkeit}::${it.menge}::${it.rechnungsart}"
            }
            prefs.edit().putString("artikelListe", gespeichert).apply()

            // Zurückgeben an RezeptAddActivity
            val resultIntent = intent.apply {
                putExtra("ausgewaehlteZutaten", ArrayList(ausgewaehlteArtikel))
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

    }

    private fun anzeigenGefiltert(liste: List<Artikel>) {
        container.removeAllViews()
        val kiloKategorien = listOf("obst", "gemüse", "fleisch")
        val literKategorien = listOf("milchprodukte", "getränke")
        val stueckKategorien = listOf("haushalt")
        var dynamischGewaehltEinheit: String? = null


        liste.forEach { artikel ->
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16, 0, 16)
            }

            val hauptCheckbox = CheckBox(this).apply {
                text = artikel.name
                textSize = 18f
            }

            val unterOptionenLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
            }

            val einheitZeilen = mutableListOf<LinearLayout>()

            fun aktualisiereArtikelListe() {
                ausgewaehlteArtikel.removeIf { it.name == artikel.name }

                if (!hauptCheckbox.isChecked) return

                // Einheit bestimmen (entweder fix oder dynamisch)
                val mengeProEinheit = when (artikel.kategorie.lowercase()) {
                    in kiloKategorien -> "kg"
                    in literKategorien -> "L"
                    in stueckKategorien -> "Stk"
                    else -> dynamischGewaehltEinheit ?: ""
                }

                val gesamtMenge = einheitZeilen.sumOf { row ->
                    val (cb, tvAnzahl, faktor) = row.tag as Triple<CheckBox, TextView, Double>
                    if (cb.isChecked) {
                        val anzahl = tvAnzahl.text.toString().toIntOrNull() ?: 1
                        anzahl * faktor
                    } else 0.0
                }

                if (gesamtMenge > 0.0 && mengeProEinheit.isNotEmpty()) {
                    val mengeText = "%.1f".format(gesamtMenge).replace(",", ".") + mengeProEinheit
                    ausgewaehlteArtikel.add(artikel.copy(menge = mengeText))
                }
            }



            fun createEinheitZeile(label: String, faktor: Double): LinearLayout {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 8, 0, 8)
                }

                val cb = CheckBox(this).apply { text = label }
                val btnPlus = Button(this).apply { text = "+" }
                val tvAnzahl = TextView(this).apply {
                    text = "1"
                    setPadding(20, 0, 20, 0)
                    textSize = 16f
                }
                val btnMinus = Button(this).apply { text = "-" }

                var anzahl = 1
                btnPlus.setOnClickListener {
                    anzahl++
                    tvAnzahl.text = anzahl.toString()
                    aktualisiereArtikelListe()
                }
                btnMinus.setOnClickListener {
                    if (anzahl > 1) {
                        anzahl--
                        tvAnzahl.text = anzahl.toString()
                        aktualisiereArtikelListe()
                    }
                }

                row.tag = Triple(cb, tvAnzahl, faktor)
                cb.setOnCheckedChangeListener { _, _ -> aktualisiereArtikelListe() }

                row.addView(cb)
                row.addView(btnPlus)
                row.addView(tvAnzahl)
                row.addView(btnMinus)
                return row
            }

            when (artikel.kategorie.lowercase()) {
                in kiloKategorien -> {
                    einheitZeilen += createEinheitZeile("1/2 kg", 0.5)
                    einheitZeilen += createEinheitZeile("1 kg", 1.0)
                }
                in literKategorien -> {
                    einheitZeilen += createEinheitZeile("1/2 L", 0.5)
                    einheitZeilen += createEinheitZeile("1 L", 1.0)
                }
                in stueckKategorien -> {
                    einheitZeilen += createEinheitZeile("1 Stk", 1.0)
                }
                in listOf("sonstige", "sonstiges") -> {
                    val auswahlenLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val radioGroup = RadioGroup(this).apply {
                        orientation = RadioGroup.HORIZONTAL
                    }

                    val optionen = listOf("kg", "L", "Stk")
                    val faktorMap = mapOf("kg" to 1.0, "L" to 1.0, "Stk" to 1.0)

                    optionen.forEach { einheit ->
                        val rb = RadioButton(this).apply { text = einheit }
                        radioGroup.addView(rb)
                    }

                    auswahlenLayout.addView(TextView(this).apply {
                        text = "Einheit: "
                        setPadding(0, 0, 8, 0)
                    })
                    auswahlenLayout.addView(radioGroup)
                    unterOptionenLayout.addView(auswahlenLayout)

                    radioGroup.setOnCheckedChangeListener { _, checkedId ->
                        val gewaehlteEinheit = findViewById<RadioButton>(checkedId)?.text?.toString()
                        dynamischGewaehltEinheit = gewaehlteEinheit
                        einheitZeilen.clear()
                        unterOptionenLayout.removeAllViews()
                        unterOptionenLayout.addView(auswahlenLayout)

                        when (gewaehlteEinheit) {
                            "kg" -> {
                                einheitZeilen += createEinheitZeile("1/2 kg", 0.5)
                                einheitZeilen += createEinheitZeile("1 kg", 1.0)
                            }
                            "L" -> {
                                einheitZeilen += createEinheitZeile("1/2 L", 0.5)
                                einheitZeilen += createEinheitZeile("1 L", 1.0)
                            }
                            "Stk" -> {
                                einheitZeilen += createEinheitZeile("1 Stk", 1.0)
                            }
                        }


                        einheitZeilen.forEach { unterOptionenLayout.addView(it) }
                        aktualisiereArtikelListe()

                    }
                }
            }


            einheitZeilen.forEach { unterOptionenLayout.addView(it) }
            aktualisiereArtikelListe()


            layout.addView(hauptCheckbox)
            layout.addView(unterOptionenLayout)
            container.addView(layout)

            hauptCheckbox.setOnCheckedChangeListener { _, isChecked ->
                unterOptionenLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
                aktualisiereArtikelListe()
            }
        }
    }

    private fun ladeArtikel(): List<Artikel> {
        val prefs = getSharedPreferences("artikel_pref", Context.MODE_PRIVATE)
        val gespeichert = prefs.getString("artikelListe", null) ?: return emptyList()

        return gespeichert.split(";;").mapNotNull { zeile ->
            val teile = zeile.split("::")
            if (teile.size >= 6) {
                Artikel(
                    name = teile[0],
                    kategorie = teile[1],
                    kuehlung = teile[2].toBooleanStrictOrNull() ?: false,
                    haltbarkeit = teile[3].toIntOrNull() ?: 0,
                    menge = teile[4],
                    rechnungsart = teile[5],
                    bildPfad = null
                )
            } else null
        }.sortedBy { it.name.lowercase() }
    }
}
