package com.example.smartshopping.einkaufsliste

import com.example.smartshopping.artikel.Artikel
import java.io.Serializable

data class Einkaufsliste(
    val titel: String,
    val artikel: MutableList<Artikel> = mutableListOf()
) : Serializable
