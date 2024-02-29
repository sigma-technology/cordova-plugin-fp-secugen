# Cordova Secugen Finger Print Plugin Android
Customised for Fingerprint Scanner App

## Simple cordova plugin that takes care of Secugen Native APIs for Android.

### Steps to add this plugin in your project.

1.  Add the plugin in your project **cordova plugin add sigma-technology/cordova-plugin-fp-secugen**
2.  You have to request for jar and .so files from Secugen Website. They ask a valid email id and send android sdk to this email.
3.  Copy complete **libs folder** from the provided zip file to your projects **android/app/libs** folder.  
4.  Also add **FDxSDKProFDAndroid.jar** file from zip sdk file to the your projects **android/app/libs** folder.
5.  Now add **FDxSDKProFDAndroid.jar** and ***.so** to dependencies in **android/app/build.gradle** file (replacing the first line to also include .so files) eg:

``` 
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar', '*.so'])
    // Other dependencies
    implementation files('libs/FDxSDKProFDAndroid.jar')
}
```

6. Add jniLibs to sourceSets in **android/app/build.gradle** file eg:

```
sourceSets {
    main.jniLibs.srcDirs = ["libs"]
}
```

### How to use it?

Add the following code snippet anywhere after deviceready event.

``` 
fpindex.capture(
    function(resp) {
        //resp json object contains fp data...
        console.log(resp);
    },
    function(b) { //Error handler...
        fpindex.requestPermission(function(a) {
            console.log(a);
        }, function(b) {
            console.log(b);
        });
    }
);
```

