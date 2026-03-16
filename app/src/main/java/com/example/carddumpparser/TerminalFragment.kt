package com.example.carddumpparser

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.carddumpparser.databinding.FragmentTerminalBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TerminalFragment displays the parsing interface allowing the user to
 * paste raw card dumps, optionally enter a billing address and parse
 * those lines into structured card objects. It also enables the
 * generation of an HTML export containing the parsed data.
 */
class TerminalFragment : Fragment() {

    private var _binding: FragmentTerminalBinding? = null
    private val binding get() = _binding!!

    // Selected card type (VISA, MASTERCARD, UNIONPAY). Provided via arguments.
    private var cardType: String = "VISA"

    // RecyclerView adapter for the preview list.
    private lateinit var adapter: CardAdapter

    // Parsed card list. Stored here for export.
    private var parsedCards: List<Card> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            cardType = bundle.getString(ARG_CARD_TYPE, "VISA")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTerminalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set selected type badge.
        binding.selectedTypeBadge.text = cardType

        // Back button navigates back to the previous fragment.
        binding.backButton.setOnClickListener { requireActivity().onBackPressed() }

        // Set up RecyclerView.
        adapter = CardAdapter(emptyList())
        binding.previewList.layoutManager = LinearLayoutManager(context)
        binding.previewList.adapter = adapter

        // Parse button triggers the extraction of cards.
        binding.parseButton.setOnClickListener {
            performParse()
        }

        // Export button writes the HTML file if cards exist.
        binding.exportButton.setOnClickListener {
            exportToHtml()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Performs parsing of the raw dump input. Extracts card numbers,
     * expiry dates and CVVs using regular expressions. Generates
     * deterministic names for each card based on its index. The parsed
     * cards are displayed in the RecyclerView and stored for export.
     */
    private fun performParse() {
        val text = binding.dumpInput.text?.toString() ?: ""
        val lines = text.split("\n", "\r")
        val cards = mutableListOf<Card>()

        val cardRegex = Regex("\d{13,16}")
        val expiryRegex = Regex("(\d{1,2})[\\/|](\d{2,4})")
        val cvvWordBoundaryRegex = Regex("\\b\\d{3,4}\\b")
        val cvvFallbackRegex = Regex("\\d{3,4}")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val cardMatch = cardRegex.find(trimmed) ?: continue
            val cardNumber = cardMatch.value
            // Only consider the first occurrence of the card number. Index is safe due to regex.
            val afterCard = trimmed.substring(cardMatch.range.last + 1)
            val expiryMatch = expiryRegex.find(afterCard) ?: continue

            // Month and year normalization. Pad month to two digits and trim year to last two digits.
            val monthRaw = expiryMatch.groupValues[1]
            val yearRaw = expiryMatch.groupValues[2]
            val month = monthRaw.padStart(2, '0')
            val year = if (yearRaw.length == 4) yearRaw.substring(yearRaw.length - 2) else yearRaw
            val expiry = "$month/$year"

            val expiryEndIndex = expiryMatch.range.last + 1
            val afterExpiry = afterCard.substring(expiryEndIndex)
            val cvvMatch = cvvWordBoundaryRegex.find(afterExpiry)
            val cvv = cvvMatch?.value ?: (cvvFallbackRegex.find(afterExpiry)?.value ?: "???")

            cards.add(Card(name = "", cardNumber = cardNumber, expiry = expiry, cvv = cvv))
        }

        // Generate names based on index and assign them to the card list.
        parsedCards = cards.mapIndexed { index, card ->
            val name = generateNameFromIndex(index)
            card.copy(name = name)
        }

        // Update the adapter and UI state.
        adapter.submitList(parsedCards)
        updateCounterAndVisibility()
    }

    /**
     * Updates the card counter text, enables/disables the export button and
     * toggles the empty view based on the size of [parsedCards].
     */
    private fun updateCounterAndVisibility() {
        val count = parsedCards.size
        val suffix = if (count == 1) "" else "s"
        binding.cardCounter.text = getString(R.string.cards_count_format, count, suffix)
        binding.exportButton.isEnabled = count > 0
        binding.noDataView.visibility = if (count == 0) View.VISIBLE else View.GONE
    }

    /**
     * Generates a pseudo‑random but deterministic card holder name given a
     * zero‑based index. The algorithm cycles through the lists of first
     * and last names and adds a numeric suffix only when the index
     * exceeds the total number of unique combinations. This replicates
     * the behaviour found in the original HTML implementation.
     */
    private fun generateNameFromIndex(idx: Int): String {
        val firstNames = FIRST_NAMES
        val lastNames = LAST_NAMES
        val namePoolSize = firstNames.size * lastNames.size
        return if (idx >= namePoolSize) {
            val baseFirst = firstNames[idx % firstNames.size]
            val baseLast = lastNames[(idx / firstNames.size) % lastNames.size]
            val suffix = (idx / namePoolSize) + 1
            "$baseFirst $baseLast $suffix"
        } else {
            val firstIdx = idx % firstNames.size
            val lastIdx = (idx / firstNames.size) % lastNames.size
            "${firstNames[firstIdx]} ${lastNames[lastIdx]}"
        }
    }

