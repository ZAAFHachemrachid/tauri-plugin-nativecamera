# Tauri Plugin nativecamera

Native camera access for [Tauri 2](https://v2.tauri.app/) apps — live preview and photo capture on iOS and Android.

## Install

Add the Rust plugin to your `src-tauri/Cargo.toml`:

```toml
[dependencies]
tauri-plugin-nativecamera = "0.1"
```

Add the JavaScript bindings to your frontend:

```sh
npm install tauri-plugin-nativecamera-api
# or: pnpm add / yarn add / bun add
```

## Setup

Register the plugin in your Tauri app builder (`src-tauri/src/lib.rs`):

```rust
fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_nativecamera::init())
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
```

Grant the plugin permissions in `src-tauri/capabilities/default.json`:

```json
{
  "permissions": [
    "nativecamera:default"
  ]
}
```

## Usage

From your frontend:

```typescript
import {
  startPreview,
  stopPreview,
  takePicture,
} from 'tauri-plugin-nativecamera-api';

// Start the live camera preview
await startPreview({
  windowed: true,          // default: true
  cameraDirection: 'back', // 'front' | 'back' — default: 'back'
});

// Capture a photo — returns a base64-encoded image string, or null
const photo = await takePicture();
if (photo) {
  img.src = `data:image/jpeg;base64,${photo}`;
}

// Stop the preview when done
await stopPreview();
```

## API

| Function | Signature | Description |
|----------|-----------|-------------|
| `startPreview` | `(options?: PreviewOptions) => Promise<void>` | Start the live camera preview. |
| `stopPreview` | `() => Promise<void>` | Stop the camera preview. |
| `takePicture` | `() => Promise<string \| null>` | Capture a photo; resolves to a base64 string or `null`. |

```typescript
interface PreviewOptions {
  windowed?: boolean;                  // default: true
  cameraDirection?: 'front' | 'back';  // default: 'back'
}
```

## Platform support

| Platform | Supported |
|----------|-----------|
| iOS      | ✅ |
| Android  | ✅ |
| Desktop  | — (no-op stub) |

## License

MIT
