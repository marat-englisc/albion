# Дизайн Albion

Стиль приложения сброшен до стокового Material 3 (шаблон Android Studio):

- `ui/theme/Color.kt`, `Type.kt`, `Theme.kt` — стандартная тема: светлая/тёмная по системе, Dynamic Color на Android 12+, `Typography()` и `Shapes()` по умолчанию, системный шрифт.
- `res/values/themes.xml` — `android:Theme.Material.Light.NoActionBar` без переопределений.
- Общие компоненты в `ui/components/` — тонкие обёртки над стандартными `Button`, `OutlinedButton`, `Card`, `ListItem`, `TopAppBar`, `Slider`, `LinearProgressIndicator`, `HorizontalDivider`.

Новый дизайн строится с нуля поверх этой базы.
