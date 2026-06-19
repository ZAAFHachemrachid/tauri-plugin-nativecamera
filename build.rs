const COMMANDS: &[&str] = &["start_preview", "stop_preview", "take_picture"];

fn main() {
  tauri_plugin::Builder::new(COMMANDS)
    .android_path("android")
    .ios_path("ios")
    .build();
}
