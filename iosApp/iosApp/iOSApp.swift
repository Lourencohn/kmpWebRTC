import SwiftUI
import AVFoundation
import WebRTC

@main
struct iOSApp: App {
    init() {
        configureAudioSession()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea()
        }
    }
}

private func configureAudioSession() {
    let session = RTCAudioSession.sharedInstance()
    session.lockForConfiguration()
    defer { session.unlockForConfiguration() }
    do {
        try session.setCategory(
            AVAudioSession.Category.playAndRecord.rawValue,
            with: [.defaultToSpeaker, .allowBluetooth, .allowBluetoothA2DP]
        )
        try session.setMode(AVAudioSession.Mode.voiceChat.rawValue)
        try session.overrideOutputAudioPort(.speaker)
        try session.setActive(true)
    } catch {
        NSLog("[TrovataCast] RTCAudioSession configuration failed: \(error)")
    }
}