    /**
     * Escapes single quotes and double quotes for inclusion inside the
     * onClick handlers of the export HTML. In Kotlin we will not use
     * inline onClick attributes but this helper is kept for completeness
     * and potential future extensions.
     */
    private fun escapeForJs(str: String): String {
        return str.replace("'", "\\'").replace("\"", "&quot;")
    }

    /**
     * Exports the parsed cards and optional address to an HTML file. The
     * file is written to the app's private external files directory,
     * specifically under Environment.DIRECTORY_DOCUMENTS when available.
     * A toast displays the absolute path on success.
     */
    private fun exportToHtml() {
        if (parsedCards.isEmpty()) return
        val context = requireContext()
        // Extract address values
        val valCountry = binding.inpCountry.text?.toString()?.trim().orEmpty()
        val valAddress = binding.inpAddress.text?.toString()?.trim().orEmpty()
        val valCity = binding.inpCity.text?.toString()?.trim().orEmpty()
        val valState = binding.inpState.text?.toString()?.trim().orEmpty()
        val valZip = binding.inpZip.text?.toString()?.trim().orEmpty()

        // Build address section only when fields are non empty.
        val addressSection = StringBuilder()
        if (valCountry.isNotEmpty() || valAddress.isNotEmpty() || valCity.isNotEmpty() || valState.isNotEmpty() || valZip.isNotEmpty()) {
            addressSection.append("<div class=\"address-banner\">\n")
            addressSection.append("  <div class=\"address-banner-title\">Billing Address</div>\n")
            addressSection.append("  <div class=\"address-banner-grid\">\n")
            fun appendItem(label: String, value: String) {
                addressSection.append("    <div class=\"address-item\">\n")
                addressSection.append("      <span class=\"address-label\">$label</span>\n")
                addressSection.append("      <span class=\"address-val\">$value</span>\n")
                addressSection.append("    </div>\n")
            }
            if (valCountry.isNotEmpty()) appendItem("Country", escapeHtml(valCountry))
            if (valAddress.isNotEmpty()) appendItem("Address Line 1", escapeHtml(valAddress))
            if (valCity.isNotEmpty()) appendItem("City", escapeHtml(valCity))
            if (valState.isNotEmpty()) appendItem("State / Province", escapeHtml(valState))
            if (valZip.isNotEmpty()) appendItem("ZIP Code", escapeHtml(valZip))
            addressSection.append("  </div>\n</div>\n")
        }

        // Build the card grid section.
        val cardsHtml = StringBuilder()
        parsedCards.forEachIndexed { index, card ->
            cardsHtml.append("<div class=\"card\">\n")
            cardsHtml.append("  <div class=\"index-badge\">${index + 1}</div>\n")
            cardsHtml.append("  <div class=\"top-right-icon\">\uD83D\uDCF6</div>\n") // Use the wifi emoji as a simple signal icon
            cardsHtml.append("  <div class=\"card-subtitle\">Virtual Card Data</div>\n")
            cardsHtml.append("  <div class=\"name-row\">${escapeHtml(card.name)}</div>\n")
            cardsHtml.append("  <div class=\"number-row\">${formatCardNumberForHtml(card.cardNumber)}</div>\n")
            cardsHtml.append("  <div class=\"bottom-row\">\n")
            cardsHtml.append("    <div class=\"info-group\">\n")
            cardsHtml.append("      <span class=\"info-label\">Expiry</span>\n")
            cardsHtml.append("      <span class=\"info-val\">${escapeHtml(card.expiry)}</span>\n")
            cardsHtml.append("    </div>\n")
            cardsHtml.append("    <div class=\"info-group\">\n")
            cardsHtml.append("      <span class=\"info-label\">CVV</span>\n")
            cardsHtml.append("      <span class=\"info-val\">${escapeHtml(card.cvv)}</span>\n")
            cardsHtml.append("    </div>\n")
            cardsHtml.append("  </div>\n")
            cardsHtml.append("</div>\n")
        }

        // Compose the full HTML document. The CSS here mirrors the
        // structure of the original export but is intentionally
        // simplified to reduce output size.
        val html = StringBuilder()
        html.append("<!DOCTYPE html>\n")
        html.append("<html lang=\"en\">\n<head>\n")
        html.append("  <meta charset=\"UTF-8\">\n")
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
        html.append("  <title>${escapeHtml(cardType)} Cards Data</title>\n")
        html.append("  <style>\n")
        html.append("    body{margin:0;padding:16px;background:#050505;color:#fff;font-family:Arial,Helvetica,sans-serif;}\n")
        html.append("    .header{text-align:center;font-weight:900;font-size:24px;margin-bottom:24px;border-bottom:2px solid #0f0;padding-bottom:8px;}\n")
        html.append("    .address-banner{background:#1e1e1e;border:1px solid #333;border-radius:8px;padding:12px;margin-bottom:16px;}\n")
        html.append("    .address-banner-title{font-weight:700;color:#0f0;margin-bottom:8px;}\n")
        html.append("    .address-banner-grid{display:flex;flex-wrap:wrap;gap:12px;}\n")
        html.append("    .address-item{flex:1 1 45%;background:#111;padding:8px;border-radius:4px;border:1px solid #222;}\n")
        html.append("    .address-label{font-size:12px;color:#888;display:block;}\n")
        html.append("    .address-val{font-size:14px;color:#eee;font-weight:700;word-break:break-word;}\n")
        html.append("    .grid{display:flex;flex-wrap:wrap;gap:16px;}\n")
        html.append("    .card{background:#1e1e1e;border:1px solid #333;border-radius:8px;padding:12px;position:relative;flex:1 1 45%;box-sizing:border-box;}\n")
        html.append("    .index-badge{position:absolute;top:-12px;left:-12px;background:#fff;color:#000;width:32px;height:32px;border-radius:16px;font-weight:bold;display:flex;align-items:center;justify-content:center;border:3px solid #000;}\n")
        html.append("    .top-right-icon{position:absolute;top:12px;right:12px;color:#cca300;}\n")
        html.append("    .card-subtitle{font-size:10px;color:#888;text-transform:uppercase;margin-bottom:8px;}\n")
        html.append("    .name-row,.number-row{margin-bottom:8px;font-weight:bold;}\n")
        html.append("    .number-row{font-family:monospace;font-size:16px;}\n")
        html.append("    .bottom-row{display:flex;justify-content:space-between;}\n")
        html.append("    .info-group{display:flex;flex-direction:column;}\n")
        html.append("    .info-label{font-size:10px;color:#777;text-transform:uppercase;}\n")
        html.append("    .info-val{font-size:14px;color:#eee;font-weight:bold;}\n")
        html.append("  </style>\n</head>\n<body>\n")
        html.append("  <div class=\"header\">${escapeHtml(cardType)}</div>\n")
        html.append("  <div class=\"container\">\n")
        html.append(addressSection)
        html.append("    <div class=\"grid\">\n")
        html.append(cardsHtml)
        html.append("    </div>\n")
        html.append("  </div>\n")
        html.append("</body>\n</html>")

        // Determine directory. Use external files dir for documents if available.
        val dir = context.getExternalFilesDir("documents") ?: context.filesDir
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val fileName = "cards_${cardType.lowercase(Locale.US)}_${dateFormat.format(Date())}.html"
        val file = File(dir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(html.toString().toByteArray(Charsets.UTF_8))
        }
        Toast.makeText(context, getString(R.string.export_success, file.absolutePath), Toast.LENGTH_LONG).show()
    }

