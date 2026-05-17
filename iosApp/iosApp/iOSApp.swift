import SwiftUI
import shared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}

struct ComposeView: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let controller = MainKt.MainViewController()
        return controller.view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}
}
