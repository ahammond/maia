# Maia

This is a very simple Scala IRC bot using PircbotX and Akka to control Hermes, a native OS X Pandora client with an AppleScript interface. It's not particularly well written. However, it is super robust, fast, and light on resources.

## Commands

`!!` is the default prefix.

* `np` does now playing
* `hate` thumbs down
* `love|like` thumbs up
* `skip` skips to next song
* `tired` dont play this for amonth
* `playpause|pauseplay|pp` toggles pause and play
* `help` get you a a link to here

## Assembling a single JAR with all the trimmings

1. Install `sbt` through something like Homebrew, conscript, or whatever.

2. Run `sbt assembly` and look in the `target/` folder.

3. There is no step 3.
