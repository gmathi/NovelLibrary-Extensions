# {WIP}|
# NovelLibrary-Extensions
Extensions for NovelLibrary

# How to create extensions

## Prerequisites
- Android Studio
- Kotlin 
- Some basic coding knowledge

## Steps to get going:
### Step 1: Identify the below URLS / URL Requests 
1. Search URL that takes in a search query parameter.
2. URL that is required to fetch the novel details.
3. URL(s) that are required to fetch chapters based on the novel selected from the above search results.

### Step 2: Create your own package entry in Android Studio.
- Make sure you are able to open project in Android Studio and able to build it. If you have successfully built it, then you would see something like this under package explorer.

![image](https://user-images.githubusercontent.com/5333537/159209131-8f110590-d64c-4302-8057-61a12d72d056.png)


- Open up extension

![image](https://user-images.githubusercontent.com/5333537/159209216-d3802f89-077e-43a4-93ed-37c29e20fef2.png)

#### Lets assume you want to add the new source `xyz.com`.

- First go to the folder window for the project.

![image](https://user-images.githubusercontent.com/5333537/159209536-1f6f09a9-ed30-4624-aca6-22874c70b1ce.png)+


### Step 3: Now we will start with copying one of the folders and renaming the below files/contents to `xyz` since that is the host name.

- Change the folder name to `xyz`.

![image](https://user-images.githubusercontent.com/5333537/159209656-0de6bd67-3a88-4cb1-8270-b4de956ee654.png)

- Inside the folder, open the `build.gradle`. Update the values as such.
```
apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'

ext {
    extName = 'Xyz'
    pkgNameSuffix = 'en.xyz'
    extClass = '.Xyz'
    extVersionCode = 1
    libVersion = '1.0'
}

apply from: "$rootDir/common.gradle"
``` 

- Rename the below folder and file name to `xyz`.

![image](https://user-images.githubusercontent.com/5333537/159210297-090a6a65-987a-43b7-b5af-64ae311d9d27.png)
to

![image](https://user-images.githubusercontent.com/5333537/159210643-1a6c2f42-c34a-46ba-a58a-65b59076c4b1.png)

- Also rename the contents in the `Xyz.kt` as below.

![image](https://user-images.githubusercontent.com/5333537/159210466-a970f7bc-88b6-4ce0-a562-5f6461b7e578.png)


### Step 4: Open Android Studio.

Goto `settings.gradle` file and add this entry. 
```
include ':extensions:individual:en:xyz'
```

Resync the project and you should have your extension ready to work with. 


- Right click on `en` in `extensions`.`individual`.`en` and go to `Add New Module`.

![image](https://user-images.githubusercontent.com/5333537/160046577-61527424-34bf-4b13-909a-1323f0c7f6d8.png)

- Copy this package name - `io.github.gmathi.novellibrary.extension.en.{replace with your source name}`, so in this case it will be `io.github.gmathi.novellibrary.extension.en.xyz`. And fill the details and make sure they look the same as below image. Make sure to uncheck the .kts gradle option.

![image](https://user-images.githubusercontent.com/5333537/160047224-c59c0f01-3172-47be-9391-0b6c8b1d0588.png)

- Click `Next` and select `Blank Activity`. Hit finish.

![image](https://user-images.githubusercontent.com/5333537/160047042-57893904-5ee2-49a2-a41a-e9b6b428816d.png)



If you have done all the steps correctly as above, then you will see the new entry after you have re-opened / resynced the project.

