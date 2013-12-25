DynamicDEXApplicationV1
=======================

DynamicDEXApplication

Function 'DexClaasLoader' can load another classes.dex. but the classes.dex file must appear on the storage on mobile phones. Android 4.0 provide functions that can load classes.dex dynamicely. so we write a SuperClassLoader function which can load classes.dex dynamicely. but there are some problems. the overrided function findclass is called by the system. and parameter passed is determined by system, and may not be found. here are the sources. this application is called 'ringtone'.
