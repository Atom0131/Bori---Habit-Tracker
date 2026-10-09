# Maintainer: Your Name <you@example.com>
# Fill in the line above with a real contact before submitting this to the AUR.
pkgname=bori
# Renamed from "rhythm" on 2026-10-09; installing bori replaces an installed rhythm package.
conflicts=('rhythm')
replaces=('rhythm')
pkgver=1.0.0
pkgrel=1
pkgdesc="Habit tracker, journal, and planner, local-first, with optional sync to the Android app"
arch=('x86_64')
url="https://github.com/Atom0131/rhythm-desktop"
license=('Apache-2.0')
makedepends=('jdk17-openjdk')
options=('!strip')
source=()
sha256sums=()

build() {
    cd "$startdir"
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :composeApp:createDistributable --console=plain
}

package() {
    install -dm755 "$pkgdir/opt/bori"
    cp -r "$startdir/composeApp/build/compose/binaries/main/app/bori/"* "$pkgdir/opt/bori/"

    install -dm755 "$pkgdir/usr/bin"
    ln -s /opt/bori/bin/bori "$pkgdir/usr/bin/bori"

    install -Dm644 "$startdir/new_icon_assets/bori_app_icon_512.png" "$pkgdir/usr/share/pixmaps/bori.png"
    install -Dm644 "$startdir/bori.desktop" "$pkgdir/usr/share/applications/bori.desktop"
    install -Dm644 "$startdir/LICENSE" "$pkgdir/usr/share/licenses/bori/LICENSE"
}
