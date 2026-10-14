#!/usr/bin/env bash
# Final step of every desktop release, after CI has attached the Windows .msi:
#   scripts/sign-release.sh v1.0.6
# Downloads the release's installers, regenerates SHA256SUMS over all of them, signs it with the
# release key (SHA256SUMS.sig, raw Ed25519) and uploads both. Installed copies of Bori refuse an
# update whose SHA256SUMS.sig doesn't verify against the public key in DesktopAppUpdater.kt.
#
# The private key lives only on this machine: ~/.config/bori-release/update-signing-key.pem.
# Back it up. If it's lost, installed copies can't update themselves any more (a new key means
# everyone reinstalls by hand once).
set -euo pipefail

tag="${1:?usage: sign-release.sh <tag, e.g. v1.0.6>}"
key="${BORI_SIGNING_KEY:-$HOME/.config/bori-release/update-signing-key.pem}"
repo="Atom0131/Bori---Habit-Tracker"
pubkey_expected="MCowBQYDK2VwAyEArvuRqm7JHqCZHataTLEGEfMNeNWVT/atxaDATWUgHOU="

[[ -r "$key" ]] || { echo "No signing key at $key" >&2; exit 1; }
pub="$(openssl pkey -in "$key" -pubout -outform DER | base64 -w0)"
[[ "$pub" == "$pubkey_expected" ]] || { echo "That key isn't the one Bori trusts." >&2; exit 1; }

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
cd "$work"

gh release download "$tag" --repo "$repo" \
    --pattern '*.msi' --pattern '*.deb' --pattern '*.rpm' --pattern '*.pkg.tar.zst'
ls *.msi >/dev/null 2>&1 || { echo "No .msi on $tag yet: wait for the Windows installer workflow." >&2; exit 1; }

sha256sum -- *.deb *.rpm *.pkg.tar.zst *.msi > SHA256SUMS
openssl pkeyutl -sign -inkey "$key" -rawin -in SHA256SUMS -out SHA256SUMS.sig
openssl pkeyutl -verify -pubin -inkey <(openssl pkey -in "$key" -pubout) -rawin \
    -in SHA256SUMS -sigfile SHA256SUMS.sig >/dev/null

gh release upload "$tag" --repo "$repo" SHA256SUMS SHA256SUMS.sig --clobber
echo "Signed and uploaded:"
cat SHA256SUMS
