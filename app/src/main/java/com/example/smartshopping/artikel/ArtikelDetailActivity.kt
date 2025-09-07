package com.example.smartshopping.artikel

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.smartshopping.R
import java.io.File

class ArtikelDetailActivity : AppCompatActivity() {

    private var artikel: Artikel? = null
    private var artikelIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artikel_detail)

        artikel = intent.getSerializableExtra("artikel") as? Artikel
        artikelIndex = intent.getIntExtra("artikelIndex", -1)

        val tvName = findViewById<TextView>(R.id.tvArtikelName)
        val tvKategorie = findViewById<TextView>(R.id.tvKategorie)
        val tvKuehlung = findViewById<TextView>(R.id.tvKuehlung)
        val tvHaltbarkeit = findViewById<TextView>(R.id.tvHaltbarkeit)
        val ivBild = findViewById<ImageView>(R.id.ivArtikelBild)

        val btnBearbeiten = findViewById<Button>(R.id.btnBearbeiten)
        val btnLoeschen = findViewById<Button>(R.id.btnLoeschen)

        aktualisiereAnsicht()

        // 👉 BEARBEITEN
        btnBearbeiten.setOnClickListener {
            val intent = Intent(this, ArtikelAddActivity::class.java).apply {
                putExtra("artikel", artikel)
                putExtra("artikelIndex", artikelIndex)
            }
            startActivityForResult(intent, 2002)
        }

        // ❌ LÖSCHEN
        btnLoeschen.setOnClickListener {
            artikel?.let {
                AlertDialog.Builder(this)
                    .setTitle("Artikel löschen")
                    .setMessage("Möchtest du den Artikel „${it.name}“ wirklich löschen?")
                    .setPositiveButton("Ja") { _, _ ->
                        val resultIntent = Intent().apply {
                            putExtra("artikelIndex", artikelIndex)
                            putExtra("loeschen", true)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                    .setNegativeButton("Abbrechen", null)
                    .show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2002 && resultCode == RESULT_OK && data != null) {
            val aktualisiert = data.getSerializableExtra("artikel") as? Artikel
            if (aktualisiert != null && aktualisiert != artikel) {
                artikel = aktualisiert
                aktualisiereAnsicht()
                setResult(RESULT_OK, data)
            }
        }
    }

    private fun aktualisiereAnsicht() {
        val tvName = findViewById<TextView>(R.id.tvArtikelName)
        val tvKategorie = findViewById<TextView>(R.id.tvKategorie)
        val tvKuehlung = findViewById<TextView>(R.id.tvKuehlung)
        val tvHaltbarkeit = findViewById<TextView>(R.id.tvHaltbarkeit)
        val ivBild = findViewById<ImageView>(R.id.ivArtikelBild)

        artikel?.let { a ->
            tvName.text = "Name: ${a.name}"
            tvKategorie.text = "Kategorie: ${a.kategorie}"
            tvKuehlung.text = "Kühlung: ${if (a.kuehlung) "Ja" else "Nein"}"
            tvHaltbarkeit.text = "Haltbar bis: ${a.haltbarkeit}"

            a.bildPfad?.let { pfad ->
                val bildDatei = File(pfad)
                if (bildDatei.exists()) {
                    ivBild.setImageBitmap(BitmapFactory.decodeFile(pfad))
                } else {
                    ivBild.setImageResource(R.drawable.ic_launcher_background)
                }
            } ?: ivBild.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, ArtikelActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}
