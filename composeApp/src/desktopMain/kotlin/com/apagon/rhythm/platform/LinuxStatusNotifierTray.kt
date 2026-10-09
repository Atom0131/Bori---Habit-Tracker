@file:Suppress("FunctionName") // D-Bus method names are dictated by the spec, capitalised

package com.apagon.rhythm.platform

import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.Struct
import org.freedesktop.dbus.Tuple
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.Position
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Bori's tray icon as a native **StatusNotifierItem** on the session D-Bus — the protocol KDE
 * Plasma (and GNOME with the AppIndicator extension) use for every modern tray icon.
 *
 * Why not Compose's `Tray`: it is `java.awt.TrayIcon`, which speaks the old XEmbed protocol. On
 * Plasma that goes through `xembedsniproxy`, which zooms the image (the icon came out cropped to
 * its middle) and has no transparency (a round icon sat in a grey square). Talking SNI directly
 * gets a real transparent, round icon like every other app in the tray. `start` returns null when
 * no `org.kde.StatusNotifierWatcher` is on the bus, and the caller falls back to Compose's `Tray`.
 *
 * Two objects are exported: the item at [ITEM_PATH], and its right-click menu at [MENU_PATH] in
 * the `com.canonical.dbusmenu` format Plasma renders. The menu is fixed (Show Bori / Quit), so it
 * never sends the layout-changed signals. Callbacks arrive on D-Bus threads; callers must hop to
 * the UI thread themselves.
 */
class LinuxStatusNotifierTray private constructor(private val connection: DBusConnection) : AutoCloseable {

    override fun close() {
        runCatching { connection.close() } // the item disappears from the tray with its connection
    }

    companion object {
        private const val ITEM_PATH = "/StatusNotifierItem"
        private const val MENU_PATH = "/MenuBar"
        private const val ID_SHOW = 1
        private const val ID_QUIT = 3

        fun start(title: String, onShow: () -> Unit, onQuit: () -> Unit): LinuxStatusNotifierTray? = runCatching {
            if (!System.getProperty("os.name").orEmpty().lowercase().contains("linux")) return null
            val connection = DBusConnectionBuilder.forSessionBus().build()
            try {
                val watcher = connection.getRemoteObject(
                    "org.kde.StatusNotifierWatcher", "/StatusNotifierWatcher", StatusNotifierWatcher::class.java
                )
                val busName = "org.kde.StatusNotifierItem-${ProcessHandle.current().pid()}-1"
                connection.requestBusName(busName)
                connection.exportObject(ITEM_PATH, Item(title, onShow, loadPixmaps()))
                connection.exportObject(MENU_PATH, Menu(title, onShow, onQuit))
                watcher.RegisterStatusNotifierItem(busName) // throws if there is no watcher
                LinuxStatusNotifierTray(connection)
            } catch (e: Exception) {
                runCatching { connection.close() }
                throw e
            }
        }.onFailure { System.err.println("Bori: native tray unavailable, using AWT tray: $it") }.getOrNull()

        /** ARGB32 in network byte order, one entry per bundled size; the host picks the closest. */
        private fun loadPixmaps(): List<Pixmap> = listOf(22, 32, 48, 64).mapNotNull { size ->
            val stream = LinuxStatusNotifierTray::class.java.getResourceAsStream("/tray/bori_tray_$size.png")
                ?: return@mapNotNull null
            val image: BufferedImage = stream.use { ImageIO.read(it) } ?: return@mapNotNull null
            val bytes = ByteArray(image.width * image.height * 4)
            var i = 0
            for (y in 0 until image.height) for (x in 0 until image.width) {
                val argb = image.getRGB(x, y)
                bytes[i++] = (argb ushr 24).toByte(); bytes[i++] = (argb ushr 16).toByte()
                bytes[i++] = (argb ushr 8).toByte(); bytes[i++] = argb.toByte()
            }
            Pixmap(image.width, image.height, bytes)
        }
    }

    // ---- D-Bus types -------------------------------------------------------------------------

    @DBusInterfaceName("org.kde.StatusNotifierWatcher")
    interface StatusNotifierWatcher : DBusInterface {
        fun RegisterStatusNotifierItem(service: String)
    }

    @DBusInterfaceName("org.kde.StatusNotifierItem")
    interface StatusNotifierItem : DBusInterface {
        fun Activate(x: Int, y: Int)
        fun SecondaryActivate(x: Int, y: Int)
        fun ContextMenu(x: Int, y: Int)
        fun Scroll(delta: Int, orientation: String)
    }

    @DBusInterfaceName("com.canonical.dbusmenu")
    interface DbusMenu : DBusInterface {
        fun GetLayout(parentId: Int, recursionDepth: Int, propertyNames: List<String>): LayoutTuple<UInt32, Layout>
        fun GetGroupProperties(ids: List<Int>, propertyNames: List<String>): List<ItemProps>
        fun GetProperty(id: Int, name: String): Variant<*>
        fun Event(id: Int, eventId: String, data: Variant<*>, timestamp: UInt32)
        fun EventGroup(events: List<MenuEvent>): List<Int>
        fun AboutToShow(id: Int): Boolean
        fun AboutToShowGroup(ids: List<Int>): AboutToShowTuple<List<Int>, List<Int>>
    }

    class Pixmap(@field:Position(0) @JvmField val width: Int, @field:Position(1) @JvmField val height: Int,
                 @field:Position(2) @JvmField val data: ByteArray) : Struct()

