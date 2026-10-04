package org.orev.nahidka.ui.common.dialog

data class DialogState<Draft, Rejection : Any>(
    val draft: Draft,
    val submittable: Boolean,
    val submitting: Boolean = false,
    val rejection: Rejection? = null,
)
