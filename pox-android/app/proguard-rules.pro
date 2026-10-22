# KotlinX Serialization keep rules
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt

-keepclassmembers class * {
    *** Companion;
}

-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep polymorphic serializable models
-keepclassmembers class com.example.genpox.data.network.HydraPacket** { *; }
-keep class com.example.genpox.data.network.** { *; }

# Room Database keep rules
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.genpox.data.PoxDao** { *; }
-keep class com.example.genpox.data.PoxDatabase** { *; }
-dontwarn androidx.room.paging.**

# Compose Stability & Model Keep Rules
-keep class com.example.genpox.theme.CyberColors { *; }
-keep class com.example.genpox.data.ModelsKt** { *; }
-keep class com.example.genpox.data.Creature** { *; }
-keep class com.example.genpox.data.GeneSequence** { *; }
-keep class com.example.genpox.data.HarvestMission** { *; }
-keep class com.example.genpox.data.ReagentStock** { *; }
