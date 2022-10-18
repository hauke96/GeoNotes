<img align="right" width="64px" src="https://raw.githubusercontent.com/hauke96/GeoNotes/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png">

# GeoNotes
A simple and lightweight app to create and manage georeferenced notes (text and photos) on a map. The goal is to create the notes as fast as possible without any unnecessary UI/UX overhead.

<p align="center">
<img src="screenshots.png" alt="GeoNotes Screenshots"/>
</p>

## Download

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/packages/de.hauke_stieler.geonotes/)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it on IzzyOnDroid" height="60">](https://apt.izzysoft.de/fdroid/index/apk/de.hauke_stieler.geonotes)
[<img src="https://user-images.githubusercontent.com/663460/26973090-f8fdc986-4d14-11e7-995a-e7c5e79ed925.png" alt="Download APK from GitHub" height="60">](https://github.com/hauke96/geonotes/releases/latest)

GeoNotes runs on Android 4.1 (SDK 16) and newer. There's no version at the Google Play store (yet).

## Need help?

See the [OSM Wiki page](https://wiki.openstreetmap.org/wiki/GeoNotes) for detailed descriptions about all the features.

## Features

* Create, move and delete notes
* Attach photos to note
* List of all notes
* Organize your notes with categories
* Export all notes in GeoJson or GPX format
* Show and follow current location

## Contribute to this project

You want to contribute to GeoNotes? Great! Please read the [CONTRIBUTE.md](CONTRIBUTE.md) file for further information.

## Use-case and Philosophy

### Basic idea of this app

Take notes as fast as possible while being outside (maybe even while walking or sitting in a bus) and later add the data to e.g. OSM.

### Usability principles

To implement the above goal/idea, the app follows some basic principles:

* **Simplicity:** Make creating, editing, moving and deleting of notes as fast/easy as possible.
* **No upload** of data and no creation of notes on osm.org.
* **General purpose:** No restriction in the content of a note.
* **Not a note management tool:** No import, no high level management operations.
* **Simple and pragmatic UI:** No unnecessary animations, no overloaded UIs.
* **Feature toggles:** The possibility to enable/disable features.

### Features which will probably *not* be added to GeoNotes

* Offline maps (too much work for now)
* Creating notes on osm.org
* Uploading data directly to OSM (there are [other apps](https://github.com/streetcomplete/StreetComplete) to do that)
* All sorts of features that will only be used by just a few users but require a lot of work to be implemented
* iOS and other non-android support

Try other apps like [StreetComplete](https://github.com/streetcomplete/StreetComplete) if you want to interact with OSM-data and osm.org notes directly.

