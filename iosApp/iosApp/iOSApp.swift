import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    
    init() {
        KmpBackgrounding.shared.cancel()
        KmpBackgrounding.shared.registerBackgroundService()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
