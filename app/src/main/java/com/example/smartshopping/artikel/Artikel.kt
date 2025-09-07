package com.example.smartshopping.artikel

import java.io.Serializable

data class Artikel(
    val name: String,
    val kategorie: String,
    val kuehlung: Boolean,
    val haltbarkeit: Int,
    var menge: String,
    val rechnungsart: String,
    val bildPfad: String? = null
) : Serializable




