use serde::de::DeserializeOwned;
use tauri::{
  plugin::{PluginApi, PluginHandle},
  AppHandle, Runtime,
};

use crate::models::*;

#[cfg(target_os = "ios")]
tauri::ios_plugin_binding!(init_plugin_nativecamera);

// initializes the Kotlin or Swift plugin classes
pub fn init<R: Runtime, C: DeserializeOwned>(
  _app: &AppHandle<R>,
  api: PluginApi<R, C>,
) -> crate::Result<Nativecamera<R>> {
  #[cfg(target_os = "android")]
  let handle = api.register_android_plugin("com.plugin.nativecamera", "NativeCameraPlugin")?;
  #[cfg(target_os = "ios")]
  let handle = api.register_ios_plugin(init_plugin_nativecamera)?;
  Ok(Nativecamera(handle))
}

/// Access to the nativecamera APIs.
pub struct Nativecamera<R: Runtime>(PluginHandle<R>);

impl<R: Runtime> Nativecamera<R> {
  pub fn start_preview(&self, payload: PreviewOptions) -> crate::Result<()> {
    self
      .0
      .run_mobile_plugin("startPreview", payload)
      .map_err(Into::into)
  }

  pub fn stop_preview(&self) -> crate::Result<()> {
    self
      .0
      .run_mobile_plugin("stopPreview", ())
      .map_err(Into::into)
  }

  pub fn take_picture(&self) -> crate::Result<PictureResponse> {
    self
      .0
      .run_mobile_plugin("takePicture", ())
      .map_err(Into::into)
  }
}
