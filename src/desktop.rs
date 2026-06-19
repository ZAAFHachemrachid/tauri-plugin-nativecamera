use serde::de::DeserializeOwned;
use tauri::{plugin::PluginApi, AppHandle, Runtime};

use crate::models::*;

pub fn init<R: Runtime, C: DeserializeOwned>(
  app: &AppHandle<R>,
  _api: PluginApi<R, C>,
) -> crate::Result<Nativecamera<R>> {
  Ok(Nativecamera(app.clone()))
}

/// Access to the nativecamera APIs.
pub struct Nativecamera<R: Runtime>(AppHandle<R>);

impl<R: Runtime> Nativecamera<R> {
  pub fn start_preview(&self, _payload: PreviewOptions) -> crate::Result<()> {
    Ok(())
  }

  pub fn stop_preview(&self) -> crate::Result<()> {
    Ok(())
  }

  pub fn take_picture(&self) -> crate::Result<PictureResponse> {
    Ok(PictureResponse { photo: None })
  }
}
