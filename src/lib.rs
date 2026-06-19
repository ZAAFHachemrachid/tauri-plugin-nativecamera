use tauri::{
  plugin::{Builder, TauriPlugin},
  Manager, Runtime,
};

pub use models::*;

#[cfg(desktop)]
mod desktop;
#[cfg(mobile)]
mod mobile;

mod commands;
mod error;
mod models;

pub use error::{Error, Result};

#[cfg(desktop)]
use desktop::Nativecamera;
#[cfg(mobile)]
use mobile::Nativecamera;

/// Extensions to [`tauri::App`], [`tauri::AppHandle`] and [`tauri::Window`] to access the nativecamera APIs.
pub trait NativecameraExt<R: Runtime> {
  fn nativecamera(&self) -> &Nativecamera<R>;
}

impl<R: Runtime, T: Manager<R>> crate::NativecameraExt<R> for T {
  fn nativecamera(&self) -> &Nativecamera<R> {
    self.state::<Nativecamera<R>>().inner()
  }
}

/// Initializes the plugin.
pub fn init<R: Runtime>() -> TauriPlugin<R> {
  Builder::new("nativecamera")
    .invoke_handler(tauri::generate_handler![commands::start_preview, commands::stop_preview, commands::take_picture])
    .setup(|app, api| {
      #[cfg(mobile)]
      let nativecamera = mobile::init(app, api)?;
      #[cfg(desktop)]
      let nativecamera = desktop::init(app, api)?;
      app.manage(nativecamera);
      Ok(())
    })
    .build()
}
