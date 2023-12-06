# Parrot

This started as a silly Discord bot that reacts to one-word messages with that message itself, for use by
a 20-year-old gaming community.

It continues to amass some functionality, however. The bot is a hodgepodge of features, such as:

* Cali Morning Greetings
* Cali Evening Greetings
* Reactions
* Wordle

## Example

<img width="214" alt="Screen Shot 2022-02-01 at 9 36 50 PM" src="https://user-images.githubusercontent.com/515201/152089447-30c05444-c359-4838-8f1c-180f94908dc6.png">

## Developer Notes

* Define a `.dotenv` file was follows:

```
export PARROT_BOT_TOKEN=your bot token
```

* Then, source it, run `sbt`, and from its command line issue a `reStart` command:

```bash
longshorej@visions parrot % source .dotenv
longshorej@visions parrot % sbt
[info] welcome to sbt 1.5.2 (AdoptOpenJDK Java 11.0.11)
[info] loading settings for project parrot-build from plugins.sbt ...
[info] loading project definition from /Users/longshorej/work/parrot/project
[info] loading settings for project parrot from build.sbt ...
[info] set current project to parrot (in build file:/Users/longshorej/work/parrot/)
[info] sbt server started at local:///Users/longshorej/.sbt/1.0/server/88e0ee16f07496fca839/sock
[info] started sbt server
sbt:parrot> reStart
[info] Application parrot not yet started
[info] Starting application parrot in the background ...
parrot Starting parrot.Entrypoint.main()
[success] Total time: 1 s, completed Feb 1, 2022, 9:04:39 PM
parrot[ERROR] [AckCord-akka.actor.default-dispatcher-3] INFO akka.event.slf4j.Slf4jLogger - Slf4jLogger started
parrot[ERROR] [guardian-akka.actor.default-dispatcher-3] INFO akka.event.slf4j.Slf4jLogger - Slf4jLogger started
parrot[ERROR] [AckCord-akka.actor.default-dispatcher-3] INFO akka.actor.ActorSystemImpl - Got WS gateway: wss://gateway.discord.gg
sbt:parrot> 
```
## Operator Notes

The bot currently runs on a plain old VPS. Run `universal:packageBin` in sbt to create a zip file,
copy it over, ssh to the machine, kill screen. There's a dotfile to be sourced with tokens in the
directory you unzip it to.

If bot has failed, ssh to machine and restart screen. Somehow this hasn't happened yet? Does the selected VPS hosting not reboot?!