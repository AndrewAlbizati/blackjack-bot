# Blackjack Discord Bot
This bot allows users to play Blackjack online through Discord. This bot was written in Java using IntelliJ IDEA and is designed to be used on small Discord servers.

## Setup
1. Add a file titled `config.properties` into the folder.
2. Add the following to the file (replace `{Discord bot token}` with your bot's token)
```
token={Discord bot token}
```
3. Enter ```gradle run``` in a command prompt or terminal in the folder. Setup is complete after that. (It may take a few minutes for the Blackjack command to initialize after startup)

## How to Play
To play Blackjack with the bot, type `/blackjack <bet>` in any channel that the bot is allowed to read and send messages. The bot will give the user basic instructions on how to play.

## Dependencies
- Javacord 3.3.2 (https://github.com/Javacord/Javacord)
- JSONSimple 1.1.1 (https://github.com/fangyidong/json-simple)