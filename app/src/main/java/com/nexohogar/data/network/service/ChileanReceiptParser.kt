package com.nexohogar.data.service

import com.nexohogar.domain.model.ScannedReceiptItem

data class ParsedReceipt(
    val store: String?,
    val date: String?,
    val items: List<ScannedReceiptItem>,
    val total: Double?
)

class ChileanReceiptParser {

    companion object {
        // Store detection patterns
        private val STORE_PATTERNS = mapOf(
            "LIDER" to listOf("LIDER", "LÍDER", "WALMART CHILE", "WALMART"),
            "JUMBO" to listOf("JUMBO", "CENCOSUD"),
            "SANTA ISABEL" to listOf("SANTA ISABEL"),
            "UNIMARC" to listOf("UNIMARC", "SMU S.A", "SMU "),
            "TOTTUS" to listOf("TOTTUS", "FALABELLA RETAIL"),
            "ACUENTA" to listOf("ACUENTA", "A CUENTA")
        )

        // Lines to skip (non-product lines)
        private val SKIP_KEYWORDS = listOf(
            "SUBTOTAL", "SUB TOTAL", "SUB-TOTAL",
            "TOTAL", "IVA", "NETO",
            "VUELTO", "CAMBIO",
            "EFECTIVO", "TARJETA", "DEBITO", "DÉBITO", "CREDITO", "CRÉDITO",
            "VISA", "MASTERCARD", "REDCOMPRA", "TRANSBANK",
            "RUT", "BOLETA", "FACTURA",
            "FECHA", "HORA", "CAJA", "SUC", "CAJERO", "CAJERA",
            "VENDEDOR", "TERMINAL", "AUTORIZA",
            "GRACIAS", "DEVOLUCION", "DEVOLUCIÓN",
            "DESCUENTO", "DESC.", "DCTO",
            "ELECTRONICA", "ELECTRÓNICA",
            "OPERACION", "OPERACIÓN",
            "S.A.", "S.A",
            "DIRECCIÓN", "DIRECCION", "FONO", "TEL",
            "CASA MATRIZ",
            "N° BOLETA", "NRO BOLETA", "NO BOLETA",
            "ITEMS VENDIDOS",
            // Receipt column headers
            "CANT ", "CODIGO", "PRECIO UNITARIO"
        )

        // ── Regex patterns ─────────────────────────────────────────────────

        private val DATE_PATTERN_SLASH = Regex("""(\d{1,2})/(\d{1,2})/(\d{2,4})""")
        private val DATE_PATTERN_DASH = Regex("""(\d{1,2})-(\d{1,2})-(\d{2,4})""")

        // Price at end of line: $1.290 or 1.290 or 1290
        private val PRICE_AT_END = Regex("""\$?\s*(-?\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?)\s*$""")

        // Standalone price line: "$ 920" or "$ 1.290" (with dollar sign)
        private val PRICE_ONLY_LINE = Regex("""^\s*\$\s*-?\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?\s*$""")

        // Number-only line that looks like a price: "920", "1.290", "12.990"
        private val NUMBER_PRICE_LINE = Regex("""^\s*\d{1,3}(?:\.\d{3})*\s*$""")

        // Barcode: 7-13 digit number (whole line)
        private val BARCODE_ONLY = Regex("""^\s*\d{7,13}\s*$""")

        // Barcode at start of line (8-13 digits followed by whitespace)
        private val BARCODE_PREFIX = Regex("""^\s*\d{8,13}\s+""")

        // Separator line (dashes, equals, asterisks, etc.)
        private val SEPARATOR_LINE = Regex("""^[\s\-=_*#.]+$""")

        // QTY×UNIT_PRICE standalone: "2x460", "3x1.450", "6x810"
        private val QTY_X_PRICE_ONLY = Regex("""^\s*(\d+)\s*[xX]\s*(\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?)\s*$""")

        // QTY×UNIT_PRICE followed by text: "2x460 FIDEOS SECOS" or "3x1.450 ACE.VEGETAL $ 4.350"
        private val QTY_X_PRICE_PREFIX = Regex("""^\s*(\d+)\s*[xX]\s*(\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?)\s+(.+)""")

        // Quantity prefix: "2 UN" or "2 un x" at start
        private val QTY_PREFIX = Regex("""^\s*(\d+(?:[.,]\d+)?)\s*(?:UN|un|Un|X|x)?\s*[xX*]?\s*""")

        // Weight line: 0,350 KG x $3.990/KG
        private val WEIGHT_LINE = Regex("""(\d+[.,]\d+)\s*(?:KG|kg|Kg)\s*[xX*]\s*\$?\s*(\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?)\s*/\s*(?:KG|kg)""")

        // Quantity inline: PRODUCT 2x$1.290 or PRODUCT 2 x $1.290
        private val QTY_INLINE = Regex("""(\d+(?:[.,]\d+)?)\s*[xX*]\s*\$?\s*(\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?)""")

        // Total line pattern
        private val TOTAL_PATTERN = Regex("""^\s*TOTAL\s+\$?\s*(\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?)\s*$""", RegexOption.IGNORE_CASE)
    }

