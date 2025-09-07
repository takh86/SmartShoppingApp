package com.example.smartshopping.artikel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.smartshopping.R
import java.io.File
import java.io.FileOutputStream

class ArtikelAddActivity : AppCompatActivity() {

    private var bearbeiteIndex: Int? = null
    private val kategorienListe = listOf("Obst", "Gemüse", "Milchprodukte", "Fleisch", "Getränke", "Haushalt", "Sonstige")

    private lateinit var ivFoto: ImageView
    private var aktuellesFotoPfad: String? = null

    private val kameraLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                val dateiname = "artikel_${System.currentTimeMillis()}.jpg"
                val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), dateiname)
                FileOutputStream(file).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                aktuellesFotoPfad = file.absolutePath
                ivFoto.setImageBitmap(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artikel_add)

        val etName = findViewById<EditText>(R.id.etName)
        val spKategorie = findViewById<Spinner>(R.id.spKategorie)
        val cbKuehlung = findViewById<CheckBox>(R.id.cbKuehlung)
        val etHaltbarkeitTage = findViewById<EditText>(R.id.etHaltbarkeitTage)
        val btnPlusHaltbarkeit = findViewById<Button>(R.id.btnPlusHaltbarkeit)
        val btnMinusHaltbarkeit = findViewById<Button>(R.id.btnMinusHaltbarkeit)
        val btnSpeichern = findViewById<Button>(R.id.btnSpeichern)
        val btnFoto = findViewById<Button>(R.id.btnFoto)
        ivFoto = findViewById(R.id.ivFoto)

        spKategorie.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kategorienListe).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        btnPlusHaltbarkeit.setOnClickListener {
            val aktuellerWert = etHaltbarkeitTage.text.toString().toIntOrNull() ?: 1
            etHaltbarkeitTage.setText((aktuellerWert + 1).toString())
        }

        btnMinusHaltbarkeit.setOnClickListener {
            val aktuellerWert = etHaltbarkeitTage.text.toString().toIntOrNull() ?: 0
            if (aktuellerWert > 1) {
                etHaltbarkeitTage.setText((aktuellerWert - 1).toString())
            }
        }

        val artikel = intent.getSerializableExtra("artikel") as? Artikel
        bearbeiteIndex = intent.getIntExtra("artikelIndex", -1).takeIf { it >= 0 }

        if (artikel != null) {
            etName.setText(artikel.name)
            spKategorie.setSelection(kategorienListe.indexOfFirst { it.equals(artikel.kategorie, true) })
            cbKuehlung.isChecked = artikel.kuehlung
            etHaltbarkeitTage.setText(artikel.haltbarkeit.toString())
            artikel.bildPfad?.let {
                val bild = File(it)
                if (bild.exists()) {
                    ivFoto.setImageBitmap(android.graphics.BitmapFactory.decodeFile(it))
                    aktuellesFotoPfad = it
                }
            }
        }

        btnFoto.setOnClickListener {
            val kameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            kameraLauncher.launch(kameraIntent)
        }

        btnSpeichern.setOnClickListener {
            val name = etName.text.toString().trim()
            val kategorie = spKategorie.selectedItem.toString()
            val kuehlung = cbKuehlung.isChecked

            if (name.isEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Fehlender Name")
                    .setMessage("Bitte gib einen Artikelnamen ein.")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }

            if (etHaltbarkeitTage.text.isNullOrBlank()) {
                etHaltbarkeitTage.setText("1")
            }

            val haltbarkeitTage = etHaltbarkeitTage.text.toString().trim().toIntOrNull()
            if (haltbarkeitTage == null || haltbarkeitTage <= 0) {
                Toast.makeText(this, "Bitte gültige Haltbarkeit in Tagen eingeben", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔁 Duplikatsprüfung ohne anyIndexed
            val vorhandeneArtikel = ladeAlleArtikel()
            for ((index, artikel) in vorhandeneArtikel.withIndex()) {
                if (artikel.name.equals(name, ignoreCase = true) && index != bearbeiteIndex) {
                    AlertDialog.Builder(this)
                        .setTitle("Artikel bereits vorhanden")
                        .setMessage("Ein Artikel mit diesem Namen existiert bereits.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@setOnClickListener
                }
            }

            // ✅ Artikel erstellen
            val artikelNeu = Artikel(
                name = name,
                kategorie = kategorie,
                kuehlung = kuehlung,
                haltbarkeit = haltbarkeitTage,
                menge = "",
                rechnungsart = "",
                bildPfad = aktuellesFotoPfad
            )

            val prefs = getSharedPreferences("artikel_pref", Context.MODE_PRIVATE)
            val gespeichert = vorhandeneArtikel.toMutableList()

            if (bearbeiteIndex != null) {
                gespeichert[bearbeiteIndex!!] = artikelNeu
            } else {
                gespeichert.add(artikelNeu)
            }

            val daten = gespeichert.joinToString(";;") { art ->
                listOf(
                    art.name,
                    art.kategorie,
                    art.kuehlung.toString(),
                    art.haltbarkeit,
                    art.menge,
                    art.rechnungsart,
                    art.bildPfad ?: ""
                ).joinToString("::")
            }
            prefs.edit().putString("artikelListe", daten).apply()

            val neuerIndex = gespeichert.indexOfFirst { it.name.equals(artikelNeu.name, ignoreCase = true) }

            val resultIntent = Intent().apply {
                putExtra("artikel", artikelNeu)
                putExtra("artikelIndex", neuerIndex)
                putExtra("nurAnzeigen", true)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

    }

    private fun ladeAlleArtikel(): List<Artikel> {
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
                    menge = teile.getOrNull(4) ?: "",
                    rechnungsart = teile.getOrNull(5) ?: "",
                    bildPfad = teile.getOrNull(6)
                )
            } else null
        }
    }

    override fun onBackPressed() {
        val etName = findViewById<EditText>(R.id.etName)
        val etHaltbarkeitTage = findViewById<EditText>(R.id.etHaltbarkeitTage)

        if (etName.text.isNotBlank() || etHaltbarkeitTage.text.isNotBlank() || aktuellesFotoPfad != null) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Artikel nicht gespeichert")
                .setMessage("Möchtest du den Artikel speichern?")
                .setPositiveButton("ja") { _, _ ->
                    findViewById<Button>(R.id.btnSpeichern).performClick()
                }
                .setNegativeButton("nein") { _, _ -> super.onBackPressed() }
                .setNeutralButton("abbrechen", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}
