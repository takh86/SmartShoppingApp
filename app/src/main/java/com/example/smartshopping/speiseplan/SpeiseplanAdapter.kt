package com.example.smartshopping.speiseplan

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartshopping.R
import com.example.smartshopping.rezept.Rezept

class SpeiseplanAdapter(
    private val context: Context,
    private val speiseplan: List<SpeiseplanTag>,
    private val rezepte: List<Rezept>,
    private val onItemClick: (SpeiseplanTag, Int) -> Unit
) : RecyclerView.Adapter<SpeiseplanAdapter.SpeiseplanViewHolder>() {

    private val expandedStates = BooleanArray(speiseplan.size) { false }
    private val prefs: SharedPreferences =
        context.getSharedPreferences("einkaufsliste_prefs", Context.MODE_PRIVATE)

    class SpeiseplanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvWochentag: TextView = itemView.findViewById(R.id.tvWochentag)
        val iconExpand: ImageView = itemView.findViewById(R.id.iconExpand)
        val btnAddMeal: ImageButton = itemView.findViewById(R.id.btnAddMeal)
        val btnDeleteMeal: ImageButton = itemView.findViewById(R.id.btnDeleteMeal)
        val detailContainer: LinearLayout = itemView.findViewById(R.id.detailContainer)
        val tvVorspeise: TextView = itemView.findViewById(R.id.tvVorspeise)
        val tvBeleg1: TextView = itemView.findViewById(R.id.tvBelg1)
        val tvHauptspeise: TextView = itemView.findViewById(R.id.tvHauptspeise)
        val tvBeleg2: TextView = itemView.findViewById(R.id.tvBelg2)
        val tvNachspeise: TextView = itemView.findViewById(R.id.tvNachspeise)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeiseplanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_speiseplan_tag_with_icons, parent, false)
        return SpeiseplanViewHolder(view)
    }

    override fun getItemCount(): Int = speiseplan.size

    override fun onBindViewHolder(holder: SpeiseplanViewHolder, position: Int) {
        val tag = speiseplan[position]
        holder.tvWochentag.text = tag.name

        holder.tvVorspeise.text = "Vorspeise: ${tag.vorspeise?.name ?: "-"}"
        holder.tvHauptspeise.text = "Hauptspeise: ${tag.hauptspeise?.name ?: "-"}"
        holder.tvBeleg1.text = "Beilage 1: ${tag.beilage1?.name ?: "-"}"
        holder.tvBeleg2.text = "Beilage 2: ${tag.beilage2?.name ?: "-"}"
        holder.tvNachspeise.text = "Nachspeise: ${tag.nachspeise?.name ?: "-"}"

        val expanded = expandedStates[position]
        holder.detailContainer.visibility = if (expanded) View.VISIBLE else View.GONE

        holder.iconExpand.setImageResource(
            if (expanded) R.drawable.ic_arrow_up_black
            else R.drawable.ic_arrow_down_black
        )

        holder.iconExpand.setOnClickListener {
            val neuExpanded = !expandedStates[position]
            expandedStates[position] = neuExpanded
            holder.detailContainer.visibility = if (neuExpanded) View.VISIBLE else View.GONE
            holder.iconExpand.setImageResource(
                if (neuExpanded) R.drawable.ic_arrow_up_black
                else R.drawable.ic_arrow_down_black
            )
        }

        val alleRezepte = listOfNotNull(
            tag.vorspeise,
            tag.hauptspeise,
            tag.beilage1,
            tag.beilage2,
            tag.nachspeise
        )

        if (alleRezepte.isEmpty()) {
            holder.btnAddMeal.setColorFilter(ContextCompat.getColor(context, R.color.green))
            holder.btnAddMeal.isEnabled = true
            holder.btnAddMeal.alpha = 1.0f
            holder.btnAddMeal.setOnClickListener {
                onItemClick(tag, position)
            }
        } else {
            val prefix = "abgehakt_liste_$position:"
            val hatAbgehakte = alleRezepte.any {
                prefs.getBoolean("$prefix${it.name.trim()}", false)
            }

            if (hatAbgehakte) {
                holder.btnAddMeal.setColorFilter(ContextCompat.getColor(context, R.color.red))
                holder.btnAddMeal.isEnabled = false
                holder.btnAddMeal.alpha = 0.5f
                holder.btnAddMeal.setOnClickListener(null)
            } else {
                holder.btnAddMeal.setColorFilter(ContextCompat.getColor(context, R.color.orange))
                holder.btnAddMeal.isEnabled = true
                holder.btnAddMeal.alpha = 1.0f
                holder.btnAddMeal.setOnClickListener {
                    onItemClick(tag, position)
                }
            }
        }

        holder.btnDeleteMeal.setOnClickListener {
            // Daten aus Objekt entfernen
            tag.vorspeise = null
            tag.hauptspeise = null
            tag.beilage1 = null
            tag.beilage2 = null
            tag.nachspeise = null

            // 🧠 Schlüssel für Speicherung
            val prefix = "speiseplan_${position}_"

            // 🔴 Aus SharedPreferences entfernen
            with(prefs.edit()) {
                remove("${prefix}vorspeise")
                remove("${prefix}hauptspeise")
                remove("${prefix}beilage1")
                remove("${prefix}beilage2")
                remove("${prefix}nachspeise")
                apply()
            }

            notifyItemChanged(position)
        }

    }
}
