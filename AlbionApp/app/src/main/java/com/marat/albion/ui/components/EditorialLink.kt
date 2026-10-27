package com.marat.albion.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Ink

/** A content link with a generous touch area and a fine divider instead of a card. */
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
    Column(modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = Ink.Muted, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(16.dp))
            } else if (index != null) {
                Text(index, style = AlbionType.index.copy(color = Ink.Muted), modifier = Modifier.width(30.dp))
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                if (eyebrow != null) {
                    Eyebrow(eyebrow)
                    Spacer(Modifier.height(5.dp))
                }
                Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, lineHeight = 27.sp),
                    color = Ink.Fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(description, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                }
                if (progress != null && progress > 0f) {
                    Spacer(Modifier.height(10.dp))
                    ThinProgress(progress, height = 3.dp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Ink.Muted, modifier = Modifier.size(18.dp))
        }
        Hairline()
    }
}
