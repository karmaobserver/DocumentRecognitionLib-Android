# DocumentRecognitionLibrary

## Pre-requisites
- Android SDK 25
- Android Build Tools v25.0.3

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
	compile 'com.github.karmaobserver:DocumentRecognitionLib-Android:0.1.7'
}	
```

## API Methods

| Method |
| ------------- |
|boolean detectDocument(Mat originalMat, Integer tolerance, Float percentage, Integer regions)  |

| Method Description|
| ------------- |
| Detects if the provided image is document or picture.  |

| Param  | Description |
| ------------- | ------------- |
| @param originalMat  | Mat which should be processed.  |
| @param tolerance  | Tolerance which will be used as threshold.  |
| @param percentage  | Percentage which defines how many white pixels needs to be contained in the image to be valid document.  |
| @param regions  | Regions number which defines how many regions needs to be contained in the image to be valid document.  |
| @return  | boolean True if provided Mat object is document, False otherwise.  |

|Mat prepareDocumentForOCR(Mat originalMat, int whiteBorderPercentage, int givenImagePrecision)  |

| Method Description|
| ------------- |
| Prepares provided Mat object for OCR.  |

| Param  | Description |
| ------------- | ------------- |
| @param originalMat  | Mat which should be processed.  |
| @param whiteBorderPercentage  | Defines the size of the frame. Suggested value for this parameter: 2  |
| @param givenImagePrecision  | Image clearness; allowed values for this parameter: 0 - 16  
		                ( 0 - for the images with low contrast, 16 - for the images with high contrast )
                                Suggested value for this parameter: 16  |
| @return  | Image prepared for OCR. |

## License
This project is licensed under the MIT License - see the LICENSE.md file for details
