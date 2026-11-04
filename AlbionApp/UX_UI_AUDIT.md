**UX/UI-аудит Albion для Android — 4 ноября 2026**

Первичный аудит выполнен по рабочей копии, git `8ff57ed`. Рассмотрены все разделы интерфейса и общие компоненты; запущены приложение и выборочные UI-тесты. На этапе аудита код приложения не менялся. Ниже сохранены исходные находки; статус последующих исправлений указан отдельно.

**Объём проверки.** Android-эмулятор Pixel 10 Pro XL, API 37.2; основное окно 448×997 dp, отдельные контейнеры 320/360/411 dp со шрифтом 100/130/150/200%, ручные проверки 320×740 dp / 200%, 740×320 dp / 100% и 1200×800 dp / 100%. Проверены светлая и тёмная темы, клавиатура, карточки, все восемь упражнений, результаты, учебник, библиотека, читалка, перевод/редактор слова, настройки, разговор и установка голосовой модели. Полный набор всех сочетаний экранов и размеров не прогонялся.

**Проверки.** Офлайн-сборки `assembleDebug` и `assembleDebugAndroidTest` завершились успешно. `EditorialDesignTest`: 1/1; `CompactUiTest`, `PracticeDesignTest`, `GameUiGalleryTest`: суммарно 8/8. Эти тесты проверяют отдельные сценарии; их успех не доказывает отсутствие визуальных ошибок. Вручную найдены ошибки, которые они не покрывают.

**Общая оценка.** Палитра, основные гарнитуры, скругления и большая часть компонентов образуют узнаваемую систему. Главные недостатки — устойчивость макета при крупном тексте и малой высоте, контраст вторичных надписей на градиенте и разрозненная семантика состояний. Полная смена визуального направления не требуется.

**Первая пачка исправлений — 4 ноября 2026.** Пункты **1–3 исправлены** в рабочей копии:

- Карточка и оценки перестраиваются в две колонки при малой высоте. В портрете действия имеют ограниченную высоту и отдельную прокрутку. При крупном шрифте убраны повторяющиеся пояснения оценок и длинная подсказка; инструкция доступна в справке. Лицевая подсказка отделена от прокручиваемого содержимого.
- Установка Qwen и Whisper в чате и Whisper в шэдоуинге использует общий `ScrollableSheet`: закреплённый заголовок с закрытием и прокручиваемое содержимое.
- Фильтры поиска сворачиваются в кнопку со счётчиком, открывающую общую панель. При очень малой высоте с клавиатурой поле ввода и результаты располагаются рядом; поле сохраняет фокус. Список получает оставшуюся высоту, а пустое состояние прокручивается.

**Проверка исправлений.** `AdaptiveWorkspaceUiTest`: 3/3 на 320×740 dp / 200% и 3/3 на 740×320 dp / 100%. Проверены видимость слова, доступность всех оценок, прокрутка панелей и закрытие касанием, видимый результат над настоящей клавиатурой и сохранение фильтра. Снимки после исправлений: [карточка, 200%](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes/portrait/adaptive-workspace/study-answer.png), [карточка в альбомном окне](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes/landscape/adaptive-workspace/study-answer.png), [поиск с клавиатурой, 200%](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes/portrait/adaptive-workspace/browse-keyboard.png), [поиск в альбомном окне](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes/landscape/adaptive-workspace/browse-keyboard.png). Это проверка указанных конфигураций, а не всех устройств.

**Регрессионные проверки первой пачки:** `WordCardDesignTest`, `FilterOptionsUiTest`, `EditorialDesignTest` — суммарно 4/4; `testDebugUnitTest` — 207/207; `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` — успешно. Сборка для эмулятора выполнена с `-Palbion.abis=x86_64`. Логи находятся в `build/ux-fixes/`.

**Вторая пачка исправлений — 4 ноября 2026.** Пункты **4–7 исправлены** в рабочей копии:

- Блок продолжения чтения измеряет подпись и слова названия. Если для текста рядом с обложкой мало места, миниатюра убирается. Полное название и автор переносятся без ограничения тремя строками. На декоративной миниатюре больше нет дублирующего мелкого названия.
- `ScreenHeader` использует текст штатного размера вместо `FitText`. По фактической ширине заголовка и действий выбирается одна строка или размещение кнопок ниже; длинному заголовку доступна полная ширина и перенос.
- `StatPair` задаёт одинаковую ширину и высоту двум показателям, если они помещаются. Минимальная ширина учитывает полное число, суффикс, самое длинное слово подписи и внутренние отступы. При нехватке места показатели располагаются вертикально. Общий компонент применён к статистике, точности упражнений и прогрессу колоды. Длинные подписи сокращены, состав общего числа ответов пояснён под группой.
- Главные показатели колоды используют те же правила перестроения и равные ограничения ширины. Проверены четырёхзначные значения и проценты без переноса цифр.

**Проверка второй пачки.** `AdaptiveReadingStatsUiTest`: 4/4 на 320×740 dp / 200% и 4/4 на 411×890 dp / 100%. Проверяется фактическая геометрия строк и наличие всех символов, отсутствие многоточия и разрывов внутри слов в подписях, сохранение системного кегля заголовка, кнопки чтения и реальные экраны библиотеки, статистики и колоды. Для границ строк допускается только округление до одного пикселя. Снимки: [библиотека, 200%](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-2/large-text/adaptive-reading-stats/library.png), [статистика, 200%](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-2/large-text/adaptive-reading-stats/stats.png), [колода, 200%](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-2/large-text/adaptive-reading-stats/deck.png), [четырёхзначные значения](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-2/large-text/adaptive-reading-stats/metrics-bottom.png), [библиотека, 100%](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-2/normal/adaptive-reading-stats/library.png).

**Регрессионные проверки второй пачки:** `FilterOptionsUiTest` и `EditorialDesignTest` — суммарно 3/3, включая переходы в светлой и тёмной темах; `testDebugUnitTest` — 207/207; `lintDebug` и обе debug-сборки — успешно. Проверка фильтра теперь прокручивает до выбранного значения после пересоздания экрана: наличие одной видимой подписи у края окна недостаточно для проверки сохранённого значения. Тест переходов прокручивает до нужной секции на коротком окне. Логи второй пачки находятся в `build/ux-fixes-2/`.

