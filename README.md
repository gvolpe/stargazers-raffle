# stargazers-raffle

Run a raffle among the 🌟 stargazers 🌟 of a Github project!

## Overview

It is designed to run directly on Github actions but it can also be used locally on your command line. The CI build is configured with the repository we want the raffle to run against. Once completed, the winner will be listed as a comment of [this issue](https://github.com/gvolpe/stargazers-raffle/issues/1) in the following way.

ℹ️ 14 requests made to Github API ℹ️

🏆 🏆 🏆 @username 🏆 🏆 🏆

 * from 378 🌟 stargazers of https://github.com/author/repo!

### Run it locally

You need to have a JDK and [scala-cli](https://scala-cli.virtuslab.org/) installed, or you can use the given Nix shell.

The following command makes a binary named `raffle`.

```shell
$ nix-shell
$ scala-cli package . -o raffle -f
```

Run the raffle binary passing two arguments: author and repo name.

```shell
$ ./raffle
Missing expected positional argument!

Usage: stargazers-raffle [] [--show-all-users] <author> <repo>

Stargazers Raffle

Options and flags:
    --help
        Display this help text.
    --version, -v
        Print the version number and exit.
    --show-all-users, -s
        Display all the stargazers before raffle
    --post-winner
        Post the winner on the designated Github issue

Environment Variables:
    GH_TOKEN=<string>
        Github personal access token
```

For example.

```shell
$ ./raffle gvolpe dconf2nix
ℹ️ 2 requests made to Github API ℹ️

🏆🏆🏆 @username 🏆🏆🏆

 * from 50 🌟 stargazers of https://github.com/gvolpe/dconf2nix!
```
