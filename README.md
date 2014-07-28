# [Floobits](https://floobits.com/) plugin for IntelliJ

### (it also works for IntelliJ forks like RubyMine, PHPStorm, PyCharm, WebStom, Android Studio, and AppCode)

Real-time collaborative editing. Think Etherpad, but with native editors. This is the plugin for IntelliJ. We also have plugins for [Emacs](https://github.com/Floobits/floobits-emacs), [Vim](https://github.com/Floobits/floobits-vim), and [Sublime Text](https://github.com/Floobits/floobits-sublime).

[Documentation](https://floobits.com/help/plugins/intellij)

[Download from JetBrains](http://plugins.jetbrains.com/plugin/7389?pr=)

### Development status: Functional

This plugin should be completely functional. If you encounter any errors please email [support@floobits.com](mailto:support@floobits.com).

The Intellij IDEA plugin for Floobits is our most stable plugin so far. It works with all IntelliJ IDEA forks, except the stable version of PyCharm. The [EAP version of PyCharm](http://confluence.jetbrains.com/display/PYH/JetBrains+PyCharm+Preview+(EAP)) works fine.

### Contributing

You'll need to setup your plugin environment.

[JetBrains instructions](http://www.jetbrains.org/display/IJOS/Writing+Plug-ins)

[A helpful guide](http://bjorn.tipling.com/how-to-make-an-intellij-idea-plugin-in-30-minutes)

Questions? Join us on IRC on Freenode in #floobits.

<a href="https://floobits.com/Floobits/intellij-plugin/redirect">
  <img alt="Floobits status" width="100" height="40" src="https://floobits.com/Floobits/intellij-plugin.png" />
</a>


#### Making changes to common

Changes in "common" are shared across IDEs (like Eclipse). Changes there should be pushed to the git subtree.

To set up the git subtree for common:

```
git remote add -f common git@github.com:Floobits/plugin-java-common.git
```

Pushing changes for common:

```
git subtree push --prefix=src/floobits/common common master
```
