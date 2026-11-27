# Regras extras do R8 (as bibliotecas trazem as suas).

# ML Kit acha os seus componentes por reflexão; sem isto o release fecha ao reconhecer o texto.
-keep class com.google.mlkit.** { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }

# PDFBox (texto de PDF): usa fontes e filtros por reflexão e partes opcionais que não vêm no app.
-keep class com.tom_roush.** { *; }
-dontwarn com.tom_roush.**
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
