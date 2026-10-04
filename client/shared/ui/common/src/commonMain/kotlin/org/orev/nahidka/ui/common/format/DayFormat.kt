package org.orev.nahidka.ui.common.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.char

val DAY_FORMAT = LocalDate.Format {
    day()
    char('.')
    monthNumber()
    char('.')
    year()
}
