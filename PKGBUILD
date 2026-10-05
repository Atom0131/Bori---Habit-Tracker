# Maintainer: Your Name <you@example.com>
# Fill in the line above with a real contact before submitting this to the AUR.
pkgname=rhythm
pkgver=1.0.0
pkgrel=1
pkgdesc="Habit tracker, journal, and planner — local-first, with optional sync to the Android app"
arch=('x86_64')
url="https://github.com/REPLACE_ME/rhythm"
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
    install -dm755 "$pkgdir/opt/rhythm"
    cp -r "$startdir/composeApp/build/compose/binaries/main/app/rhythm/"* "$pkgdir/opt/rhythm/"

    install -dm755 "$pkgdir/usr/bin"
    ln -s /opt/rhythm/bin/rhythm "$pkgdir/usr/bin/rhythm"

    install -Dm644 "$startdir/new_icon_assets/new_app_icon_512.png" "$pkgdir/usr/share/pixmaps/rhythm.png"
    install -Dm644 "$startdir/rhythm.desktop" "$pkgdir/usr/share/applications/rhythm.desktop"
    install -Dm644 "$startdir/LICENSE" "$pkgdir/usr/share/licenses/rhythm/LICENSE"
}
