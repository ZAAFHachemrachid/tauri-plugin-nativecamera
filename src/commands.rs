use tauri::{AppHandle, command, Runtime};

use crate::models::*;
use crate::Result;
use crate::NativecameraExt;

#[command]
pub(crate) async fn start_preview<R: Runtime>(
    app: AppHandle<R>,
    payload: PreviewOptions,
) -> Result<()> {
    app.nativecamera().start_preview(payload)
}

#[command]
pub(crate) async fn stop_preview<R: Runtime>(
    app: AppHandle<R>,
) -> Result<()> {
    app.nativecamera().stop_preview()
}

#[command]
pub(crate) async fn take_picture<R: Runtime>(
    app: AppHandle<R>,
) -> Result<PictureResponse> {
    app.nativecamera().take_picture()
}