    fun parse(ocrText: String): ParsedReceipt {
        val rawLines = ocrText.lines().map { it.trim() }.filter { it.isNotBlank() }

        val store = detectStore(rawLines)
        val date = detectDate(rawLines)
        val total = detectTotal(rawLines)

        // Phase 1: Reconstruct fragmented OCR lines into complete product lines
        val mergedLines = reconstructLines(rawLines)
        // Phase 2: Parse each reconstructed line into items
        val items = parseItems(mergedLines)

        return ParsedReceipt(
            store = store,
            date = date,
            items = items,
            total = total
        )
    }

    // ── Phase 1: Line reconstruction ───────────────────────────────────────

    /**
     * Pre-process OCR output to merge fragmented text into complete product lines.
     *
     * ML Kit OCR often splits columnar receipts into separate text blocks:
     *   Block 1: "400006837684"       (barcode)
     *   Block 2: "2x460"             (qty × unit price)
     *   Block 3: "FIDEOS SECOS"      (product name)
     *   Block 4: "$ 920"             (total price)
     *
     * This function reassembles them into: "2x460 FIDEOS SECOS $ 920"
     */
    private fun reconstructLines(rawLines: List<String>): List<String> {
        val result = mutableListOf<String>()
        var i = 0

        while (i < rawLines.size) {
            val line = rawLines[i].trim()

            // Skip blanks, separators, barcode-only lines
            if (line.isBlank() || SEPARATOR_LINE.matches(line) || BARCODE_ONLY.matches(line)) {
                i++
                continue
            }

            // ── Price-only fragment → attach to previous incomplete line ──
            // Must check BEFORE shouldSkipLine since prices match the "all digits/symbols" filter
            if (isPriceLine(line)) {
                if (result.isNotEmpty() && !PRICE_AT_END.containsMatchIn(result.last())) {
                    result[result.lastIndex] = "${result.last()} $line"
                }
                i++
                continue
            }

            // Skip known non-product lines (TOTAL, IVA, BOLETA, etc.)
            if (shouldSkipLine(line)) {
                i++
                continue
            }

            // ── Standalone QTY×PRICE (e.g., "2x460") → look ahead for name + total ──
            if (QTY_X_PRICE_ONLY.matches(line)) {
                val combined = lookAheadFromQtyPrice(rawLines, i)
                result.add(combined.first)
                i = combined.second
                continue
            }

            // ── Complete line (has price at end) → strip barcode prefix and add ──
            if (PRICE_AT_END.containsMatchIn(line)) {
                result.add(stripBarcode(line))
                i++
                continue
            }

            // ── Text without price → look ahead for a price-only fragment ──
            val combined = lookAheadForPrice(rawLines, i)
            result.add(stripBarcode(combined.first))
            i = combined.second
        }

        return result
    }

    /**
     * From a standalone QTY×PRICE line (e.g., "2x460"), look ahead to find
     * the product name and total price on subsequent lines.
     *
     * Returns (combinedLine, nextIndex).
     */
    private fun lookAheadFromQtyPrice(lines: List<String>, startIdx: Int): Pair<String, Int> {
        val parts = mutableListOf(lines[startIdx].trim())
        var j = startIdx + 1
        var hasProductName = false

        while (j < lines.size && j <= startIdx + 3) {
            val next = lines[j].trim()
            if (next.isBlank() || SEPARATOR_LINE.matches(next)) { j++; continue }
            if (BARCODE_ONLY.matches(next)) { j++; continue }

            // Price-only fragment → include it, signals end of this item
            if (isPriceLine(next)) {
                parts.add(next)
                j++
                break
            }

            if (shouldSkipLine(next)) break

            // Complete line with text + price → include (has name and total together)
            if (PRICE_AT_END.containsMatchIn(next)) {
                parts.add(next)
                j++
                break
            }

            // Text without price → likely the product name (only take one)
            if (!hasProductName) {
                parts.add(next)
                hasProductName = true
                j++
                continue
            }

            // Already have a product name; this is probably a different item
            break
        }

        return Pair(parts.joinToString(" "), j)
    }