    class ToolTip(@field:Position(0) @JvmField val iconName: String, @field:Position(1) @JvmField val icons: List<Pixmap>,
                  @field:Position(2) @JvmField val title: String, @field:Position(3) @JvmField val text: String) : Struct()

    class Layout(@field:Position(0) @JvmField val id: Int, @field:Position(1) @JvmField val props: Map<String, Variant<*>>,
                 @field:Position(2) @JvmField val children: List<Variant<*>>) : Struct()

    // Tuples are generic on purpose: dbus-java reads a multi-value return's element types from the
    // method's parameterised return type, so a non-generic Tuple fails at export time.
    class LayoutTuple<A, B>(@field:Position(0) @JvmField val revision: A, @field:Position(1) @JvmField val layout: B) : Tuple()

    class ItemProps(@field:Position(0) @JvmField val id: Int, @field:Position(1) @JvmField val props: Map<String, Variant<*>>) : Struct()

    class MenuEvent(@field:Position(0) @JvmField val id: Int, @field:Position(1) @JvmField val eventId: String,
                    @field:Position(2) @JvmField val data: Variant<*>, @field:Position(3) @JvmField val timestamp: UInt32) : Struct()

    class AboutToShowTuple<A, B>(@field:Position(0) @JvmField val updatesNeeded: A,
                                 @field:Position(1) @JvmField val idErrors: B) : Tuple()

    // ---- Exported objects --------------------------------------------------------------------

    private class Item(
        private val title: String,
        private val onShow: () -> Unit,
        private val pixmaps: List<Pixmap>
    ) : StatusNotifierItem, Properties {
        override fun getObjectPath() = ITEM_PATH

        // Left click shows the window (ItemIsMenu = false); right click gets the dbusmenu.
        override fun Activate(x: Int, y: Int) = onShow()
        override fun SecondaryActivate(x: Int, y: Int) = onShow()
        override fun ContextMenu(x: Int, y: Int) {}
        override fun Scroll(delta: Int, orientation: String) {}

        private fun props(): Map<String, Variant<*>> = mapOf(
            "Category" to Variant("ApplicationStatus"),
            "Id" to Variant("bori"),
            "Title" to Variant(title),
            "Status" to Variant("Active"),
            "IconName" to Variant(""),
            "IconPixmap" to Variant(pixmaps, "a(iiay)"),
            "ToolTip" to Variant(ToolTip("", emptyList(), title, ""), "(sa(iiay)ss)"),
            "ItemIsMenu" to Variant(false),
            "Menu" to Variant(DBusPath(MENU_PATH))
        )

        @Suppress("UNCHECKED_CAST")
        override fun <A : Any?> Get(interfaceName: String, propertyName: String): A =
            props()[propertyName] as A
        override fun <A : Any?> Set(interfaceName: String, propertyName: String, value: A) {}
        override fun GetAll(interfaceName: String): Map<String, Variant<*>> = props()
    }

    private class Menu(
        private val title: String,
        private val onShow: () -> Unit,
        private val onQuit: () -> Unit
    ) : DbusMenu, Properties {
        override fun getObjectPath() = MENU_PATH

        private fun itemProps(id: Int): Map<String, Variant<*>> = when (id) {
            ID_SHOW -> mapOf("label" to Variant("Show $title"))
            2 -> mapOf("type" to Variant("separator"))
            ID_QUIT -> mapOf("label" to Variant("Quit"))
            else -> mapOf("children-display" to Variant("submenu"))
        }

        private fun layout(id: Int): Layout = if (id == 0) {
            Layout(0, itemProps(0), listOf(ID_SHOW, 2, ID_QUIT).map { Variant(layout(it), "(ia{sv}av)") })
        } else {
            Layout(id, itemProps(id), emptyList())
        }

        override fun GetLayout(parentId: Int, recursionDepth: Int, propertyNames: List<String>) =
            LayoutTuple(UInt32(1), layout(parentId))
        override fun GetGroupProperties(ids: List<Int>, propertyNames: List<String>) =
            (ids.ifEmpty { listOf(0, ID_SHOW, 2, ID_QUIT) }).map { ItemProps(it, itemProps(it)) }
        override fun GetProperty(id: Int, name: String): Variant<*> = itemProps(id)[name] ?: Variant("")
        override fun Event(id: Int, eventId: String, data: Variant<*>, timestamp: UInt32) {
            if (eventId != "clicked") return
            when (id) {
                ID_SHOW -> onShow()
                ID_QUIT -> onQuit()
            }
        }
        override fun EventGroup(events: List<MenuEvent>): List<Int> {
            events.forEach { Event(it.id, it.eventId, it.data, it.timestamp) }
            return emptyList()
        }
        override fun AboutToShow(id: Int) = false
        override fun AboutToShowGroup(ids: List<Int>) = AboutToShowTuple<List<Int>, List<Int>>(emptyList(), emptyList())

        private fun props(): Map<String, Variant<*>> = mapOf(
            "Version" to Variant(UInt32(3)),
            "TextDirection" to Variant("ltr"),
            "Status" to Variant("normal"),
            "IconThemePath" to Variant(emptyList<String>(), "as")
        )

        @Suppress("UNCHECKED_CAST")
        override fun <A : Any?> Get(interfaceName: String, propertyName: String): A =
            props()[propertyName] as A
        override fun <A : Any?> Set(interfaceName: String, propertyName: String, value: A) {}
        override fun GetAll(interfaceName: String): Map<String, Variant<*>> = props()
    }
}
