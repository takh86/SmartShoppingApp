package com.example.smartshopping.einkaufsliste

import com.example.smartshopping.artikel.Artikel
import com.example.smartshopping.speiseplan.SpeiseplanTag
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object EinkaufslistenGenerator {

    fun generiereAusSpeiseplan(speiseplan: List<SpeiseplanTag>): List<Einkaufsliste> {
        println("### Generierung gestartet ###")
        val heute = LocalDate.now()
        println("Heute ist: $heute")
        val verwendungen = mutableListOf<Triple<LocalDate, Artikel, String>>()

        for (tag in speiseplan) {
            val datum = berechneDatumFürWochentag(tag.name, heute)
            println("Wochentag '${tag.name}' → Datum $datum")
            val rezepte = listOfNotNull(tag.vorspeise, tag.hauptspeise, tag.beilage1, tag.beilage2, tag.nachspeise)
            for (rezept in rezepte) {
                println("  Rezept: ${rezept.name}")
                for (zutat in rezept.zutaten) {
                    println("    Zutat: ${zutat.name}, Menge=${zutat.menge}, Haltbarkeit=${zutat.haltbarkeit}")
                    verwendungen.add(Triple(datum, zutat, zutat.name))
                }
            }
        }

        val gruppiert = verwendungen.groupBy { it.third }
        val einkaufslistenMap = sortedMapOf<LocalDate, MutableList<Artikel>>()

        for ((artikelName, liste) in gruppiert) {
            println()
            println("### Bearbeite Artikel: $artikelName ###")
            val sortiert = liste.sortedBy { it.first }
            for ((verbrauchsdatum, artikel, _) in sortiert) {
                val haltbarkeitTage = artikel.haltbarkeit
                if (haltbarkeitTage <= 0) continue

                val spätesterKauftag = verbrauchsdatum
                val frühesterKauftag = maxOf(LocalDate.now(), spätesterKauftag.minusDays(haltbarkeitTage.toLong() - 1))

                val geeigneterKauftag = einkaufslistenMap.keys
                    .filter { tag ->
                        tag >= LocalDate.now() &&
                                tag in frühesterKauftag..spätesterKauftag &&
                                tag >= verbrauchsdatum.minusDays(1) &&
                                tag <= verbrauchsdatum &&
                                tag.plusDays(haltbarkeitTage.toLong()) > verbrauchsdatum
                    }
                    .maxOrNull() ?: verbrauchsdatum.minusDays(1)

                println("  Verwendungsdatum: $verbrauchsdatum")
                println("  Frühester Kauftag: $frühesterKauftag, Spätester: $spätesterKauftag")
                println("  Gewählter Einkaufstag: $geeigneterKauftag")

                val listeAmTag = einkaufslistenMap.getOrPut(geeigneterKauftag) {
                    println("  → Neue Einkaufsliste für $geeigneterKauftag erstellt.")
                    mutableListOf()
                }

                val einheit = parseMenge(artikel.menge).einheit
                val vorhanden = listeAmTag.find { it.name == artikel.name && parseMenge(it.menge).einheit == einheit }

                if (vorhanden != null) {
                    val neueMenge = parseMenge(vorhanden.menge).menge + parseMenge(artikel.menge).menge
                    val aktualisiert = vorhanden.copy(menge = "%.2f%s".format(neueMenge, einheit))
                    listeAmTag.remove(vorhanden)
                    listeAmTag.add(aktualisiert)
                    println("  → Menge zusammengeführt mit existierendem Artikel: ${aktualisiert.menge}")
                } else {
                    listeAmTag.add(artikel)
                    println("  → Artikel hinzugefügt: ${artikel.name}, Menge=${artikel.menge}")
                }
            }
        }

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val ergebnis = einkaufslistenMap.entries.map { (datum, artikelListe) ->
            val titel = "Einkaufsliste ${datum.format(formatter)}"
            println()
            println("==> Einkaufsliste erstellt: $titel mit ${artikelListe.size} Artikeln")
            Einkaufsliste(titel).apply { artikel.addAll(artikelListe) }
        }.sortedBy { it.titel }
            .filter { einkaufsliste ->
                val datum = LocalDate.parse(einkaufsliste.titel.removePrefix("Einkaufsliste "), formatter)
                datum >= heute
            }

        println()
        println("### Generierung abgeschlossen: ${ergebnis.size} Einkaufslisten ###")
        return ergebnis
    }

    private fun berechneDatumFürWochentag(wochentag: String, start: LocalDate): LocalDate {
        val wochentage = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
        val heuteIndex = start.dayOfWeek.value % 7
        val zielIndex = wochentage.indexOf(wochentag)
        val diff = (zielIndex - heuteIndex + 7) % 7
        return start.plusDays(diff.toLong())
    }

    private fun parseMenge(menge: String): MengeEinheit {
        val cleaned = menge.trim().lowercase()
        val regex = Regex("([0-9,.]+)\\s*(kg|g|l|ml|stk|stück|gramm|milliliter|pack|pck)?")
        val match = regex.find(cleaned) ?: return MengeEinheit(0.0, "")
        val value = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
        val einheit = match.groupValues[2].ifEmpty { "" }
        return MengeEinheit(value, einheit)
    }

    data class MengeEinheit(val menge: Double, val einheit: String)
}
