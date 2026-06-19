import SwiftRs
import Tauri
import UIKit
import WebKit

class PreviewOptions: Decodable {
  let windowed: Bool?
  let cameraDirection: String?
}

class NativecameraPlugin: Plugin {
  // NOT TESTED YET - iOS Camera implementation needs to be tested
  
  @objc public func startPreview(_ invoke: Invoke) throws {
    // NOT TESTED YET
    let args = try invoke.parseArgs(PreviewOptions.self)
    // TODO: Implement iOS camera preview logic using AVFoundation
    invoke.reject("iOS startPreview not implemented/tested yet")
  }

  @objc public func stopPreview(_ invoke: Invoke) throws {
    // NOT TESTED YET
    // TODO: Implement iOS camera stop logic
    invoke.reject("iOS stopPreview not implemented/tested yet")
  }

  @objc public func takePicture(_ invoke: Invoke) throws {
    // NOT TESTED YET
    // TODO: Implement iOS picture taking logic
    invoke.reject("iOS takePicture not implemented/tested yet")
  }
}

@_cdecl("init_plugin_nativecamera")
func initPlugin() -> Plugin {
  return NativecameraPlugin()
}