    /**
     * From a text line without a price, look ahead for a price-only fragment
     * (e.g., "$ 920" or "920") on the next line.
     *
     * Only combines with price-only fragments, NOT with complete product lines
     * (to avoid merging a store name with the next product line).
     *
     * Returns (combinedLine, nextIndex).
     */
    private fun lookAheadForPrice(lines: List<String>, startIdx: Int): Pair<String, Int> {
        val line = lines[startIdx].trim()
        var j = startIdx + 1

        while (j < lines.size && j <= startIdx + 2) {
            val next = lines[j].trim()
            if (next.isBlank()) { j++; continue }
            if (BARCODE_ONLY.matches(next)) { j++; continue }

            // Only combine with price-only fragments
            if (isPriceLine(next)) {
                return Pair("$line $next", j + 1)
            }

            // Anything else (product line, text, etc.) → stop looking
            break
        }

        // Couldn't find a price fragment, return the line as-is
        return Pair(line, startIdx + 1)
    }

    /**
     * Check if a line is just a price value (e.g., "$ 920", "$1.290", "920").
     */
    private fun isPriceLine(line: String): Boolean {
        val trimmed = line.trim()
        return PRICE_ONLY_LINE.matches(trimmed) || NUMBER_PRICE_LINE.matches(trimmed)
    }

    /**
     * Strip barcode number (8-13 digits) from the beginning of a line.
     */
    private fun stripBarcode(line: String): String {
        return BARCODE_PREFIX.replaceFirst(line, "").trim()
    }

    // ── Store / date / total detection ─────────────────────────────────────

    private fun detectStore(lines: List<String>): String? {
        val headerLines = lines.take(10).joinToString(" ").uppercase()
        for ((storeName, patterns) in STORE_PATTERNS) {
            for (pattern in patterns) {
                if (headerLines.contains(pattern)) {
                    return storeName
                }
            }
        }
        return null
    }

    private fun detectDate(lines: List<String>): String? {
        for (line in lines.take(15)) {
            val matchSlash = DATE_PATTERN_SLASH.find(line)
            if (matchSlash != null) {
                return formatDate(
                    matchSlash.groupValues[1],
                    matchSlash.groupValues[2],
                    matchSlash.groupValues[3]
                )
            }
            val matchDash = DATE_PATTERN_DASH.find(line)
            if (matchDash != null) {
                return formatDate(
                    matchDash.groupValues[1],
                    matchDash.groupValues[2],
                    matchDash.groupValues[3]
                )
            }
        }
        return null
    }

    private fun formatDate(day: String, month: String, year: String): String {
        val fullYear = if (year.length == 2) "20$year" else year
        val paddedDay = day.padStart(2, '0')
        val paddedMonth = month.padStart(2, '0')
        return "$fullYear-$paddedMonth-$paddedDay"
    }

    private fun detectTotal(lines: List<String>): Double? {
        for (line in lines) {
            val match = TOTAL_PATTERN.find(line)
            if (match != null) {
                return parseChileanPrice(match.groupValues[1])
            }
        }
        return null
    }

    // ── Phase 2: Item parsing ──────────────────────────────────────────────

