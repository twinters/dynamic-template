# Dynamic Template

The Dynamic Template algorithm is a text generation algorithm that is used to automatically imitate the global style of given input texts.
Most notably, it is used in the [TorfsBot](https://twitter.com/TorfsBot) twitterbot and the [Dutch Humor Detection by Generating Negative Examples](https://github.com/twinters/dutch-humor-detection).

The algorithm replaces low frequency words of a base text with other words from one or more other context texts having the same grammatical form.
It this effectively turns any text of the corpus into a template, that is dynamically determined based on the available context words from another line of the input corpus.

This implementation works with the Dutch package of [LanguageTool](http://wiki.languagetool.org/) for labeling the part-of-speech tags.

You can read more about the algorithm on [our TorfsBot paper](https://arxiv.org/pdf/1909.09480.pdf) or [Dutch humor detection paper](https://arxiv.org/pdf/2010.13652.pdf).

## How to use

### Dependencies

The following repositories need to be cloned in folders next to this repository, as they are dependencies of this project:
- [generator-util](https://github.com/twinters/generator-util)
- [language-util](https://github.com/twinters/language-util)
- [chatbot-util](https://github.com/twinters/chatbot-util)
- [text-util](https://github.com/twinters/text-util)
- [markov](https://github.com/twinters/markov)

### Running the generator

1. Clone this repository, and also the dependencies listed above.
2. Open the project in an IDE that supports [Gradle](https://gradle.org/), e.g. [IntelliJ](https://www.jetbrains.com/idea/).
3. Build the Gradle project from `build.gradle`.
4. Run `DynamicTemplateExecutor.java`, which accepts a *"base text"* corpus and a *"context words"* corpus as its first and second arguments.
