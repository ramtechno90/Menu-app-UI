-keep class com.pizzaparadize.menuapp.data.model.** { *; }

# Keep the no-argument constructor for all data model classes, which is required by Firestore.
-keepclassmembers class com.pizzaparadize.menuapp.data.model.** {
    public <init>();
}

-keep class com.pizzaparadize.menuapp.data.firebase.model.** { *; }

# Keep the no-argument constructor for all data model classes, which is required by Firestore.
-keepclassmembers class com.pizzaparadize.menuapp.data.firebase.model.** {
    public <init>();
}
