export interface UploadSuccessResponse {
  bucket_url: string;
  deal_id: string;
}

export class UploadError extends Error {
  status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = "UploadError";
    this.status = status;
  }
}

interface UploadZipOptions {
  file: File;
  uploadedByUserId?: string;
  onProgress: (progress: number) => void;
}

// Maps gateway error contracts (see MANUAL_TESTS.md) to user-facing messages.
function messageForStatus(status: number): string {
  switch (status) {
    case 400:
      return "Invalid request. Please try again.";
    case 413:
      return "File exceeds 500 MB limit.";
    case 415:
      // TC-FE-05: exact wording from 1.1 Story Task spec.
      return "Unsupported file types detected";
    case 422:
      // TC-FE-06: exact wording from 1.1 Story Task spec.
      return "Password-protected archives not allowed";
    case 401:
    case 502:
      return "Classification service unavailable. Please retry shortly.";
    default:
      return "Upload failed. Please try again.";
  }
}

/**
 * Uploads a ZIP archive to the gateway as a single multipart POST.
 *
 * Contract (see gateway UploadController):
 *   - Form field: `file` (the ZIP, MultipartFile)
 *   - Header:     `X-User-Id` (optional; defaults to "anonymous" on the gateway)
 *
 * Progress is reported via XMLHttpRequest's upload progress events because
 * fetch() does not expose upload progress in the browser today.
 */
export function uploadZipFile({
  file,
  uploadedByUserId = "current-user",
  onProgress,
}: UploadZipOptions): Promise<UploadSuccessResponse> {
  return new Promise((resolve, reject) => {
    const formData = new FormData();
    formData.append("file", file, file.name);

    const xhr = new XMLHttpRequest();
    // Relative URL: in Vite dev the /uploads proxy (vite.config.ts) forwards to
    // the gateway on :8080; in the Docker image nginx.conf reverse-proxies
    // /uploads to the gateway-service container. This keeps the frontend
    // portable across environments without baking in a host:port at build time.
    xhr.open("POST", "/uploads");
    xhr.setRequestHeader("X-User-Id", uploadedByUserId);

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        onProgress(Math.round((event.loaded / event.total) * 100));
      }
    };

    xhr.onload = () => {
      if (xhr.status === 201) {
        try {
          resolve(JSON.parse(xhr.responseText));
        } catch {
          reject(new UploadError("Malformed gateway response.", 201));
        }
        return;
      }
      reject(new UploadError(messageForStatus(xhr.status), xhr.status));
    };

    xhr.onerror = () =>
      reject(new UploadError("Network error. Please try again."));
    xhr.onabort = () => reject(new UploadError("Upload was aborted."));

    xhr.send(formData);
  });
}
