# UMLbot

UML genrator bot for Slack on top of PlantUML

## Let's try

1. setup a [Slack App](https://api.slack.com/apps) and subscribe it to `app_mention` events
3. give it access to `chat:write:bot` in the OAuth2 and Permissions tab 
2. [![Deploy](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy) this app
3. set env var URL to same as Heroku application.
4. set env var TOKEN to "Verification Token" from the Slack App
4. set env var BOT_TOKEN to value from Slack App (in OAUth2 and Permissions)

## Screenshot

![umlbot](https://raw.githubusercontent.com/taichi/umlbot/master/docs/umlbot.png)

![class](https://raw.githubusercontent.com/taichi/umlbot/master/docs/class.png)

![object](https://raw.githubusercontent.com/taichi/umlbot/master/docs/object.png)

![wireframe](https://raw.githubusercontent.com/taichi/umlbot/master/docs/wireframe.png)

## License

GPL v3

## Required Environment

Java8 or higher

## Build

    ./mvnw install

## Test

    ./mvnw test

## CI status

[![wercker status](https://app.wercker.com/status/c1ba9b381bde8b76b181c3d4a1cc90d0/m "wercker status")](https://app.wercker.com/project/bykey/c1ba9b381bde8b76b181c3d4a1cc90d0)