    private fun parseItems(lines: List<String>): List<ScannedReceiptItem> {
        val items = mutableListOf<ScannedReceiptItem>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            // Skip non-product lines
            if (shouldSkipLine(line)) {
                i++
                continue
            }

            // ── Try weight item: 0,350 KG x $3.990/KG  $1.397 ──
            val weightMatch = WEIGHT_LINE.find(line)
            if (weightMatch != null) {
                val weight = parseChileanDecimal(weightMatch.groupValues[1])
                val pricePerKg = parseChileanPrice(weightMatch.groupValues[2])
                val priceAtEnd = PRICE_AT_END.find(line.substring(weightMatch.range.last + 1))
                val totalPrice = if (priceAtEnd != null) {
                    parseChileanPrice(priceAtEnd.groupValues[1])
                } else {
                    weight * pricePerKg
                }

                val productName = if (items.isEmpty() && i > 0 && !shouldSkipLine(lines[i - 1]) && PRICE_AT_END.find(lines[i - 1]) == null) {
                    cleanProductName(lines[i - 1])
                } else if (items.isNotEmpty()) {
                    val prevLine = lines.getOrNull(i - 1)
                    if (prevLine != null && PRICE_AT_END.find(prevLine) == null && !shouldSkipLine(prevLine)) {
                        cleanProductName(prevLine)
                    } else {
                        "Producto a granel"
                    }
                } else {
                    "Producto a granel"
                }

                items.add(
                    ScannedReceiptItem(
                        name = productName,
                        quantity = weight,
                        pricePerUnit = pricePerKg,
                        priceTotal = totalPrice,
                        unit = "kg"
                    )
                )
                i++
                continue
            }

            // ── Try QTY×UNIT_PRICE prefix: "2x460 FIDEOS SECOS $ 920" ──
            val qtyPricePrefixMatch = QTY_X_PRICE_PREFIX.find(line)
            if (qtyPricePrefixMatch != null) {
                val qty = parseChileanDecimal(qtyPricePrefixMatch.groupValues[1])
                val unitPrice = parseChileanPrice(qtyPricePrefixMatch.groupValues[2])
                val rest = qtyPricePrefixMatch.groupValues[3].trim()

                val priceAtEnd = PRICE_AT_END.find(rest)
                if (priceAtEnd != null) {
                    val totalPrice = parseChileanPrice(priceAtEnd.groupValues[1])
                    val rawName = rest.substring(0, priceAtEnd.range.first).trim()
                    val productName = cleanProductName(rawName)

                    if (productName.isNotBlank()) {
                        items.add(
                            ScannedReceiptItem(
                                name = productName,
                                quantity = qty,
                                pricePerUnit = unitPrice,
                                priceTotal = totalPrice
                            )
                        )
                        i++
                        continue
                    }
                } else {
                    // No price at end — rest is just the product name, calculate total
                    val productName = cleanProductName(rest)
                    if (productName.isNotBlank()) {
                        items.add(
                            ScannedReceiptItem(
                                name = productName,
                                quantity = qty,
                                pricePerUnit = unitPrice,
                                priceTotal = qty * unitPrice
                            )
                        )
                        i++
                        continue
                    }
                }
            }

            // ── Try line with price at end ──
            val priceMatch = PRICE_AT_END.find(line)
            if (priceMatch != null) {
                val price = parseChileanPrice(priceMatch.groupValues[1])
                val textBeforePrice = line.substring(0, priceMatch.range.first).trim()

                if (textBeforePrice.isEmpty()) {
                    i++
                    continue
                }

                // Check for quantity prefix: "2 UN YOGHURT..." or "2 x PRODUCT..."
                val qtyPrefixMatch = QTY_PREFIX.find(textBeforePrice)
                if (qtyPrefixMatch != null && qtyPrefixMatch.range.first == 0) {
                    val qty = parseChileanDecimal(qtyPrefixMatch.groupValues[1])
                    val productName = cleanProductName(
                        textBeforePrice.substring(qtyPrefixMatch.range.last + 1)
                    )
                    if (productName.isNotBlank() && qty > 0) {
                        items.add(
                            ScannedReceiptItem(
                                name = productName,
                                quantity = qty,
                                pricePerUnit = if (qty > 1) price / qty else price,
                                priceTotal = price
                            )
                        )
                        i++
                        continue
                    }
                }

                // Check for inline quantity: "PRODUCT 2x$1.290"
                val inlineQtyMatch = QTY_INLINE.find(textBeforePrice)
                if (inlineQtyMatch != null) {
                    val qty = parseChileanDecimal(inlineQtyMatch.groupValues[1])
                    val unitPrice = parseChileanPrice(inlineQtyMatch.groupValues[2])
                    val productName = cleanProductName(
                        textBeforePrice.substring(0, inlineQtyMatch.range.first)
                    )
                    if (productName.isNotBlank() && qty > 0) {
                        items.add(
                            ScannedReceiptItem(
                                name = productName,
                                quantity = qty,
                                pricePerUnit = unitPrice,
                                priceTotal = price
                            )
                        )
                        i++
                        continue
                    }
                }

                // Simple product line: "PRODUCT NAME   $1.290"
                val productName = cleanProductName(textBeforePrice)
                if (productName.isNotBlank()) {
                    items.add(
                        ScannedReceiptItem(
                            name = productName,
                            quantity = 1.0,
                            pricePerUnit = price,
                            priceTotal = price
                        )
                    )
                }

                i++
                continue
            }

            // Check if next line has quantity info (multi-line format)
            val nextLine = lines.getOrNull(i + 1)?.trim()
            if (nextLine != null) {
                val nextWeightMatch = WEIGHT_LINE.find(nextLine)
                if (nextWeightMatch != null) {
                    i++
                    continue
                }

                val qtyLinePattern = Regex("""^\s*(\d+(?:[.,]\d+)?)\s*(?:UN|un|Un)\s*[xX*]\s*\$?\s*(\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?)""")
                val qtyLineMatch = qtyLinePattern.find(nextLine)
                if (qtyLineMatch != null) {
                    val qty = parseChileanDecimal(qtyLineMatch.groupValues[1])
                    val unitPrice = parseChileanPrice(qtyLineMatch.groupValues[2])
                    val totalPriceMatch = PRICE_AT_END.find(nextLine)
                    val totalPrice = if (totalPriceMatch != null) {
                        parseChileanPrice(totalPriceMatch.groupValues[1])
                    } else {
                        qty * unitPrice
                    }

                    items.add(
                        ScannedReceiptItem(
                            name = cleanProductName(line),
                            quantity = qty,
                            pricePerUnit = unitPrice,
                            priceTotal = totalPrice
                        )
                    )
                    i += 2
                    continue
                }
            }

            i++
        }

