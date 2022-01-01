# FIXME: coursier needs to fetch deps from Maven but can't make HTTP requests in a derivation...
# nix-build -I nixpkgs=https://github.com/NixOS/nixpkgs/archive/5b091d4fbe3b.tar.gz

{ pkgs ? import <nixpkgs> {} }:

let
  scala-cli = "${pkgs.scala-cli}/bin/scala-cli";
in
pkgs.stdenv.mkDerivation {
  name = "stargazers-raffle-0.0.1";

  src = ./.;

  buildInputs = with pkgs; [ coursier jdk17 ];

  phases = [ "installPhase" ];

  installPhase = ''
    mkdir -p $out/bin
    ${scala-cli} package . -o $out/bin/raffle
    chmod +x $out/bin/raffle
  '';
}
