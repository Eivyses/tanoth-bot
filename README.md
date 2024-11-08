# Tanoth bot

this is a browser game bot, created for fun.

## Building

use `./gradlew clean build` to create a standalone jar file that will be placed to `build/libs` folder

## Usage

To use the bot you will need to install java version 17+

Usage from terminal:

```
java -jar TanothBot-{version}-standalone.jar --session-id <your-id> --gf-token <your-token> --auto-runes
```

More info about parameters is available when running executable without parameters like

```
java -jar TanothBot-{version}-standalone.jar
```

## Tokens

### Session ID

Session ID is used to connect to currently existing browser session (will disconnect after some time). It can be found
by logging into the game and inspecting HTML code by pressing F12 and navigating to `Inspector`
or `Elements` tab. sessionID will be in the first script inside `<body>` tag

### GF token

gf-token is used to get a longer session as it can be used to automatically refresh session-id and keep your connection
for longer periods of time. It is located inside cookies, you can navigate there by pressing F12 and opening `Storage`
or `Application tab. It will be under `Cookies` sidebar called as `gf-token-production`