        return items
    }

    private fun shouldSkipLine(line: String): Boolean {
        val upper = line.uppercase().trim()

        if (upper.length < 2) return true

        // Skip lines with only digits, dots, spaces, symbols (no letters)
        if (upper.matches(Regex("""^[\d\s.\-\$,/:%*=]+$"""))) return true

        for (keyword in SKIP_KEYWORDS) {
            if (upper.startsWith(keyword) || upper == keyword) return true
        }

        if (upper.matches(Regex(""".*RUT\s*:?\s*\d{1,2}\.\d{3}\.\d{3}-[\dkK].*"""))) return true

        for ((_, patterns) in STORE_PATTERNS) {
            for (pattern in patterns) {
                if (upper == pattern || upper.startsWith("$pattern ")) return true
            }
        }

        return false
    }

    /**
     * Parse Chilean price format: dots as thousands separators.
     * "1.290" -> 1290.0, "12.344" -> 12344.0, "990" -> 990.0
     */
    private fun parseChileanPrice(priceStr: String): Double {
        val cleaned = priceStr.replace("$", "").replace(" ", "").trim()

        if (cleaned.contains(",") && cleaned.contains(".")) {
            return cleaned.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        }

        val withoutDots = cleaned.replace(".", "")
        return withoutDots.toDoubleOrNull() ?: 0.0
    }

    /**
     * Parse Chilean decimal format: comma as decimal separator.
     * "0,350" -> 0.35, "2,5" -> 2.5
     */
    private fun parseChileanDecimal(decStr: String): Double {
        val cleaned = decStr.replace(" ", "").replace(",", ".")
        return cleaned.toDoubleOrNull() ?: 1.0
    }

    /**
     * Clean product name by removing barcodes, leading/trailing numbers, symbols, and whitespace.
     */
    private fun cleanProductName(name: String): String {
        var cleaned = name.trim()

        // Strip leading barcode (8-13 digits followed by whitespace)
        cleaned = cleaned.replace(Regex("""^\d{8,13}\s+"""), "")
        // Strip leading short numbers with separators (e.g., "123. " or "45) ")
        cleaned = cleaned.replace(Regex("""^\d{1,3}[\s.\-)\]]+"""), "")
        // Strip trailing price patterns
        cleaned = cleaned.replace(Regex("""\$\s*[\d.,]+\s*$"""), "")
        // Strip leading/trailing punctuation and whitespace
        cleaned = cleaned.replace(Regex("""^[\s\-*•.]+"""), "")
        cleaned = cleaned.replace(Regex("""[\s\-*•.]+$"""), "")

        return cleaned.trim()
    }
}
