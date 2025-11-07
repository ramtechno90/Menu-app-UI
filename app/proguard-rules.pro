-keep class com.example.menuapp.data.model.** { *; }

# Keep the no-argument constructor for all data model classes, which is required by Firestore.
-keepclassmembers class com.example.menuapp.data.model.** {
    public <init>();
}
