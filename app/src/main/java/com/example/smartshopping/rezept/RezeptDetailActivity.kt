package com.example.smartshopping.rezept

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartshopping.R
import androidx.core.text.HtmlCompat

class RezeptDetailActivity : AppCompatActivity() {

    private lateinit var tvRezeptDetails: TextView
    private lateinit var btnBearbeiten: Button
    private lateinit var btnLoeschen: Button
    private lateinit var btnSpeichern: Button
    private lateinit var rezept: Rezept
    private var rezeptIndex: Int = -1

    companion object {
        const val REZEPT_BEARBEITEN_REQUEST = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rezept_detail)

        title = "Rezeptdetails"

        tvRezeptDetails = findViewById(R.id.tvRezeptDetails)
        btnBearbeiten = findViewById(R.id.btnBearbeiten)
        btnLoeschen = findViewById(R.id.btnLoeschen)
        btnSpeichern = findViewById(R.id.btnSpeichern)
        btnSpeichern.visibility = Button.GONE

        rezept = intent.getSerializableExtra("rezept") as Rezept
        rezeptIndex = intent.getIntExtra("rezeptIndex", -1)

        zeigeDetails()

        btnBearbeiten.setOnClickListener {
            val intent = Intent(this, RezeptAddActivity::class.java).apply {
                putExtra("rezept", rezept)
                putExtra("rezeptIndex", rezeptIndex)
            }
            startActivityForResult(intent, REZEPT_BEARBEITEN_REQUEST)
        }

        btnSpeichern.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("neuesRezept", rezept)
                putExtra("rezeptIndex", rezeptIndex)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        btnLoeschen.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Rezept löschen")
                .setMessage("Möchtest du das Rezept wirklich löschen?")
                .setPositiveButton("Ja") { _, _ ->
                    val resultIntent = Intent().apply {
                        putExtra("deleteIndex", rezeptIndex)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                .setNegativeButton("Abbrechen", null)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REZEPT_BEARBEITEN_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val neuesRezept = data.getSerializableExtra("neuesRezept") as? Rezept
            val index = data.getIntExtra("rezeptIndex", -1)

            if (neuesRezept != null && index >= 0) {
                rezept = neuesRezept
                zeigeDetails()

                // Ergebnis sofort zurück an RezeptActivity geben
                val resultIntent = Intent().apply {
                    putExtra("neuesRezept", rezept)
                    putExtra("rezeptIndex", index)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    private fun zeigeDetails() {
        val zutatenText = rezept.zutaten.joinToString("<br>") {
            val mengeMitLeerzeichen = it.menge.replace(
                Regex("""(\d+(?:[.,]\d+)?)([a-zA-Z]+)"""),
                "$1 $2"
            )
            "&nbsp;&nbsp;&nbsp;&nbsp;- ${it.name}, $mengeMitLeerzeichen"
        }

        val dauerInMin = rezept.dauer
        val stunden = dauerInMin / 60
        val minuten = dauerInMin % 60
        val dauerText = if (stunden > 0)
            "$stunden h ${minuten} min"
        else
            "$minuten min"

        val beschreibungMitZeilenumbruch = rezept.beschreibung.replace("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")

        val inhaltHtml = """
        <div style="line-height: 1.6;">
            <b>🍝 Rezept:</b> ${rezept.name}<br><br>
            <b>Kategorie:</b> ${rezept.kategorie}<br><br>

            <b>⏱ Dauer:</b> $dauerText<br><br>
            <b>👥 Portionen:</b> ${rezept.personen}<br><br>

            <b>📝 Beschreibung:</b><br>
            &nbsp;&nbsp;&nbsp;&nbsp;$beschreibungMitZeilenumbruch<br><br>

            <b>🧾 Zutaten:</b><br>
            $zutatenText
        </div>
    """.trimIndent()

        tvRezeptDetails.text = HtmlCompat.fromHtml(inhaltHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }


}
