package com.walterhblack.dovizwidget.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

fun Modifier.semanticsFavorite(code: String, selected: Boolean) = semantics {
    contentDescription = if (selected) "$code favorilerden çıkar" else "$code favorilere ekle"
}
