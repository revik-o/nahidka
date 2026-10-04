package org.orev.nahidka.ui.financialmanagement.component

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format.char

internal val FINANCIAL_DAY_FORMAT = LocalDate.Format {
    day()
    char('.')
    monthNumber()
    char('.')
    year()
}

internal val FINANCIAL_MONTH_FORMAT = YearMonth.Format {
    monthNumber()
    char('.')
    year()
}
