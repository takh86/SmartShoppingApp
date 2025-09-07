package com.example.smartshopping

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smartshopping.artikel.ArtikelActivity
import com.example.smartshopping.einkaufsliste.EinkaufslisteActivity
import com.example.smartshopping.rezept.RezeptActivity
import com.example.smartshopping.speiseplan.SpeiseplanActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.graphics.Typeface


class MainActivity : AppCompatActivity() {

    private lateinit var btnRezept: Button
    private lateinit var btnSpeiseplan: Button
    private lateinit var tagesgerichtText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnArtikel = findViewById<Button>(R.id.btnArtikel)
        btnRezept = findViewById(R.id.btnRezept)
        btnSpeiseplan = findViewById(R.id.btnSpeiseplan)
        val btnEinkaufsliste = findViewById<Button>(R.id.btnEinkaufsliste)
        tagesgerichtText = findViewById(R.id.tagesgerichtText)

        btnArtikel.setOnClickListener {
            startActivity(Intent(this, ArtikelActivity::class.java))
        }

        btnRezept.setOnClickListener {
            startActivity(Intent(this, RezeptActivity::class.java))
        }

        btnSpeiseplan.setOnClickListener {
            startActivity(Intent(this, SpeiseplanActivity::class.java))
        }

        btnEinkaufsliste.setOnClickListener {
            startActivity(Intent(this, EinkaufslisteActivity::class.java))
        }

        zeigeTagesgericht()
    }

    override fun onResume() {
        super.onResume()

        val artikelVorhanden = hatMindestensEinenArtikel()
        val rezeptVorhanden = hatMindestensEinRezept()

        btnRezept.isEnabled = artikelVorhanden
        btnRezept.alpha = if (artikelVorhanden) 1.0f else 0.5f

        btnSpeiseplan.isEnabled = rezeptVorhanden
        btnSpeiseplan.alpha = if (rezeptVorhanden) 1.0f else 0.5f

        if (!rezeptVorhanden) {
            val speiseplanPrefs = getSharedPreferences("speiseplan", Context.MODE_PRIVATE)
            speiseplanPrefs.edit().clear().apply()
        }

        zeigeTagesgericht()
    }

    private fun hatMindestensEinenArtikel(): Boolean {
        val prefs = getSharedPreferences("artikel_pref", Context.MODE_PRIVATE)
        val daten = prefs.getString("artikelListe", null)
        if (daten.isNullOrEmpty()) return false

        return daten.split(";;").any { eintrag ->
            val teile = eintrag.split("::")
            teile.size >= 2 && teile[0].isNotBlank() && teile[1].isNotBlank()
        }
    }

    private fun hatMindestensEinRezept(): Boolean {
        val prefs = getSharedPreferences("rezepte", Context.MODE_PRIVATE)
        val daten = prefs.getString("rezeptListe", null) ?: return false

        return daten.split(";;").any { eintrag ->
            val teile = eintrag.split("::")
            teile.size >= 6 && teile[0].isNotBlank()
        }
    }

    private fun zeigeTagesgericht() {
        val prefs = getSharedPreferences("speiseplan", Context.MODE_PRIVATE)
        val daten = prefs.getString("wochenplan", null)

        if (daten.isNullOrEmpty()) {
            tagesgerichtText.text = "Kein Tagesgericht vorhanden"
            return
        }

        val planMap = daten.split(";;").mapNotNull { zeile ->
            val teile = zeile.split("::")
            if (teile.size >= 6) {
                teile[0] to mapOf(
                    "vorspeise" to teile[1].ifBlank { null },
                    "hauptspeise" to teile[2].ifBlank { null },
                    "beilage1" to teile[3].ifBlank { null },
                    "beilage2" to teile[4].ifBlank { null },
                    "nachspeise" to teile[5].ifBlank { null }
                )
            } else null
        }.toMap()

        val wochentage = listOf(
            "Montag", "Dienstag", "Mittwoch",
            "Donnerstag", "Freitag", "Samstag", "Sonntag"
        )

        val heute = LocalDate.now()

        for (offset in 0 until 7) {
            val datum = heute.plusDays(offset.toLong())
            val dayIndex = (datum.dayOfWeek.value + 6) % 7 // Sonntag → 6
            val tagName = wochentage[dayIndex]

            val gericht = planMap[tagName]
            if (gericht != null && gericht.values.any { it != null }) {
                val deutschesDatum = datum.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                val spannable = SpannableStringBuilder()

                val kopfzeile = "Tagesgericht für $deutschesDatum\n\n"
                spannable.append(kopfzeile)
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    kopfzeile.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                spannable.append("• Vorspeise: ${gericht["vorspeise"] ?: "-"}\n")
                spannable.append("• Hauptspeise: ${gericht["hauptspeise"] ?: "-"}\n")
                spannable.append("• Beilage 1: ${gericht["beilage1"] ?: "-"}\n")
                spannable.append("• Beilage 2: ${gericht["beilage2"] ?: "-"}\n")
                spannable.append("• Nachspeise: ${gericht["nachspeise"] ?: "-"}")

                tagesgerichtText.text = spannable
                return
            }
        }

        tagesgerichtText.text = "Kein Tagesgericht verfügbar"
    }


}
