# VOID Launcher keeps its data models intact.
-keep class com.void.launcher.model.** { *; }
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.coroutines.**
