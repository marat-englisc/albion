# kotlinx.serialization: ключи навигации и JSON-модели
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.marat.albion.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.marat.albion.**$$serializer { *; }

# ML Kit (перевод фраз) находит свои компоненты через рефлексию по манифесту —
# без этих правил R8 их вырезает и release падает при создании переводчика.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keep class com.google.firebase.components.** { *; }

# PdfBox-Android: необязательный декодер JPEG 2000 не подключён, ресурсы шрифтов грузятся по имени.
-dontwarn com.gemalto.jp2.**
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
