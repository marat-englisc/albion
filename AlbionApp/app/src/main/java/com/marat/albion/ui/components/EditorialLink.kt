package com.marat.albion.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import com.marat.albion.ui.theme.LocalFeedback

@Composable
fun EditorialLink(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    index: String? = null,
    eyebrow: String? = null,
    progress: Float? = null,
) {
    val feedback = LocalFeedback.current
    ListItem(
        modifier = modifier.clickable { feedback.selection(); onClick() },
        overlineContent = eyebrow?.let { { Text(it) } },
        headlineContent = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (description.isNotBlank() || (progress != null && progress > 0f)) {
            {
                Column {
                    if (description.isNotBlank()) Text(description)
                    if (progress != null && progress > 0f) ThinProgress(progress)
                }
            }
        } else null,
        leadingContent = when {
            icon != null -> { { Icon(icon, null) } }
            index != null -> { { Text(index) } }
            else -> null
        },
        trailingContent = { Icon(AlbionIcons.ArrowForward, null) },
    )
}