**Третья пачка исправлений — 4 ноября 2026.** Пункты **8–10 исправлены** в рабочей копии:

- Метки сценария и уровня в чате используют `FlowRow`: уровень занимает свою естественную ширину и не разрывается на символы. Статус загрузки или сжатия беседы выводится отдельной строкой с объявлением изменения для средств доступности.
- Поле ответа получило короткую подсказку «Ваш ответ…» и больше полезной ширины на узком экране благодаря меньшим отступам. Длинный черновик прокручивается внутри ограниченной по высоте области; минимальная высота сохраняет одну строку текущего системного шрифта. При открытой клавиатуре метаданные беседы сворачиваются, освобождая место истории.
- Подсказка и голосовой ввод доступны в меню действий сообщения. Быстрый микрофон, отправка, остановка генерации и завершение записи сохранены. Доступная подпись поля задана отдельно от визуального placeholder.
- `SectionHeader` и `ModuleHeading` используют общий `SectionHeading`: одинаковые отступы, кегль заголовка и счётчика, перенос счётчика по фактической ширине содержимого. Длинный заголовок получает полную оставшуюся ширину; короткий счётчик остаётся рядом, когда помещается, в том числе при масштабе 200%. Заголовки получили семантическую роль для навигации средствами доступности.

**Проверка третьей пачки.** `AdaptiveChatHeadingUiTest`: 4/4 на 320×740 dp / 200% в светлой теме и 4/4 на 411×890 dp / 100% в тёмной теме. Проверены целостность A2, отдельная строка статуса, перенос и отсутствие перекрытия заголовка и счётчика, сохранение штатного кегля, размеры пустого редактора, настоящий экран чата и редактирование с системной клавиатурой. Черновик из 951 символа сохраняется и передаётся в обработчик отправки целиком; история остаётся видимой над редактором. Проверены подсказка, голосовой ввод, остановка ответа и завершение записи без запуска модели или записи микрофона. Снимки: [чат, 200%](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-3/large-text/adaptive-chat-headings/actual-chat.png), [статус загрузки](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-3/large-text/adaptive-chat-headings/context-loading.png), [черновик с клавиатурой](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-3/large-text/adaptive-chat-headings/composer-keyboard-long-draft.png), [заголовки модулей](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-3/large-text/adaptive-chat-headings/module-headings.png), [чат в тёмной теме](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-3/normal-dark/adaptive-chat-headings/actual-chat.png).

**Регрессионные проверки третьей пачки:** `FilterOptionsUiTest` и `EditorialDesignTest` — суммарно 3/3; `testDebugUnitTest` — 207/207; `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` — успешно. Логи и снимки находятся в `build/ux-fixes-3/`. Сборки выполнены для x86_64-эмулятора. Проверка клавиатуры проведена в указанных портретных конфигурациях.

**Четвёртая пачка исправлений — 4 ноября 2026.** Пункты **11–13 исправлены** в рабочей копии:

