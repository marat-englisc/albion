package com.marat.albion.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marat.albion.data.db.CardEntity
import com.marat.albion.data.repo.WordCards
import com.marat.albion.ui.theme.AlbionType
import com.marat.albion.ui.theme.Ink

/** Each dictionary sense has its own numbered line and room for the text to wrap. */
@Composable
fun WordMeanings(
    card: CardEntity,
    showRussian: Boolean,
    modifier: Modifier = Modifier,
    showDefinitions: Boolean = false,
) {
    val senses = WordCards.members(card)
    Column(modifier.fillMaxWidth()) {
        senses.forEachIndexed { index, sense ->
            if (index > 0) {
                Spacer(Modifier.height(10.dp))
                Hairline(color = Ink.Gold.copy(alpha = .38f))
                Spacer(Modifier.height(10.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (senses.size > 1) {
                    Text("%02d".format(index + 1), modifier = Modifier.widthIn(min = 24.dp).alignByBaseline(),
                        style = AlbionType.index.copy(fontStyle = FontStyle.Normal, fontSize = 18.sp, lineHeight = 23.sp),
                        color = Ink.GoldText)
                }
                Column(Modifier.weight(1f).alignBy(FirstBaseline)) {
                    val meaning = if (showRussian) sense.meaningRu.ifBlank { sense.meaningEn } else sense.meaningEn
                    Text(meaning, style = MaterialTheme.typography.bodyMedium, color = Ink.Soft)
                    if (showDefinitions && sense.meaningEn.isNotBlank() && sense.meaningEn != meaning) {
                        Spacer(Modifier.height(4.dp))
                        Text(sense.meaningEn, style = MaterialTheme.typography.bodySmall, color = Ink.Muted)
                    }
                }
            }
        }
    }
}
