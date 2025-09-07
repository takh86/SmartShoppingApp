package com.example.smartshopping.speiseplan

import com.example.smartshopping.rezept.Rezept
import java.io.Serializable

data class SpeiseplanTag(
    val name: String,
    var vorspeise: Rezept? = null,
    var hauptspeise: Rezept? = null,
    var beilage1: Rezept? = null,
    var beilage2: Rezept? = null,
    var nachspeise: Rezept? = null
) : Serializable
