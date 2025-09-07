package com.example.smartshopping.rezept

import com.example.smartshopping.artikel.Artikel
import java.io.Serializable

data class Rezept(
    val name: String,
    val beschreibung: String,
    val zutaten: List<Artikel>,
    val dauer: Int = 0,
    val personen: Int = 1,
    val kategorie: String = "Allgemein"
) : Serializable




