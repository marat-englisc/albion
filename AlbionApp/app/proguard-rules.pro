# kotlinx.serialization: ключи навигации и JSON-модели
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.marat.albion.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.marat.albion.**$$serializer { *; }

# JNI looks up the bridge and streaming callback by their exact names.
-keep class com.marat.albion.data.translation.LlamaBridge { *; }
-keep interface com.marat.albion.data.translation.LlamaBridge$TokenCallback { *; }
-keepclassmembers class * implements com.marat.albion.data.translation.LlamaBridge$TokenCallback {
    public boolean onTokens(byte[]);
}

# PdfBox-Android: необязательный декодер JPEG 2000 не подключён, ресурсы шрифтов грузятся по имени.
-dontwarn com.gemalto.jp2.**
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }

# ML Kit находит свои компоненты рефлексией по имени класса и вызывает конструктор без аргументов.
# Полный режим R8 удаляет такие конструкторы — без правила офлайн-перевод книг в релизе не запускается.
-keep class * implements com.google.firebase.components.ComponentRegistrar { public <init>(); }
