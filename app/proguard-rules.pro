-keep class * extends androidx.room.RoomDatabase { void <init>(); }

-keep class * implements androidx.glance.appwidget.action.ActionCallback { void <init>(); }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { void <init>(); }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { void <init>(); }
