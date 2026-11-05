package com.marat.albion.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.Springs
import com.marat.albion.ui.theme.quietFloatAsState

/** Decorative indicator; the enclosing control announces its expanded state. */
@Composable
fun DisclosureIndicator(expanded: Boolean, modifier: Modifier = Modifier) {
    val turn by quietFloatAsState(if (expanded) 180f else 0f, Springs.snappy(), label = "disclosure")
    Icon(AlbionIcons.ExpandMore, null, modifier.size(20.dp).graphicsLayer { rotationZ = turn },
        tint = MaterialTheme.colorScheme.onSurfaceVariant)
}
