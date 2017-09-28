# DocumentRecognitionLibrary

## Pre-requisites
- Android SDK 24
- Android Build Tools v25.0.0

## How to install
To get a Git project into your build: 
- Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories: </br>
```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
		maven { url  "http://dl.bintray.com/steveliles/maven" }
	}
}
```
  
- Step 2. Add the dependency </br>
```
dependencies {
	compile 'com.github.karmaobserver:DocumentRecognitionLib-Android:0.1.6'
}	
```

## API Methods

| Method |
| ------------- |
| Mat detectAndPrepareDocument(Mat originalMat, Integer tolerance, Float percentage, Integer regions)  |

| Method Description|
| ------------- |
| Detect document, if it is a document, prepare it for OCR otherwise make smooth Image.  |

| Param  | Description |
| ------------- | ------------- |
| @param originalMat  | Mat which should be processed.  |
| @param tolerance  | Tolerance which will be used as threshold.  |
| @param percentage  | Percentage which defines how many white pixels needs to be contained in document to be valid document.  |
| @param regions  | Regions number which defines how many regions needs to be contained in document to be valid document.  |
| @return  | Mat which is prepared for OCR in case Mat is a document, else MAT as smooth Image.  |

## License
This project is licensed under the MIT License - see the LICENSE.md file for details
