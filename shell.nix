#nix-shell - p scala-cli - p jdk17 - I nixpkgs="https://github.com/NixOS/nixpkgs/archive/5b091d4fbe3b.tar.gz"

let
  nixpkgs = fetchTarball {
    name   = "nixos-unstable-2022-01-01";
    url    = "https://github.com/NixOS/nixpkgs/archive/5b091d4fbe3b.tar.gz";
    sha256 = "0yb7l5p4k9q8avwiq0fgp87ij50d6yavgh4dfw14jh2lf8daqbmp";
  };

  pkgs = import nixpkgs { };

in
pkgs.mkShell
{
  name = "scala-cli-shell";

  buildInputs = [
    pkgs.jdk17
    pkgs.scala-cli
  ];
}
