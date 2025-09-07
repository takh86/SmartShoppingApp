package com.example.smartshopping.artikel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import com.example.smartshopping.R

class ArtikelExpandableAdapter(
    private val context: Context,
    private var kategorien: List<String>,
    private var artikelMap: Map<String, List<Artikel>>
) : BaseExpandableListAdapter() {

    override fun getGroupCount(): Int = kategorien.size

    override fun getChildrenCount(groupPosition: Int): Int {
        return artikelMap[kategorien[groupPosition]]?.size ?: 0
    }

    override fun getGroup(groupPosition: Int): Any = kategorien[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return artikelMap[kategorien[groupPosition]]?.get(childPosition)
            ?: Artikel("", "", false, 0, "", "")
    }

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_expandable_list_item_1, parent, false)

        val textView = view.findViewById<TextView>(android.R.id.text1)

        val kategorie = kategorien[groupPosition]
        val anzahl = artikelMap[kategorie]?.size ?: 0

        // 👁️ Zeige z. B. "Obst (3)" oder "Fleisch (0)"
        textView.text = "$kategorie ($anzahl)"
        textView.setTextAppearance(context, android.R.style.TextAppearance_Medium)

        textView.setPadding(24, textView.paddingTop, textView.paddingRight, textView.paddingBottom)

        return view
    }



    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val artikel = artikelMap[kategorien[groupPosition]]?.get(childPosition)
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.activity_artikel_list_item, parent, false)

        val tvName = view.findViewById<TextView>(R.id.tvArtikelName)
        val tvDetails = view.findViewById<TextView>(R.id.tvArtikelInfo)

        tvName.text = artikel?.name

        val menge = artikel?.menge?.trim() ?: ""
        val haltbarkeit = artikel?.haltbarkeit ?: 0

        // Rechnungsart in passende Einheit umwandeln
        val einheit = when (artikel?.rechnungsart?.lowercase()) {
            "gramm" -> "g"
            "milliliter" -> "ml"
            "stück" -> "Stk"
            else -> ""
        }

        // Verhindert doppelte Einheit (wenn z.B. "200g" bereits enthalten ist)
        val mengeMitEinheit = if (!menge.endsWith(einheit, ignoreCase = true) && einheit.isNotEmpty()) {
            "$menge $einheit"
        } else menge

        tvDetails.text = "Menge: $mengeMitEinheit  |  Haltbarkeit: $haltbarkeit Tage"

        return view
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    fun updateData(newKategorien: List<String>, newArtikelMap: Map<String, List<Artikel>>) {
        kategorien = newKategorien
        artikelMap = newArtikelMap
        notifyDataSetChanged()
    }
}
