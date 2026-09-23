package com.apagon.rhythm.platform

// Desktop actual for WidgetRefresher — there's no home-screen widget system
// on Linux desktop, so this is a deliberate no-op (matches the interface's
// own doc comment: "iOS: no-op until WidgetKit support lands").
class DesktopWidgetRefresher : WidgetRefresher {
    override suspend fun refreshAll() {}
}