    /**
     * Escapes special HTML characters to avoid breaking the generated HTML
     * document. Minimal escaping is applied since card numbers and
     * expiry/CVV values are strictly numeric or contain slashes.
     */
    private fun escapeHtml(value: String): String {
        return TextUtils.htmlEncode(value)
    }

    /**
     * Formats the card number for the export HTML. Groups digits into
     * four‑character segments separated by spaces, as in the on‑screen
     * preview.
     */
    private fun formatCardNumberForHtml(num: String): String {
        val sb = StringBuilder()
        num.forEachIndexed { idx, c ->
            sb.append(c)
            if ((idx + 1) % 4 == 0 && (idx + 1) < num.length) {
                sb.append(' ')
            }
        }
        return sb.toString()
    }

    companion object {
        private const val ARG_CARD_TYPE = "arg_card_type"

        /** Static arrays of first and last names used for deterministic
         * generation of card holder names. The lists are intentionally
         * limited in size to reduce memory consumption while still
         * providing a variety of names. */
        private val FIRST_NAMES = arrayOf(
            "James", "Olivia", "Daniel", "Sophia", "Michael", "Emma", "William", "Ava", "Alexander", "Mia",
            "Ethan", "Charlotte", "Benjamin", "Amelia", "Jacob", "Harper", "Noah", "Evelyn", "Logan", "Abigail",
            "Lucas", "Emily", "Matthew", "Elizabeth", "Jackson", "Sofia", "David", "Avery", "Oliver", "Ella",
            "Jayden", "Madison", "Elijah", "Scarlett", "Aiden", "Victoria", "Samuel", "Grace", "Joseph", "Chloe",
            "Henry", "Penelope", "John", "Riley", "Owen", "Zoey", "Carter", "Lillian", "Gabriel", "Addison",
            "Anthony", "Lucy", "Joshua", "Hannah", "Andrew", "Audrey", "Thomas", "Maya"
        )
        private val LAST_NAMES = arrayOf(
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
            "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin",
            "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson",
            "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
            "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts",
            "Gomez", "Phillips", "Evans", "Turner", "Diaz", "Parker", "Cruz", "Edwards"
        )

        /** Factory method to create an instance of TerminalFragment with the
         * selected card type passed as an argument. */
        fun newInstance(type: String): TerminalFragment {
            val fragment = TerminalFragment()
            val bundle = Bundle()
            bundle.putString(ARG_CARD_TYPE, type)
            fragment.arguments = bundle
            return fragment
        }
    }
}