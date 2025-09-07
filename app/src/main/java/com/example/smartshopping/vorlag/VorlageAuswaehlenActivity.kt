package com.example.smartshopping.vorlag

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.smartshopping.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import android.view.View
import android.view.ViewGroup

class VorlageAuswaehlenActivity : AppCompatActivity() {

    private val vorlagen = mutableListOf<EinkaufslisteVorlage>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var listView: ListView
    private lateinit var btnNeueVorlage: Button
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var gson: Gson

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vorlage_auswaehlen)

        listView = findViewById(R.id.lvVorlagen)
        btnNeueVorlage = findViewById(R.id.btnNeueVorlage)

        prefs = getSharedPreferences("vorlagen_pref", Context.MODE_PRIVATE)
        gson = Gson()

        ladeVorlagen()

        adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            vorlagen.map { it.name }
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val context = this@VorlageAuswaehlenActivity

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
                        2
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
            val vorlage = vorlagen[position]
            val intent = Intent(this, VorlageBearbeitenActivity::class.java)

            // Übergabe der Vorlage als JSON (statt Serializable)
            val vorlageJson = gson.toJson(vorlage)
            intent.putExtra("vorlage_json", vorlageJson)

            startActivity(intent)
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            val zuLöschendeVorlage = vorlagen[position]

            val dialog = AlertDialog.Builder(this)
                .setTitle("Vorlage löschen")
                .setMessage("Möchtest du die Vorlage '${zuLöschendeVorlage.name}' wirklich löschen?")
                .setPositiveButton("Ja") { _, _ ->
                    vorlagen.removeAt(position)
                    speichereVorlagen()
                    aktualisiereListe()
                    Toast.makeText(this, "Vorlage gelöscht", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Nein", null)
                .create()

            dialog.show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isAllCaps = false
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.isAllCaps = false

            true
        }


        btnNeueVorlage.setOnClickListener {
            zeigeVorlageErstellenDialog()
        }
    }

    private fun ladeVorlagen() {
        vorlagen.clear()
        val json = prefs.getString("vorlagenListe", null)
        if (json != null) {
            val typ = object : TypeToken<MutableList<EinkaufslisteVorlage>>() {}.type
            val geladeneVorlagen = gson.fromJson<MutableList<EinkaufslisteVorlage>>(json, typ)
            vorlagen.addAll(geladeneVorlagen)
        }
    }

    private fun speichereVorlagen() {
        val editor = prefs.edit()
        editor.putString("vorlagenListe", gson.toJson(vorlagen))
        editor.apply()
    }

    private fun zeigeVorlageErstellenDialog() {
        // Eingabefeld
        val input = EditText(this).apply {
            hint = "Name der Vorlage"
            setPadding(24, 16, 24, 16) // Innenabstand im Eingabefeld
        }

        // Container mit Randabstand zum Dialog
        val container = FrameLayout(this).apply {
            setPadding(32, 16, 32, 0) // Außenabstand im Dialog
            addView(input)
        }

        // Dialog bauen
        val dialog = AlertDialog.Builder(this)
            .setTitle("Neue Vorlage")
            .setView(container)
            .setPositiveButton("Erstellen") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Bitte einen Namen eingeben", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val nameKlein = name.lowercase()
                val existiert = vorlagen.any { it.name.lowercase() == nameKlein }

                if (existiert) {
                    Toast.makeText(this, "Vorlage '$name' existiert bereits", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                vorlagen.add(EinkaufslisteVorlage(name, mutableListOf()))
                speichereVorlagen()
                aktualisiereListe()
            }
            .setNegativeButton("Abbrechen", null)
            .create()

        dialog.show()

        // 👉 Button-Beschriftungen klein (nicht in Großbuchstaben)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isAllCaps = false
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.isAllCaps = false
    }


    private fun aktualisiereListe() {
        adapter.clear()
        adapter.addAll(vorlagen.map { it.name })
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        ladeVorlagen()
        aktualisiereListe()
    }
}
