package com.marat.albion.ui.library

import com.marat.albion.ui.components.DestructiveConfirmationDialog
import com.marat.albion.ui.components.QuietLinearProgressIndicator as LinearProgressIndicator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marat.albion.LocalAppContainer
import com.marat.albion.data.translation.ModelState
import com.marat.albion.data.translation.QWEN_MODELS
import com.marat.albion.ui.components.*
import com.marat.albion.ui.settings.SwitchSetting
import kotlinx.coroutines.launch

/** Qwen management for conversation practice and auxiliary translations in exercises. */
@Composable
fun TranslationModelControls() {
    val container = LocalAppContainer.current
    val translator = container.translator
    val models = translator.models
    val status by models.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var deleting by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) container.appScope.launch { models.importFrom(uri) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Qwen3.5 ведёт разговорную практику и переводит реплики и примеры в упражнениях прямо на устройстве. После установки модели интернет не нужен.",
            style = MaterialTheme.typography.bodyMedium)
        if (status.phase == ModelState.Unavailable) {
            FieldError(status.message)
        } else {
            if (!status.busy) {
                val ids = QWEN_MODELS.map { it.id } + if ("custom" in status.installed) listOf("custom") else emptyList()
                val titles = QWEN_MODELS.map { it.title } + if ("custom" in status.installed) listOf("Свой файл Qwen3.5") else emptyList()
                OptionPicker("Модель Qwen3.5", titles, ids.indexOf(status.selected), { scope.launch { models.select(ids[it]) } })
                Text(if (status.selected == "2B") "Более точная модель. Рекомендуется от 6 ГБ оперативной памяти."
                    else if (status.selected == "0.8B") "Лёгкая модель для телефонов с небольшим запасом памяти. Качество перевода проще."
                    else "Импортированная GGUF-модель Qwen3.5. Файл скопирован в хранилище приложения.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FieldError(status.message.takeIf { status.phase != ModelState.Downloading })
            when {
                status.busy -> {
                    Text(status.summary, style = MaterialTheme.typography.bodyMedium)
                    if (status.phase == ModelState.Downloading) {
                        ThinProgress(if (status.total > 0) status.downloaded.toFloat() / status.total else 0f)
                        Text("${status.downloaded / 1_000_000} / ${status.total / 1_000_000} МБ", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
                    TextButton(onClick = { scope.launch { models.cancel() } }) { Text("Отменить") }
                }
                status.phase == ModelState.Ready -> {
                    Text("Модель установлена. Перевод и беседа работают офлайн.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { deleting = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Удалить выбранную модель") }
                    GhostButton("Импортировать GGUF", { picker.launch(arrayOf("*/*")) }, icon = AlbionIcons.Add)
                }
                else -> {
                    SwitchSetting("Загрузка по мобильной сети", status.allowMobile) { scope.launch { models.setAllowMobile(it) } }
                    if (!status.allowMobile) Text("Загрузка дождётся Wi-Fi или другой сети без тарификации.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryButton(if (status.phase == ModelState.Failed) "Повторить загрузку модели" else "Загрузить модель",
                        { scope.launch { models.downloadSelected() } })
                    GhostButton("Выбрать файл GGUF", { picker.launch(arrayOf("*/*")) }, icon = AlbionIcons.Add)
                }
            }
        }
    }
    if (deleting) DestructiveConfirmationDialog(
        title = "Удалить модель собеседника?",
        message = "Освободится место на устройстве. История бесед сохранится. Для ответов собеседника модель потребуется установить снова.",
        onConfirm = { deleting = false; translator.releaseMemory(); scope.launch { models.deleteSelected() } },
        onDismiss = { deleting = false },
    )
}
