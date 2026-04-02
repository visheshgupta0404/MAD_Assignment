# proguard-rules.pro
# ProGuard / R8 rules for the Photo Gallery app.
# These rules prevent code shrinking from removing classes that are
# referenced reflectively or via XML (which the static analyser can't see).

# Keep all Activity subclasses (referenced in AndroidManifest)
-keep public class * extends android.app.Activity

# Keep the FileProvider (referenced in AndroidManifest)
-keep class androidx.core.content.FileProvider

# Keep RecyclerView adapter and ViewHolder (referenced from XML / reflection)
-keep class * extends androidx.recyclerview.widget.RecyclerView$Adapter
-keep class * extends androidx.recyclerview.widget.RecyclerView$ViewHolder

# Suppress warnings for unused classes in library dependencies
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
