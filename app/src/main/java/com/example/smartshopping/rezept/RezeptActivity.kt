package com.example.smartshopping.rezept

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.smartshopping.R
import com.example.smartshopping.artikel.Artikel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RezeptActivity : AppCompatActivity() {

    private lateinit var rezeptListe: MutableList<Rezept>
    private lateinit var fabAddArtikel: FloatingActionButton
    private lateinit var expandableListView: ExpandableListView

    private val bekannteRezeptKategorien = listOf(
        "Vorspeisen", "Hauptspeisen", "Beilagen", "Nachtisch"
    )

    companion object {
        const val REZEPT_HINZUFUEGEN_REQUEST = 100
        const val REZEPT_BEARBEITEN_REQUEST = 200
        const val REZEPT_DETAIL_REQUEST = 300
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rezept)

        fabAddArtikel = findViewById(R.id.fabAddArtikel)
        expandableListView = findViewById(R.id.expandableRezeptListe)

        rezeptListe = ladeRezepte().filter { it.kategorie != "Allgemein" }.toMutableList()

        updateExpandableListView()

        fabAddArtikel.setOnClickListener {
            val intent = Intent(this, RezeptAddActivity::class.java)
            startActivityForResult(intent, REZEPT_HINZUFUEGEN_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (data.hasExtra("deleteIndex")) {
                val deleteIndex = data.getIntExtra("deleteIndex", -1)
                if (deleteIndex >= 0 && deleteIndex < rezeptListe.size) {
                    rezeptListe.removeAt(deleteIndex)
                    speichereRezepte()
                    updateExpandableListView()
                }
            } else if (requestCode == REZEPT_HINZUFUEGEN_REQUEST && data.hasExtra("neuesRezept") && data.hasExtra("rezeptIndex")) {
                val neuesRezept = data.getSerializableExtra("neuesRezept") as? Rezept
                val index = data.getIntExtra("rezeptIndex", -1)

                if (neuesRezept != null && index >= 0) {
                    if (index < rezeptListe.size) {
                        rezeptListe[index] = neuesRezept
                    } else {
                        rezeptListe.add(neuesRezept)
                    }
                    speichereRezepte()
                    updateExpandableListView()
                }
            } else if (requestCode == REZEPT_BEARBEITEN_REQUEST && data.hasExtra("neuesRezept") && data.hasExtra("rezeptIndex")) {
                val neuesRezept = data.getSerializableExtra("neuesRezept") as? Rezept
                val index = data.getIntExtra("rezeptIndex", -1)

                if (neuesRezept != null && index >= 0 && index < rezeptListe.size) {
                    rezeptListe[index] = neuesRezept
                    speichereRezepte()
                    updateExpandableListView()
                }
            } else if (requestCode == REZEPT_DETAIL_REQUEST && data.hasExtra("neuesRezept") && data.hasExtra("rezeptIndex")) {
                val neuesRezept = data.getSerializableExtra("neuesRezept") as? Rezept
                val index = data.getIntExtra("rezeptIndex", -1)

                if (neuesRezept != null && index >= 0 && index < rezeptListe.size) {
                    rezeptListe[index] = neuesRezept
                    speichereRezepte()
                    updateExpandableListView()
                }
            }
        }
    }




    private fun updateExpandableListView() {
        val gruppiert = rezeptListe.groupBy { it.kategorie ?: "Sonstiges" }

        val rezeptMap = bekannteRezeptKategorien.associateWith { k ->
            gruppiert[k] ?: emptyList()
        }

        val gruppenNamen = rezeptMap.keys.toList()
        val rezeptNamen = gruppenNamen.map { rezeptMap[it]?.map { r -> r.name } ?: listOf() }

        val adapter = object : BaseExpandableListAdapter() {
            override fun getGroupCount(): Int = gruppenNamen.size
            override fun getChildrenCount(groupPosition: Int): Int = rezeptNamen[groupPosition].size
            override fun getGroup(groupPosition: Int): Any = gruppenNamen[groupPosition]
            override fun getChild(groupPosition: Int, childPosition: Int): Any = rezeptNamen[groupPosition][childPosition]
            override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()
            override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()
            override fun hasStableIds(): Boolean = false
            override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

            override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: layoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
                val textView = view.findViewById<TextView>(android.R.id.text1)

                val name = gruppenNamen[groupPosition]
                val anzahl = rezeptNamen[groupPosition].size

                textView.text = "$name ($anzahl)"
                textView.setTextAppearance(this@RezeptActivity, android.R.style.TextAppearance_Medium)
                textView.setTypeface(null, android.graphics.Typeface.BOLD)
                textView.setPadding(24, textView.paddingTop, textView.paddingRight, textView.paddingBottom)

                return view
            }


            override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
                val rezeptName = getChild(groupPosition, childPosition).toString()
                val rezept = rezeptListe.find { it.name == rezeptName }

                return LinearLayout(this@RezeptActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(60, 16, 16, 16)

                    val name = TextView(this@RezeptActivity).apply {
                        text = rezept?.name ?: rezeptName
                        textSize = 16f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }

                    val dauerInMin = rezept?.dauer ?: 0
                    val stunden = dauerInMin / 60
                    val minuten = dauerInMin % 60
                    val dauerText = if (stunden > 0) {
                        "${stunden} h ${minuten} min"
                    } else {
                        "${minuten} min"
                    }

                    val details = TextView(this@RezeptActivity).apply {
                        text = "Dauer: $dauerText | Portionen: ${rezept?.personen ?: 1}"
                        textSize = 14f
                    }


                    val beschreibung = TextView(this@RezeptActivity).apply {
                        text = rezept?.beschreibung ?: ""
                        setTextColor(resources.getColor(R.color.gray, null))
                        textSize = 14f
                    }

                    addView(name)
                    addView(details)
                    addView(beschreibung)
                }
            }
        }

        expandableListView.setAdapter(adapter)

        expandableListView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val rezeptName = rezeptNamen[groupPosition][childPosition]
            val rezept = rezeptListe.find { it.name == rezeptName }
            rezept?.let {
                val intent = Intent(this, RezeptDetailActivity::class.java).apply {
                    putExtra("rezept", it)
                    putExtra("rezeptIndex", rezeptListe.indexOf(it))
                }
                startActivityForResult(intent, REZEPT_DETAIL_REQUEST)
            }
            true
        }

        expandableListView.setOnItemLongClickListener { _, _, flatPos, _ ->
            val packedPos = expandableListView.getExpandableListPosition(flatPos)
            if (ExpandableListView.getPackedPositionType(packedPos) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                val groupPos = ExpandableListView.getPackedPositionGroup(packedPos)
                val childPos = ExpandableListView.getPackedPositionChild(packedPos)
                val rezeptName = rezeptNamen[groupPos][childPos]
                val rezept = rezeptListe.find { it.name == rezeptName }

                rezept?.let {
                    val intent = Intent(this, RezeptAddActivity::class.java).apply {
                        putExtra("rezept", it)
                        putExtra("rezeptIndex", rezeptListe.indexOf(it))
                    }
                    startActivityForResult(intent, REZEPT_BEARBEITEN_REQUEST)
                }
                true
            } else {
                false
            }
        }
    }

    private fun ladeRezepte(): MutableList<Rezept> {
        val prefs = getSharedPreferences("rezepte", Context.MODE_PRIVATE)
        val gespeichert = prefs.getString("rezeptListe", null) ?: return mutableListOf()

        return gespeichert.split(";;").mapNotNull { zeile ->
            val teile = zeile.split("::")
            if (teile.size < 6) return@mapNotNull null
            val name = teile[0]
            val beschreibung = teile[1]
            val zutaten = teile[2].split("|").mapNotNull {
                val infos = it.split(",")
                if (infos.size >= 2) Artikel(infos[0], "", false, 0, infos[1], "", null)
                else null
            }
            val dauer = teile[3].toIntOrNull() ?: 0
            val personen = teile[4].toIntOrNull() ?: 1
            val kategorie = teile[5]
            Rezept(name, beschreibung, zutaten, dauer, personen, kategorie)
        }.toMutableList()
    }


    private fun speichereRezepte() {
        val prefs = getSharedPreferences("rezepte", Context.MODE_PRIVATE)
        val rezeptText = rezeptListe.joinToString(";;") { rezept ->
            val zutatenText = rezept.zutaten.joinToString("|") { "${it.name},${it.menge}" }
            "${rezept.name}::${rezept.beschreibung}::${zutatenText}::${rezept.dauer}::${rezept.personen}::${rezept.kategorie}"
        }
        prefs.edit().putString("rezeptListe", rezeptText).apply()
    }
}
