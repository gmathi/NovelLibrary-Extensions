
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

![image](https://user-images.githubusercontent.com/5333537/160157710-564394fc-f0d2-406b-a513-fcadec554a19.png)

Resync the project and you should have your extension ready to work with. 

Now open the `Xyz.kt` file and start making the changes to make sure you get your data.
