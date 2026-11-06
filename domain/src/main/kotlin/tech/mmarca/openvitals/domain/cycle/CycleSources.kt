package tech.mmarca.openvitals.domain.cycle

/** A citation: the text of a card is a string resource keyed by the card id. */
data class CycleSource(val label: String, val url: String)

/** The reviewed sources the cycle cards cite. Nothing here is written from memory. */
object CycleSources {
    val NHS_PERIODS = CycleSource("NHS, Periods", "https://www.nhs.uk/conditions/periods/")
    val NHS_PERIOD_PAIN = CycleSource("NHS, Period pain", "https://www.nhs.uk/symptoms/period-pain/")
    val NHS_PMS = CycleSource("NHS, Premenstrual syndrome", "https://www.nhs.uk/conditions/pre-menstrual-syndrome/")
    val NHS_IRREGULAR = CycleSource("NHS, Irregular periods", "https://www.nhs.uk/conditions/irregular-periods/")
    val GRIEGER = CycleSource(
        "Grieger & Norman, J Med Internet Res, 2020",
        "https://pmc.ncbi.nlm.nih.gov/articles/PMC7381001/",
    )
    val LI_APPLE = CycleSource("Li et al., npj Digital Medicine, 2023", "https://pmc.ncbi.nlm.nih.gov/articles/PMC10226714/")
    val BULL = CycleSource("Bull et al., npj Digital Medicine, 2019", "https://pmc.ncbi.nlm.nih.gov/articles/PMC6710244/")
    val WHO_MENSTRUAL_HEALTH = CycleSource(
        "World Health Organization, 2022",
        "https://www.who.int/news/item/22-06-2022-who-statement-on-menstrual-health-and-rights",
    )
    val MIHM = CycleSource(
        "Mihm et al., Animal Reproduction Science, 2011",
        "https://doi.org/10.1016/j.anireprosci.2010.08.030",
    )
    val FEHRING = CycleSource("Fehring et al., JOGNN, 2006", "https://doi.org/10.1111/j.1552-6909.2006.00051.x")
    val ACOG_PMS = CycleSource(
        "ACOG, Premenstrual syndrome",
        "https://www.acog.org/womens-health/faqs/premenstrual-syndrome-pms",
    )
    val ACOG_PREMENSTRUAL_GUIDELINE = CycleSource(
        "ACOG, Management of Premenstrual Disorders, 2023",
        "https://pubmed.ncbi.nlm.nih.gov/37973069/",
    )
    val HAS_ENDOMETRIOSIS = CycleSource(
        "HAS, Prise en charge de l'endométriose, 2017",
        "https://www.has-sante.fr/jcms/c_2819733/fr/prise-en-charge-de-l-endometriose",
    )
    val CNGOF_PAIN = CycleSource("CNGOF / Convergences PP, Douleurs pelviennes, 2025", "https://www.cngof.fr")
    val INSERM_PMDD = CycleSource(
        "Inserm, Syndrome prémenstruel et TDPM, 2023",
        "https://www.inserm.fr/c-est-quoi/payetoncycle-cest-quoi-le-syndrome-premenstruel/",
    )
    val MONASH_PCOS = CycleSource(
        "Monash University / ESHRE, Guideline for PCOS, 2023",
        "https://www.monash.edu/medicine/mchri/pcos/guideline",
    )
    val BMS_PERIMENOPAUSE = CycleSource(
        "British Menopause Society, Consensus Statement, 2023",
        "https://thebms.org.uk/publications/consensus-statements/",
    )
    val GUNGOR_THYROID = CycleSource(
        "Güngör Semiz & Hekimsoy, Cureus, 2024",
        "https://pmc.ncbi.nlm.nih.gov/articles/PMC11259460/",
    )

    /** Wearable skin temperature. Each DOI was resolved on Crossref; the numbers the card quotes were read in the full text. */
    val WANG_WRIST = CycleSource("Wang et al., Human Reproduction, 2025", "https://doi.org/10.1093/humrep/deaf005")
    val THIGPEN_RING = CycleSource("Thigpen et al., J Med Internet Res, 2025", "https://doi.org/10.2196/60667")
    val MAIJALA_RING = CycleSource("Maijala et al., BMC Women's Health, 2019", "https://doi.org/10.1186/s12905-019-0844-9")
    val SHILAIH_WRIST = CycleSource("Shilaih et al., Bioscience Reports, 2018", "https://doi.org/10.1042/bsr20171279")
    val ZHU_WRIST = CycleSource("Zhu et al., J Med Internet Res, 2021", "https://doi.org/10.2196/20710")

    /** Hormonal methods and temperature. Each DOI was resolved on Crossref; the numbers the card quotes were read in the abstract. */
    val DITTMAR_OC_WRIST = CycleSource("Dittmar et al., Chronobiology International, 2024", "https://doi.org/10.1080/07420528.2024.2342945")
    val SALKELD_OC_TEMPERATURE = CycleSource("Salkeld et al., Neuroimmunomodulation, 2001", "https://doi.org/10.1159/000059389")
    val CHARKOUDIAN_THERMOREGULATION = CycleSource("Charkoudian & Stachenfeld, Comprehensive Physiology, 2014", "https://doi.org/10.1002/cphy.c130029")
    val RIVERA_HORMONAL_MECHANISM = CycleSource("Rivera et al., Am J Obstet Gynecol, 1999", "https://doi.org/10.1016/s0002-9378(99)70120-1")
    val GOODALE_2019 = CycleSource(
        "Goodale et al., J Med Internet Res, 2019",
        "https://doi.org/10.2196/13404",
    )
    val JASINSKI_2024 = CycleSource(
        "Jasinski et al., npj Digital Medicine, 2024",
        "https://doi.org/10.1038/s41746-024-01394-0",
    )
    val GONZALEZ_2026 = CycleSource(
        "Gonzalez et al., npj Digital Medicine, 2026",
        "https://doi.org/10.1038/s41746-026-02799-9",
    )
    val LIN_MCPHASES_2026 = CycleSource(
        "Lin et al., Scientific Data, 2026",
        "https://doi.org/10.1038/s41597-026-06805-3",
    )
    val DE_JAGER_2026 = CycleSource(
        "de Jager et al., Sports Medicine, 2026",
        "https://doi.org/10.1007/s40279-025-02388-y",
    )
    val PIETILA_2018 = CycleSource(
        "Pietila et al., JMIR Mental Health, 2018",
        "https://doi.org/10.2196/mental.9519",
    )
    val SHI_2026 = CycleSource(
        "Shi et al., npj Digital Medicine, 2026",
        "https://doi.org/10.1038/s41746-025-02320-8",
    )
}

/** Scatters consecutive days across a list so the pick is stable per day but does not walk in order. */
internal fun stableIndexFor(date: java.time.LocalDate, size: Int): Int =
    Math.floorMod(date.toEpochDay() * 2_654_435_761L, size.toLong()).toInt()