- Информационный текст больше не использует `FitText`. Сегменты измеряют полные подписи штатным кеглем: если равные колонки тесны, варианты переходят в вертикальный список. Высота горизонтального варианта учитывает реальную высоту строки. Короткие числовые варианты сохраняются в одной строке, если помещаются.
- Уровни учебника получили горизонтальную прокрутку, естественную минимальную ширину плитки и сохранение видимости выбранного уровня. Код CEFR и счётчик не уменьшаются. Трёхзначный номер урока увеличивает метку; если номер и самое длинное слово названия не помещаются рядом, название располагается ниже на полной ширине. Геометрия линии маршрута учитывает размер метки.
- Заголовок учебника переиспользует общий `AdaptiveScreenTitle`: 28 sp и действия на отдельной строке при нехватке ширины. Подписи нижних вкладок используют обычный `Text`; существующий переход к меню при недостаточной ширине сохранён. `FitText` остался только у логотипа, с минимумом 14 sp и пересчётом при смене системного масштаба.
- `SignalLabel` использует Onest 12,5 sp / 16 sp с умеренной разрядкой вместо Unbounded 9,5 sp / 1,1 sp. Информационные счётчики и подписи загрузки приведены к той же гарнитуре. Unbounded сохранён для акцентных заголовков и логотипа.
- Добавлены встроенный Noto Sans и отдельная роль `IpaStyle`. Она применена к транскрипциям карточек, отдельных значений и форм глаголов, поиска, перевода, упражнений и итогового разбора произношения. Сеть при отображении не нужна. [Исходный шрифт](https://github.com/google/fonts/tree/main/ofl/notosans) и [лицензия SIL OFL 1.1](https://github.com/google/fonts/blob/main/ofl/notosans/OFL.txt); лицензия включена в assets приложения. SHA-256 файла: `bfb7bb691513f12e734dc346c03a03f784912432d7e3fa8e56efcf906fe86b3d`.

**Проверка четвёртой пачки.** `AdaptiveTypographyUiTest`: 5/5 на 320×740 dp / 200% в светлой теме и 5/5 на 411×890 dp / 100% в тёмной теме. Проверяются фактические границы всех строк и символов, отсутствие многоточий и разрывов слов в изменённых подписях, неизменный кегль, выбор темы и количества, прокрутка до C1, трёхзначный номер 109 и реальный экран учебника. Android TextRunShaper проверил 6 582 уникальные транскрипции из карточек и форм неправильных глаголов плюс образец диакритики: 72 132 глифа, один встроенный файл шрифта, ноль пропущенных глифов и ноль системных подстановок. Проверка конкретного выбора шрифта использует API 31+; текущий эмулятор — API 37.2.

Снимки: [сегменты, 200%](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-4/large-text/adaptive-typography/segments.png), [уровни и длинное название урока](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-4/large-text/adaptive-typography/grammar-atlas-start.png), [прокрутка до C1](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-4/large-text/adaptive-typography/grammar-atlas-end.png), [служебные подписи и IPA](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-4/large-text/adaptive-typography/labels-and-ipa.png), [реальный учебник](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-4/large-text/adaptive-typography/grammar-screen.png), [отчёт покрытия IPA](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-4/large-text/adaptive-typography/ipa-coverage.txt).

**Регрессионные проверки четвёртой пачки:** `AdaptiveChatHeadingUiTest`, `FilterOptionsUiTest`, проверка заголовка из `AdaptiveReadingStatsUiTest` и `EditorialDesignTest` — суммарно 8/8. Вместе с новыми сценариями выполнено 18 успешных проверок на эмуляторе. Сквозной сценарий проходит основные разделы в обеих темах. `testDebugUnitTest` — 207/207; `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` — успешно. Логи: `build/ux-fixes-4/`; сборки выполнены для x86_64. Дополнительные снимки: [сегменты в тёмной теме](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-4/normal-dark/adaptive-typography/segments.png), [учебник, 100%](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-4/normal-dark/adaptive-typography/grammar-screen.png), [транскрипции, 100%](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-4/normal-dark/adaptive-typography/labels-and-ipa.png).

**Граница четвёртой пачки.** На этом этапе при 320 dp / 200% в акцентной карточке «Следующий шаг» оставался разрыв длинного названия. Он исправлен в пятой пачке ниже. Это не означает устранения всех переносов во всех остальных компонентах.

**Пятая пачка исправлений — 4 ноября 2026.** Пункты **14–16 исправлены** в рабочей копии:

- `BoundVolume` использует более тёмный градиент, менее яркий блик и непрозрачный белый информационный текст. Приведены к этой роли подписи учебника, библиотеки, колоды, практики и разговоров. Убрана полупрозрачность невыбранных сегментов внутри акцентной карточки; их подложка затемняется. Сетка и прочие декоративные элементы сохраняют отдельные роли.
- `CorrectionCard` использует `onTertiaryContainer` на `tertiaryContainer` для исправленной фразы, заголовка и объяснения. Цвет сигнала не применяется произвольно к серому фону.
- Роль `controlBorder` связана с Material `outline`: `#697080` в светлой теме, `#8B92A3` в тёмной. `outlineVariant` сохранён для декоративных разделителей. Новая обводка 1 dp применена к поиску, выбору, вторичным кнопкам, кликабельным панелям, полю чата, полям ответа и доступным вариантам упражнений. Поиск, редактор чата и поле ответа получают акцентную обводку 2 dp при фокусе; раскрытый выбор также использует более толстую акцентную рамку. Стандартные `OutlinedTextField` наследуют обновлённый `outline`.
- В карточке «Следующий шаг» горизонтальный отступ выбирается по ширине самого длинного слова, в пределах 12–24 dp. Название сохраняет 22 sp; на 320 dp / 200% «Структура английского предложения» переносится целыми словами. Проверены обе кнопки: проверка знаний и чтение урока.

**Проверка пятой пачки.** `ContrastAndHeroUiTest`: 6/6 на 320×740 dp / 200% в светлой теме и 6/6 на 411×890 dp / 100% в тёмной теме. Восемь проверенных поверхностей включают фон, контейнеры разной яркости и поверхности выбора. Минимальный расчётный контраст функциональной обводки — 3,82:1 в светлой и 4,60:1 в тёмной теме; исправления — 12,04:1 и 12,78:1 соответственно. Консервативный расчёт акцентного фона с максимальным бликом и пересечением двух линий сетки даёт 5,41:1 для белого текста. Отдельный попиксельный проход по области содержимого отрисованной пустой карточки дал минимум 7,31:1 в светлом стенде и 7,38:1 в тёмном. Разница связана с размерами карточки и положением градиента; рецепт цветов в обеих темах одинаков. Проверка охватывает собственные цветовые роли и указанные компоненты; это не сертификат соответствия всего приложения WCAG.

Проверены фактические непрозрачные цвета подписей во всех пяти потребителях `BoundVolume`, перенос названия без уменьшения кегля и работа действий. У поиска и выбора обводки проверены по пикселям экрана, включая фокус и возврат после его снятия; ввод и очистка текста, выбор уровня, вторичная кнопка и строка выбора работают. Снимки: [следующий урок, 200%](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-5/large-text/contrast-and-hero/next-lesson.png), [исправление в чате](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-5/large-text/contrast-and-hero/correction.png), [поля без фокуса](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-5/large-text/contrast-and-hero/controls-idle.png), [поиск с фокусом](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-5/large-text/contrast-and-hero/controls-focused.png), [карточка продолжения чтения](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-5/large-text/contrast-and-hero/library-caption.png), [расчёт цветовых пар](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-5/large-text/contrast-and-hero/color-pairs.txt), [результат попиксельного прохода](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-5/large-text/contrast-and-hero/rendered-hero.txt).

Снимки в тёмной теме: [следующий урок](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-5/normal-dark/contrast-and-hero/next-lesson.png), [исправление](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-5/normal-dark/contrast-and-hero/correction.png), [контуры без фокуса](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-5/normal-dark/contrast-and-hero/controls-idle.png), [фокус с клавиатурой](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-5/normal-dark/contrast-and-hero/controls-focused.png), [расчёт цветовых пар](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-5/normal-dark/contrast-and-hero/color-pairs.txt).

Ориентиры проверки: [WCAG 2.2 — контраст текста](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html) и [значимые границы и состояния](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html). Порог 3:1 применялся к функциональным границам и признакам состояния; декоративные линии не усиливались до этого порога. Сглаженные края букв не использовались как исходный цвет текста.

**Регрессионные проверки пятой пачки:** `FilterOptionsUiTest` — 2/2; продолжение чтения из `AdaptiveReadingStatsUiTest` — 1/1; редактор чата из `AdaptiveChatHeadingUiTest` — 1/1; открытие настройки и раунда всех восьми упражнений из `PracticeDesignTest` — 1/1; переходы по основным разделам в обеих темах из `EditorialDesignTest` — 1/1. Суммарно с новыми сценариями — 18 успешных проверок на эмуляторе. `testDebugUnitTest` — 207/207; `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` — успешно. Логи и снимки: `build/ux-fixes-5/`. Сборки выполнены для x86_64. Проверялись указанные портретные конфигурации; полный набор размеров окон и все состояния всех контролов не проверялись.

**Шестая пачка исправлений — 4 ноября 2026.** Пункты **17–19 исправлены** в рабочей копии:

- Вместо безымянных ячеек тепловой карты показан календарь с числами и днями недели. Выбор даты выводит полную дату с годом и точное число ответов; выбранная дата сохраняется при обновлении счётчика. Положение дат рассчитано по тому же абсолютному дню, который использует статистика. Шкала фиксирована: `0`, `1–9`, `10–24`, `25–49`, `50+`. Сегодня отмечено точкой, выбор — отдельной рамкой. Числа используют контрастный чёрный или белый цвет по яркости заливки.
- Каждому дню дана область касания не меньше 48×48 dp, полная подпись даты и количества ответов, состояние выбора и положение в сетке для сервисов доступности. Заголовок сообщает изменение через polite live region. При нехватке ширины календарь прокручивается по горизонтали, не уменьшая кегль и области касания; выбранный столбец остаётся видимым.
- Общие функции движения учитывают настройку при изменении `Float`/`Dp`, размеров содержимого и положения элементов списков. Включение «Меньше движения» удаляет анимированное состояние и сразу показывает конечное значение. Подключены сообщения и автопрокрутка чата, списки библиотеки/практики/разговоров/поиска, стрелки раскрытия, индикаторы и собственные переходы учебных компонентов. У колец прогресса, появления блоков, покачивания ответа и прочих собственных эффектов учтено изменение настройки во время работы.
- Повторяющиеся декоративные эффекты, пульсации аудио и записи, точки набора и циклы заставки в этом режиме не создают бесконечные анимации. Волна записи заменяется статичной подписью. Неопределённое ожидание обозначается неподвижными песочными часами или полосатой дорожкой с семантикой загрузки. На выходе заставки остаётся короткое растворение без масштабирования. Обычный режим сохраняет анимации; прямое движение за пальцем сохраняется в обоих режимах.
- Убрана автоматическая демонстрация свайпа на основной карточке после первого раскрытия. Ответ остаётся неподвижным для чтения. Свайпы вправо/влево продолжают выставлять «Хорошо»/«Снова»; в режиме уменьшения движения возврат и завершение жеста происходят без пролёта и вращения карточки.

**Проверка шестой пачки.** `ActivityMotionUiTest`: 6/6 на 320×740 dp / 200% в светлой теме и 6/6 на 411×890 dp / 100% в тёмной теме. Проверены календарные даты на границе месяцев, точный счётчик и его обновление, фиксированная легенда, выбор через Android accessibility, размеры целей касания, состояние «Сегодня» и прокрутка. Реальная сохранённая настройка включается посреди незавершённого перехода: он останавливается, следующие значения появляются сразу. Геометрия растущего сообщения, переставленного списка и раскрытого блока сравнивается после первых кадров и после ожидания; неподвижность индикаторов проверяется попиксельно. В обычном режиме сообщение продолжает анимировать рост. После раскрытия карточка отслеживается две секунды без самостоятельного движения; обе оценки проверяются реальными жестами, включая режим уменьшения движения.

Снимки: [выбор даты и обновлённый счётчик, 200%](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-6/large-text/activity-and-motion/calendar-selected.png), [прокрутка календаря](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-6/large-text/activity-and-motion/calendar-scrolled.png), [раскрытый блок и статичные индикаторы](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-6/large-text/activity-and-motion/static-components.png), [календарь в тёмной теме](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-6/normal-dark/activity-and-motion/calendar-selected.png), [сообщение и порядок списка](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-6/normal-dark/activity-and-motion/streaming-and-list.png). Логи новых сценариев: [200%](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-6/large-text-tests.txt), [100%, тёмная тема](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-6/normal-dark-tests.txt).

**Регрессионные проверки шестой пачки:** редактор чата из `AdaptiveChatHeadingUiTest` и все оценки карточки из `AdaptiveWorkspaceUiTest` — 2/2 на 320×740 dp / 200%; `FilterOptionsUiTest` — 2/2; открытие настройки и раунда всех восьми упражнений из `PracticeDesignTest` — 1/1; переходы по основным разделам в обеих темах из `EditorialDesignTest` — 1/1. С новыми сценариями — 18 успешных UI-проверок. `testDebugUnitTest` — 207/207; `assembleDebug` и `assembleDebugAndroidTest` — успешно. `lintDebug` завершился с нулём ошибок; в проекте остаются 38 предупреждений. [Финальная сборка и проверки](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-6/final-build-and-checks.txt), [проверка карточки и чата](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-6/large-text-regression.txt), [регрессионные сценарии](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-6/regression-tests.txt). Сборки выполнены для эмулятора x86_64 / API 37.2.

**Граница шестой пачки.** Проверены перечисленные собственные эффекты приложения и указанные сценарии; плавность по FPS/frame timing и внутренние эффекты системных/Material-компонентов отдельно не измерялись. Короткие растворения и смена цвета допустимы. Ожидание вступления заставки из пункта 20, порядок содержимого из пункта 21 и адаптация навигации из пункта 22 остаются следующей работой.

**Предлагаемая следующая пачка:** пункты **20–22** — убрать обязательное ожидание готового приложения на заставке, поднять основные действия и контент выше второстепенных блоков, адаптировать навигацию одновременно к ширине, высоте и крупному шрифту. В пункт 21 входит ранее отмеченный перенос названия и кнопки продолжения практики на 320 dp / 200%.

**Приоритеты.** P1 — мешает пройти сценарий в воспроизведённой конфигурации; P2 — заметно ухудшает чтение, управление или предсказуемость; P3 — согласованность и качество деталей. Рекомендации и риски отдельно помечены; они не выдаются за воспроизведённые ошибки.

1. **[P1] Карточка и часть оценок исчезают.** На 740×320 dp лицевая сторона сжата до полосы, а после раскрытия ответ полностью вытесняется подсказкой и оценками. На 320×740 dp при шрифте 200% видны только «Снова» и «Трудно»; «Хорошо» и «Легко» недоступны.

   **Решение:** При малой высоте использовать две колонки: карточка слева, оценки справа. В портрете переносить длинную подсказку в справку и делать область действий компактной или прокручиваемой. Содержимому гарантировать минимально полезную высоту.

   **Основание:** Подтверждено. [study/StudyScreen.kt:149](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/study/StudyScreen.kt:149); [study/StudyScreen.kt:173](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/study/StudyScreen.kt:173); [study/StudyScreen.kt:321](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/study/StudyScreen.kt:321); [components/CoachMark.kt:66](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/CoachMark.kt:66). Снимки: [answer-landscape.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/answer-landscape.png), [answer-320-font200.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/answer-320-font200.png), [study-landscape.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/study-landscape.png).

2. **[P1] В установке моделей нельзя добраться до кнопки загрузки.** В панели «Голосовой ввод» на 320 dp / 200% описание и выбор модели занимают весь экран. Нижние действия выходят за экран; прокрутки нет. Такой же каркас используется для установки Qwen в чате и Whisper в шэдоуинге.

   **Решение:** Общий компонент панели: закреплённая шапка, прокручиваемое содержимое, доступная область основного действия. Применить его ко всем панелям моделей и учитывать клавиатуру.

   **Основание:** Подтверждено. [conversation/ChatScreen.kt:165](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/conversation/ChatScreen.kt:165); [games/ShadowingGame.kt:326](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/games/ShadowingGame.kt:326). Снимки: [speech-sheet-320-font200.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/speech-sheet-320-font200.png).

3. **[P1] Фильтры вытесняют результаты поиска.** В «Все карточки и поиск» на 320 dp / 200% с открытой клавиатурой видны поиск, два больших фильтра и число карточек. Сам список не виден; пользователь не может одновременно вводить запрос и оценивать результаты.

   **Решение:** При вводе сворачивать фильтры в одну кнопку «Фильтры · N». Верхнюю область включить в общую прокрутку или уменьшать по доступной высоте; оставлять место минимум для нескольких результатов.

   **Основание:** Подтверждено. [browse/BrowseScreens.kt:135](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/browse/BrowseScreens.kt:135); [browse/BrowseScreens.kt:165](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/browse/BrowseScreens.kt:165). Снимки: [browse-320-font200-keyboard.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/browse-320-font200-keyboard.png).

4. **[P2] Блок продолжения чтения не перестраивается.** На 320 dp / 200% обложка и текст остаются в одной строке. «Продолжить» рвётся перед последней буквой, название книги обрезается, на миниатюре текст пересекается с декоративной сферой.

   **Решение:** По измеренной ширине текста и масштабу переходить к вертикальной композиции. Обложку уменьшать или убирать; полное название показывать вне обложки и разрешать перенос целыми словами.

   **Основание:** Подтверждено. [library/LibraryScreen.kt:239](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/library/LibraryScreen.kt:239); [library/LibraryScreen.kt:297](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/library/LibraryScreen.kt:297). Снимки: [library-320-font200.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/library-320-font200.png).

5. **[P2] Заголовки обрезаются без многоточия.** В библиотеке на 320 dp / 200% заголовок отображается как «Библиоте» с жёстким срезом. FitText достигает минимального размера, а затем использует Clip; кнопки справа продолжают занимать фиксированное место.

   **Решение:** Переносить действия на отдельную строку или в меню. Заголовку разрешить две строки. Для основных названий экранов не использовать уменьшение до предела с последующим Clip.

   **Основание:** Подтверждено. [components/Components.kt:108](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Components.kt:108); [components/Components.kt:465](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Components.kt:465). Снимки: [library-320-font200.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/library-320-font200.png).

6. **[P2] Статистика превращается в узкие столбцы текста.** Две колонки показателей сохраняются на 320 dp / 200%. Подписи разрываются внутри слов: «вспомн / или», «карточк / и», «практик / а». Карточки приобретают разную высоту и плохо сканируются.

   **Решение:** Переходить к одной колонке по фактической минимальной ширине карточки с учётом шрифта. Сократить подписи, а пояснения вынести ниже группы.

   **Основание:** Подтверждено. [stats/StatsScreen.kt:113](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/stats/StatsScreen.kt:113); [components/Editorial.kt:29](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Editorial.kt:29). Снимки: [stats-320-font200-lower.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/stats-320-font200-lower.png).

7. **[P2] Показатели колоды не помещаются рядом.** На 320 dp / 200% правая колонка «новых сегодня» сужается до нескольких букв, число 20 подрезается справа. У колонок нет равных ограничений ширины и режима перестроения.

   **Решение:** Применить адаптивную пару показателей: равные колонки при достаточной ширине, вертикальный список при крупном тексте. Предусмотреть трёх- и четырёхзначные числа.

   **Основание:** Подтверждено. [deck/DeckScreen.kt:102](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/deck/DeckScreen.kt:102). Снимки: [deck-320-font200.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/deck-320-font200.png).

8. **[P2] Метки в шапке чата ломаются по символам.** На 320 dp / 200% длинная метка сценария оставляет уровню A2 слишком мало места: A и 2 оказываются на разных строках. При загрузке рядом добавляется ещё и статус модели.

   **Решение:** Использовать FlowRow, уровню запретить перенос внутри значения. Статус загрузки вынести на отдельную строку, не конкурирующую с метками.

   **Основание:** Подтверждено. [conversation/ChatScreen.kt:87](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/conversation/ChatScreen.kt:87). Снимки: [chat-320-font200.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/chat-320-font200.png).

9. **[P2] Поле сообщения непропорционально разрастается.** На 320 dp / 200% пустое поле занимает примерно три строки из-за длинного placeholder; «по-английски» разрывается внутри слова. Вместе с боковыми кнопками оно сильно сокращает ленту.

   **Решение:** Сократить placeholder до «Ваш ответ…» и дать полю больше ширины. Вторичные действия объединить в меню. Ограничить высоту редактора с внутренней прокруткой введённого текста.

   **Основание:** Подтверждено. [conversation/ChatScreen.kt:309](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/conversation/ChatScreen.kt:309). Снимки: [chat-320-font200.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/chat-320-font200.png).

10. **[P2] Заголовки секций адаптируются неодинаково.** SectionHeader переносит счётчик под заголовок при крупном шрифте, ModuleHeading оставляет номер, заголовок и trailing в одной строке. На главной и в практике название секции получает существенно меньше места.

   **Решение:** Объединить правила в одном компоненте заголовка: счётчик переносить вниз по измерению содержимого, а не только по одному порогу fontScale.

   **Основание:** Подтверждено кодом. [components/Spatial.kt:59](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Spatial.kt:59); [components/Components.kt:87](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Components.kt:87).

11. **[P2] Автоуменьшение текста частично отменяет системный масштаб.** FitText используется для сегментов, заголовков и индексов. Минимумы доходят до 7 sp в учебнике и 9 sp в общем компоненте; после достижения минимума текст всё равно может быть срезан.

   **Решение:** Сначала перестраивать макет: перенос, вертикальный выбор, горизонтальная прокрутка уровней. Автоуменьшение оставить для декоративных чисел и логотипа, определить разумный минимум для служебного текста.

   **Основание:** Подтверждено кодом и снимками. [components/Components.kt:465](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Components.kt:465); [grammar/GrammarRoadmapUi.kt:72](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/grammar/GrammarRoadmapUi.kt:72). Снимки: [grammar-320-font200.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/grammar-320-font200.png).

12. **[P3] Служебный шрифт слишком мелкий и широкий.** SignalLabel набран Unbounded 9,5 sp с разрядкой 1,1 sp. Это увеличивает занимаемую ширину и снижает удобство чтения коротких пояснений и индексов. Сам по себе малый кегль не означает нарушение WCAG.

   **Решение:** Для информационных подписей использовать Onest 12–13 sp с умеренной разрядкой. Unbounded оставить для крупных заголовков, коротких индексов и декоративных элементов.

   **Основание:** Экспертная рекомендация. [components/Spatial.kt:35](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Spatial.kt:35); [theme/Type.kt:37](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/theme/Type.kt:37).

13. **[P2] В транскрипциях смешиваются шрифты.** Проверка cmap встроенного Onest выявила отсутствие ряда частых IPA-символов, в том числе ɪ, ʊ, ɑ, θ, ʃ, ɜ и знаков ударения/долготы. Android подставляет их из резервного шрифта; единая гарнитура транскрипции не обеспечена.

   **Решение:** Задать отдельную роль IpaStyle с гарнитурой, полностью покрывающей используемый IPA, например подходящим Noto Sans. Проверить базовую линию и метрики на реальном наборе транскрипций.

   **Основание:** Подтверждено содержимым шрифта. [theme/Type.kt:30](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/theme/Type.kt:30); [study/CardFaces.kt:72](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/study/CardFaces.kt:72); [games/VoiceGames.kt:111](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/games/VoiceGames.kt:111).

14. **[P2] Вторичный текст на градиенте имеет недостаточный контраст.** Для белого с alpha 0,7 на #6E52FF расчёт даёт 3,20:1. По фону текущего снимка учебника рядом с подписями получается примерно 3,37–3,68:1. Для мелких подписей это ниже ориентира 4,5:1.

   **Решение:** На акцентных панелях использовать непрозрачный белый текст. Затемнить светлый участок градиента и проверить все сочетания с бликом; различать иерархию кеглем и начертанием.

   **Основание:** Подтверждено. [components/Cards.kt:53](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Cards.kt:53); [grammar/GrammarRoadmapUi.kt:91](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/grammar/GrammarRoadmapUi.kt:91); [games/GamesScreen.kt:82](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/games/GamesScreen.kt:82); [library/LibraryScreen.kt:247](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/library/LibraryScreen.kt:247). Снимки: [editorial/grammar-monochrome-light.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/editorial/grammar-monochrome-light.png).

15. **[P2] Зелёный текст исправлений слегка не дотягивает до нужного контраста.** В светлом чате CorrectionCard использует #0F7A55 на #E8EAEF: 4,43:1. Это ниже 4,5:1 для текста bodyMedium. Контраст близок к границе, но не имеет запаса.

   **Решение:** Использовать подходящий onTertiaryContainer на tertiaryContainer либо отдельный более тёмный successText. Проверять пары ролей, а не один success-цвет на любых поверхностях.

   **Основание:** Подтверждено расчётом. [conversation/ChatScreen.kt:238](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/conversation/ChatScreen.kt:238); [theme/Color.kt:21](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/theme/Color.kt:21); [theme/Theme.kt:37](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/theme/Theme.kt:37).

16. **[P2] Границы неактивных полей почти сливаются с поверхностью.** outlineVariant даёт около 1,30:1 на белом и 1,25:1 на тёмной панели. Эти границы используются не только декоративно, но и для полей поиска, выбора и вторичных кнопок.

   **Решение:** Разделить декоративный divider и функциональный controlBorder. Для важных границ и состояний дать существенно больший контраст, ориентироваться на 3:1 там, где граница необходима для распознавания элемента.

   **Основание:** Подтверждено расчётом. [theme/Theme.kt:40](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/theme/Theme.kt:40); [theme/Theme.kt:66](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/theme/Theme.kt:66); [browse/BrowseScreens.kt:186](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/browse/BrowseScreens.kt:186); [components/OptionPicker.kt:45](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/OptionPicker.kt:45).

17. **[P2] Календарь активности не позволяет прочитать точные данные.** Тепловая карта показывает интенсивность цветом, без дат в ячейках, числовой легенды и выбора дня. По ней нельзя узнать дату и количество ответов конкретного дня. У ячеек также нет текстового представления данных для доступности.

   **Решение:** Добавить выбор дня с подписью «4 ноября — 12 ответов», легенду интенсивности и доступные названия ячеек. Сохранить различимость активности без необходимости угадывать оттенок.

   **Основание:** Подтверждено. [stats/StatsScreen.kt:383](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/stats/StatsScreen.kt:383). Снимки: [editorial/progress-monochrome-light.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/editorial/progress-monochrome-light.png).

18. **[P2] Режим «Меньше движения» применяется не ко всем анимациям.** Общие Quiet-компоненты учитывают настройку, но animateContentSize и animateItem в чате, часть animateItem в библиотеке/практике, вращение стрелки Disclosure и изменения тепловой карты обходят её.

   **Решение:** Подключить все движения к одной политике: при reduceMotion — статичная смена размера/положения, допустимые короткие растворения по согласованному правилу. Проверить настройку на живом обновлении сообщений и списков.

   **Основание:** Подтверждено кодом. [conversation/ChatScreen.kt:102](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/conversation/ChatScreen.kt:102); [conversation/ChatScreen.kt:194](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/conversation/ChatScreen.kt:194); [components/Usability.kt:36](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Usability.kt:36); [stats/StatsScreen.kt:403](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/stats/StatsScreen.kt:403).

19. **[P2] Обучающая анимация мешает чтению ответа.** После первого раскрытия карточка сама ездит вправо-влево примерно на 12% ширины и показывает «Хорошо»/«Снова». Это происходит, когда пользователь начинает читать; похоже на уже выбранную оценку.

   **Решение:** Показать жест на отдельной миниатюре или запустить демонстрацию по запросу. Основную карточку оставлять неподвижной, пока её не двигает пользователь.

   **Основание:** Подтверждено. [study/StudyScreen.kt:449](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/study/StudyScreen.kt:449). Снимки: [editorial/answer-monochrome-dark.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/editorial/answer-monochrome-dark.png).

20. **[P2] Заставка добавляет ожидание после готовности приложения.** В коде задано вступление 2800 ms и выход 720 ms; короткий вариант — 1300 + 720 ms. Даже готовый интерфейс ждёт окончания вступления, если пользователь не догадается пропустить его касанием.

   **Решение:** Показывать приложение сразу после готовности. Полную заставку оставить первому знакомству; для обычного запуска использовать короткое завершение системного splash без обязательного ожидания.

   **Основание:** Подтверждено кодом. [launch/LaunchScreen.kt:86](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/launch/LaunchScreen.kt:86); [launch/LaunchScreen.kt:115](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/launch/LaunchScreen.kt:115).

21. **[P2] Второстепенные блоки отодвигают основной контент.** В практике перед каталогом стоят крупное продолжение, разговор с ИИ и все группы ошибок. В библиотеке поиск расположен после большого продолжения. В низком горизонтальном окне даже «Начать» на главной требует прокрутки.

   **Решение:** На главной поставить короткий блок занятия первым. В практике компактно свернуть ошибки и продолжение; фильтр и упражнения поднять выше. В библиотеке держать поиск рядом с заголовком, независимо от книги продолжения.

   **Основание:** Экспертная рекомендация по снимкам. [home/HomeScreen.kt:112](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/home/HomeScreen.kt:112); [games/GamesScreen.kt:68](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/games/GamesScreen.kt:68); [library/LibraryScreen.kt:181](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/library/LibraryScreen.kt:181). Снимки: [editorial/practice-monochrome-light.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/editorial/practice-monochrome-light.png), [editorial/library-monochrome-light.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/editorial/library-monochrome-light.png), [landscape-current.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/landscape-current.png).

   **Дополнение после пятой пачки:** на 320 dp / 200% у акцентной карточки практики название «Грамматический тест» теснится иконкой, а «Продолжить» — стрелкой и отступами кнопки; слова разрываются. [Снимок](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-fixes-5/large-text/contrast-and-hero/practice-caption.png). При работе над компоновкой практики убрать иконку из колонки названия при недостаточной ширине, а действиям дать ширину по самому длинному слову либо перенести стрелку. Кегль не уменьшать.

22. **[P2] Навигация резко меняет доступность разделов.** При крупном тексте пять вкладок превращаются в одно «Все разделы»: переход требует дополнительного действия. На 740×320 dp выбор по одной ширине включает высокий rail с логотипом, и в нём одновременно видны только первые два раздела.

   **Решение:** Учитывать и ширину, и высоту. Для низкого окна использовать компактный rail/меню без большого логотипа. Для крупного текста сохранить быстрый доступ к частым разделам и понятное постоянное меню остальных.

   **Основание:** Экспертная рекомендация по воспроизведённому поведению. [nav/AlbionNavigation.kt:205](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/nav/AlbionNavigation.kt:205); [nav/AlbionNavigation.kt:280](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/nav/AlbionNavigation.kt:280); [nav/AlbionNavigation.kt:362](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/nav/AlbionNavigation.kt:362). Снимки: [grammar-landscape.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/grammar-landscape.png), [compact-ui/home-320-2.0.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/compact-ui/home-320-2.0.png).

23. **[P3] Выбранное состояние оформляется несколькими способами.** Сегменты и ChoiceRow используют тонированный ирис с рамкой, выбранная строка OptionPicker — серую поверхность, selectedRow — secondaryContainer. Общие роли selected/default/focused описаны и реализованы неодинаково.

   **Решение:** Ввести единые токены selectedContainer, selectedBorder и selectedContent. Согласовать их применение для меню, списков, сегментов и плиток с учётом роли компонента.

   **Основание:** Замечание к единству стиля. [components/Components.kt:398](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Components.kt:398); [components/Components.kt:433](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Components.kt:433); [components/OptionPicker.kt:77](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/OptionPicker.kt:77); [components/Selection.kt:13](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Selection.kt:13).

24. **[P2] Разрушительные действия не имеют единого оформления.** Удаление книги и прогресса учебника выделено error-цветом; удаление беседы и модели использует обычный TextButton, а сброс колоды — onSurface. Одинаково опасные подтверждения выглядят по-разному.

   **Решение:** Сделать общий компонент подтверждения удаления/сброса с единым error-цветом, порядком действий и формулировкой последствий. Обычные действия не оформлять как разрушительные.

   **Основание:** Подтверждено кодом. [library/LibraryScreen.kt:229](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/library/LibraryScreen.kt:229); [conversation/ChatScreen.kt:181](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/conversation/ChatScreen.kt:181); [conversation/ConversationsScreen.kt:135](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/conversation/ConversationsScreen.kt:135); [deck/DeckScreen.kt:159](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/deck/DeckScreen.kt:159); [library/TranslationModelControls.kt:74](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/library/TranslationModelControls.kt:74).

25. **[P3] Одинаковая стрелка обозначает переход и раскрытие.** Закрытый Disclosure использует ArrowForward; очень похожая стрелка стоит в строках перехода на другую страницу. Внешне неясно, откроется новая страница или раскроется содержимое.

   **Решение:** Для раскрытия использовать chevron вниз/вверх, для перехода — вправо. Сохранить одинаковый размер и положение этих индикаторов во всех разделах.

   **Основание:** Замечание к единству стиля. [components/Usability.kt:59](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Usability.kt:59); [components/EditorialLink.kt:73](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/EditorialLink.kt:73). Снимки: [editorial/settings-monochrome-light.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/editorial/settings-monochrome-light.png).

26. **[P3] Превью размера учебника показывает другой шрифт.** В настройках образец учебника использует bodyLarge/Onest, тогда как реальные правила набраны ReadingStyle/Literata. По образцу нельзя точно оценить результат настройки.

   **Решение:** Переиспользовать реальный компонент абзаца учебника или тот же ReadingStyle с тем же межстрочным интервалом.

   **Основание:** Подтверждено кодом. [settings/SettingsScreen.kt:86](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/settings/SettingsScreen.kt:86); [grammar/MarkdownView.kt:88](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/grammar/MarkdownView.kt:88).

27. **[P3] Часть учебного текста не учитывает локальный масштаб.** CodeView фиксирует 13 sp и не применяет LocalMdEnv.scale. При увеличении текста учебника блоки MdCode остаются прежнего размера относительно остального материала.

   **Решение:** Применять локальный масштаб к кеглю и интерлиньяжу всех учебных блоков. Если моноширинный текст важен, оставить гарнитуру, но масштабировать её вместе с уроком.

   **Основание:** Подтверждено кодом. [grammar/MarkdownView.kt:436](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/grammar/MarkdownView.kt:436).

28. **[P3] Удаление своей книги спрятано в долгом нажатии.** Обычное меню книги отсутствует; способ удаления объясняет одноразовая подсказка. После её закрытия действие трудно обнаружить вновь.

   **Решение:** Добавить видимую кнопку «Ещё» у пользовательской книги или меню на странице книги. Долгое нажатие можно оставить дополнительным способом.

   **Основание:** Экспертная рекомендация. [library/LibraryScreen.kt:268](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/library/LibraryScreen.kt:268).

29. **[P2] На планшете большая правая панель изначально пустует.** На 1200×800 dp учебник сразу разделяется на фиксированный список 380 dp и большой placeholder. Большая карточка следующего шага сужена в левой панели, а правая половина не помогает выбрать материал.

   **Решение:** В пустой правой панели показывать рекомендованный урок или обзор маршрута. Список слева сделать компактным; карточку рекомендации перенести вправо, сохранив явный выбор пользователя.

   **Основание:** Экспертная рекомендация по воспроизведённому поведению. [nav/ListDetail.kt:85](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/nav/ListDetail.kt:85); [nav/ListDetail.kt:112](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/nav/ListDetail.kt:112); [grammar/GrammarRoadmapUi.kt:87](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/grammar/GrammarRoadmapUi.kt:87). Снимки: [grammar-tablet.png](C:/Users/Marat/Desktop/albion/AlbionApp/build/ux-audit/grammar-tablet.png).

30. **[P2] Безопасные зоны и шарнир не учтены общей системой.** Scaffold обнуляет contentWindowInsets, экраны в основном добавляют только statusBars/navigationBars. Явной общей обработки бокового displayCutout и разделяющего шарнира нет. Перекрытие на конкретном физическом устройстве не воспроизводилось.

   **Решение:** Применить safeDrawing/displayCutout к интерактивному содержимому, отдельно оставив фон edge-to-edge. Для складных устройств выбирать панели с учётом folding feature и доступной области каждой панели.

   **Основание:** Риск по коду; требуется отдельная проверка. [nav/AlbionNavigation.kt:210](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/nav/AlbionNavigation.kt:210); [components/Components.kt:345](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/components/Components.kt:345); [nav/ListDetail.kt:85](C:/Users/Marat/Desktop/albion/AlbionApp/app/src/main/java/com/marat/albion/ui/nav/ListDetail.kt:85).

**Рекомендуемая последовательность исправлений.** Сначала 1–3 (недоступное содержимое/действия), затем 4–11 и 14–18 (адаптивность, контраст, движение), затем привести к общей системе состояния и остальные детали. Исправления общих компонентов проводить до точечной полировки отдельных экранов.

**Критерии приёмки.** При 320×740 dp / 200% и 740×320 dp / 100% карточка читается и все четыре оценки доступны; загрузка модели достижима прокруткой; при открытой клавиатуре виден хотя бы один полноценный результат поиска. Названия и коды уровней не рвутся по буквам. Для текстовых цветовых пар — контраст не ниже применимого ориентира 4,5:1; значимые границы и состояния проверяются отдельно. «Меньше движения» отключает движение во всех разделах, включая чат и обновление списков. Для новой проверки добавить снимки с длинными названиями, большими числами и выбранными/ошибочными/недоступными состояниями.

**Ограничения.** Плавность по frame timing/FPS не измерялась. Физические вырезы, шарнир складного устройства, все версии Android и все комбинации окон не проверены. Для них приведены риски по коду, а не утверждения о воспроизведённом перекрытии. Часть тестовых снимков содержит узкий контейнер и пустую область справа/снизу; это испытательный стенд, а не дефект приложения. Старые снимки из build/review не использованы как доказательства текущего дизайна.

**Основания для численных ориентиров.** [Android: доступность, контраст и цели касания](https://developer.android.com/guide/topics/ui/accessibility/apps); [WCAG 2.2: контраст нетекстовых элементов](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html); [Android Compose: обработка вырезов](https://developer.android.com/develop/ui/compose/system/cutouts). Порог 3:1 относится к необходимым для распознавания границам/состояниям, а не ко всем декоративным линиям. Для рекомендаций по кеглю 12–13 sp нет утверждения о нормативном минимуме WCAG.

