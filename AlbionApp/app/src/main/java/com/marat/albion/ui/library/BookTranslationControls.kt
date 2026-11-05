package com.marat.albion.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.library.BookTranslationPhase
import com.marat.albion.ui.components.FieldError
import com.marat.albion.ui.components.PrimaryButton
import com.marat.albion.ui.components.QuietLinearProgressIndicator

@Composable
fun BookTranslationControls() {
    val translator = LocalAppContainer.current.bookTranslator
    val status by translator.state.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Слова и выделенные фразы переводятся с английского на русский на устройстве. Языковая модель автоматически загружается один раз; затем перевод работает без интернета.",
            style = MaterialTheme.typography.bodyMedium)
        Text("Автоперевод: Google Translate · ML Kit", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (status.phase) {
            BookTranslationPhase.Preparing -> {
                Text(status.summary, style = MaterialTheme.typography.bodyMedium)
                QuietLinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
            }
            BookTranslationPhase.Ready -> Text("Перевод готов и доступен офлайн.", style = MaterialTheme.typography.bodyMedium)
            BookTranslationPhase.Failed -> {
                FieldError(status.message)
                PrimaryButton("Повторить загрузку", translator::preload)
            }
        }
    }
}
