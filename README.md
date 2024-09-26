# Tanoth bot

this is a browser game bot

## Building

use `./gradlew clean build` to create an standalone jar file that will be placed to `build/libs` folder

## Usage

```
java -jar TanothBot-0.1-standalone.jar -sessionId <your-id> -rune AMBER -useGemsForAdventures true

* sessionId - your session id
* rune - optional parameter, if provided will upgrade rune
* useGemsForAdventures - optional parameter to use gems when adventure limit is reached, default false 
```