Add-Type -AssemblyName System.Drawing
$taskDirectory = $PSScriptRoot

function Write-Preview {
    param([string]$Filename, [string]$Subtitle, [array]$Items)
    $columns = 4
    $thumbWidth = 250
    $thumbHeight = 557
    $gutter = 22
    $header = 104
    $cellHeight = $thumbHeight + 48
    $rows = [int][Math]::Ceiling($Items.Count / $columns)
    $canvas = [System.Drawing.Bitmap]::new($columns * ($thumbWidth + $gutter) + $gutter, $header + $rows * $cellHeight + $gutter)
    $graphics = [System.Drawing.Graphics]::FromImage($canvas)
    $graphics.Clear([System.Drawing.ColorTranslator]::FromHtml('#F4EBDD'))
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $ink = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#263F32'))
    $muted = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#696354'))
    $line = [System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml('#CDBB9A'), 1)
    $titleFont = [System.Drawing.Font]::new('Georgia', 25)
    $labelFont = [System.Drawing.Font]::new('Segoe UI', 11)
    try {
        $graphics.DrawString('ALBION / OLD MONEY', $titleFont, $ink, [single]$gutter, [single]18)
        $graphics.DrawString($Subtitle, $labelFont, $muted, [single]$gutter, [single]61)
        for ($index = 0; $index -lt $Items.Count; $index++) {
            $item = $Items[$index]
            $x = $gutter + ($index % $columns) * ($thumbWidth + $gutter)
            $y = $header + [int][Math]::Floor($index / $columns) * $cellHeight
            $source = [System.Drawing.Image]::FromFile((Join-Path $taskDirectory $item[0]))
            try {
                $scale = [Math]::Min($thumbWidth / $source.Width, $thumbHeight / $source.Height)
                $width = [int][Math]::Round($source.Width * $scale)
                $height = [int][Math]::Round($source.Height * $scale)
                $graphics.DrawImage($source, [System.Drawing.Rectangle]::new($x, $y, $width, $height))
                $graphics.DrawRectangle($line, $x, $y, $width, $height)
            } finally { $source.Dispose() }
            $graphics.DrawString($item[1], $labelFont, $ink, [single]$x, [single]($y + $thumbHeight + 10))
        }
        $canvas.Save((Join-Path $taskDirectory $Filename), [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $titleFont.Dispose(); $labelFont.Dispose(); $ink.Dispose(); $muted.Dispose()
        $line.Dispose(); $graphics.Dispose(); $canvas.Dispose()
    }
}

Write-Preview 'pages-preview.png' 'Страницы приложения · снимки из Android-эмулятора' @(
    ,@('screens/home-light.png', 'Сегодня · светлая тема')
    ,@('screens/home-dark.png', 'Сегодня · тёмная тема')
    ,@('screens/grammar-light.png', 'Учебник')
    ,@('screens/library-dark.png', 'Библиотека')
    ,@('screens/reader-light.png', 'Читалка')
    ,@('screens/study-light.png', 'Карточки')
    ,@('screens/progress-light.png', 'Прогресс')
    ,@('screens/settings-dark.png', 'Настройки')
)

Write-Preview 'practice-preview.png' 'Все восемь тренажёров · рабочие экраны' @(
    ,@('screens/TYPE_ANSWER-round.png', '01 · Ввод ответа')
    ,@('screens/IRREGULAR-round.png', '02 · Формы глагола')
    ,@('screens/GRAMMAR_CHOICE-round.png', '03 · Грамматика')
    ,@('screens/GRAMMAR_GAP-round.png', '04 · Пропуски')
    ,@('screens/SENTENCE-round.png', '05 · Сборка предложения')
    ,@('screens/TENSES-round.png', '06 · Времена')
    ,@('screens/PRONUNCIATION-round.png', '07 · Произношение')
    ,@('screens/SHADOWING-round.png', '08 · Шэдоуинг')
)

Write-Preview 'practice-setup-preview.png' 'Все восемь тренажёров · эмблемы в прежних слотах 64 × 64 dp' @(
    ,@('screens/TYPE_ANSWER-setup.png', '01 · Ввод ответа')
    ,@('screens/IRREGULAR-setup.png', '02 · Формы глагола')
    ,@('screens/GRAMMAR_CHOICE-setup.png', '03 · Грамматика')
    ,@('screens/GRAMMAR_GAP-setup.png', '04 · Пропуски')
    ,@('screens/SENTENCE-setup.png', '05 · Сборка предложения')
    ,@('screens/TENSES-setup.png', '06 · Времена')
    ,@('screens/PRONUNCIATION-setup.png', '07 · Произношение')
    ,@('screens/SHADOWING-setup.png', '08 · Шэдоуинг')
)
