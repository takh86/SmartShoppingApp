package com.example.smartshopping.vorlag

import com.example.smartshopping.artikel.Artikel
import java.io.Serializable

data class EinkaufslisteVorlage (
    val name: String,
    val artikel: MutableList<Artikel>
) : Serializable