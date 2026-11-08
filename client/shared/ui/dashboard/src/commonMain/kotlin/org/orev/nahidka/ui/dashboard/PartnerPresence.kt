package org.orev.nahidka.ui.dashboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PartnerPresence(
    val identifier: String,
    val name: String,
    val initials: String,
    val isOnline: Boolean,
)

/** Session-owned presence snapshot. Replace snapshots when partner presence changes. */
class PartnerPresenceStore {
    private val mutablePartners = MutableStateFlow<List<PartnerPresence>>(emptyList())
    val partners: StateFlow<List<PartnerPresence>> = mutablePartners.asStateFlow()

    fun replace(partners: List<PartnerPresence>) {
        require(partners.map { it.identifier }.distinct().size == partners.size)
        mutablePartners.value = partners.toList()
    }
}
