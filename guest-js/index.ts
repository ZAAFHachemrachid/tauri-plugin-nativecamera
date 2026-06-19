import { invoke } from '@tauri-apps/api/core'

export interface PreviewOptions {
  windowed?: boolean;
  cameraDirection?: 'front' | 'back';
}

export async function startPreview(options?: PreviewOptions): Promise<void> {
  await invoke('plugin:nativecamera|start_preview', {
    payload: {
      windowed: options?.windowed ?? true,
      cameraDirection: options?.cameraDirection ?? 'back',
    },
  });
}

export async function stopPreview(): Promise<void> {
  await invoke('plugin:nativecamera|stop_preview');
}

export async function takePicture(): Promise<string | null> {
  const result = await invoke<{ photo?: string }>('plugin:nativecamera|take_picture');
  return result.photo || null;
}

export async function requestPermissions(): Promise<any> {
  return await invoke('plugin:nativecamera|requestPermissions');
}

export async function checkPermissions(): Promise<any> {
  return await invoke('plugin:nativecamera|checkPermissions');
}
