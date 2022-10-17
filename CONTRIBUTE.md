
You want to report a bug, add a feature wish or maybe even add some code?
That's great :D Here's how to do that.

# Report feedback (no GitHub-account needed)

1. Open the GeoNotes app.
2. Go into the settings (upper right menu → "Settings"/gear-icon) and click on the "Feedback" button at the bottom of the screen.

This will open your default E-Mail App on your phone so that you can now write me an E-Mail.

# Report bug / add feature request

1. Search through the [existing issues](https://github.com/hauke96/GeoNotes/issues) if equal/similar requests already exist.
2. If not, open a [new issue](https://github.com/hauke96/GeoNotes/issues/new). If your concern is already discussed in an existing issue, feel free to join the discussion.
3. Describe the bug/feature as clearly as possible. Maybe add some screenshots or drawings to clear things up.
4. Be open for questions and an discussion.

After a possible discussion, the bug will hopefully be fixed or the feature implemented.
Don't be sad if your feature won't make it. This is not my only project and I'm running this in my spare time, my resources are therefore quite limited ;)

# Translate the app

The translations are within simple XML files, so it's kind of like code.
This means you have to **fork and clone this repo** before you can start, so make yourself familiar with git, GitHub, forks and pull-requests.

## Enhance an existing translation

1. Go to `app/src/main/res/values-LANG` (where `LANG` is the language you want to enhance, so e.g. `it` if you want to improve the Italian translation)
2. Open the `strings.xml` file and improve the translations.
  * Please make sure that the order of the entries is the same as in the original `values/strings.xml` file:
3. Create a commit, push it and open a pull-request on GitHub.

## Add new language

1. Go to `app/src/main/res/`
2. Create a folder `values-LANG` (where `LANG` has to be replaced with the language code of the language you want to add, so e.g. `it` for Italian)
3. Copy the `strings.xml` from the `values` folder. This is the original English file.
4. Replace each English string by the translated one. 
  * Example: `<string name="reset">Reset</string>` becomes `<string name="reset">Zurücksetzen</string>` for the German translation:
  * Please make sure that the order of the entries is the same as in the original `values/strings.xml` file:
5. Create a commit, push it and open a pull-request on GitHub.

# Contribute code

Please create an issue before adding code (except it's just a spelling mistake or something similarly small).

1. Open a [new issue](https://github.com/hauke96/GeoNotes/issues/new).
2. Describe the changes you want to make as clearly as possible. Maybe add mock-ups/drawings, code snippets, diagrams, etc. to clear things up.
3. Be open for questions and an discussion.
4. When everything is clear, enjoy coding ;)
5. Push your changes and open a pull-request on GitHub

Don't be sad if I don't want your feature idea to be in GeoNotes.
This is my private project and I have a certain idea (s. below) what this app should be and what not.
But feel free to create a fork and develop your own version of this app :)