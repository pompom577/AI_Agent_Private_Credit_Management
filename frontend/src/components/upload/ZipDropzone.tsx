import { useCallback, useState } from "react";
import { useDropzone } from "react-dropzone";
import ErrorBanner from "./ErrorBanner";
import UploadProgressBar from "./UploadProgressBar";
import { uploadZipFile } from "../../services/apiClient";

const MAX_FILE_SIZE = 500 * 1024 * 1024; // 500MB

function validateZipFile(file: File): string | null {
  const isZip = file.name.toLowerCase().endsWith(".zip");

  if (!isZip) {
    return "Unsupported file type";
  }

  if (file.size > MAX_FILE_SIZE) {
    return "File exceeds 500 MB";
  }

  return null;
}

export default function ZipDropzone() {
  const [progress, setProgress] = useState(0);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const onDrop = useCallback(async (acceptedFiles: File[]) => {
    const file = acceptedFiles[0];

    setErrorMessage(null);
    setSuccessMessage(null);
    setProgress(0);

    if (!file) return;

    const validationError = validateZipFile(file);

    if (validationError) {
      setErrorMessage(validationError);
      return;
    }

    try {
      setIsUploading(true);

      const result = await uploadZipFile({
        file,
        onProgress: setProgress,
      });

      setProgress(100);
      setSuccessMessage(`Upload complete. Deal ID: ${result.deal_id}`);
      setTimeout(() => {
        setProgress(0);
        setSuccessMessage(null);
      }, 5000);
    } catch (error) {
      setProgress(0);

      if (error instanceof Error) {
        setErrorMessage(error.message);
      } else {
        setErrorMessage("Upload failed. Please try again.");
      }
    } finally {
      setIsUploading(false);
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    // TC-FE-02: surface the "Unsupported file type" message when the dropzone
    // filters out a non-zip file (otherwise react-dropzone silently drops it).
    onDropRejected: () => {
      setErrorMessage("Unsupported file type");
      setSuccessMessage(null);
      setProgress(0);
    },
    multiple: false,
    disabled: isUploading,
    accept: {
      "application/zip": [".zip"],
    },
  });

  return (
    <div className="space-y-5">
      <div
        {...getRootProps()}
        aria-label="ZIP file upload dropzone. Drag and drop or click to select a ZIP archive."
        aria-busy={isUploading}
        className={`flex min-h-[280px] flex-col items-center justify-center rounded-xl border-2 border-dashed p-10 text-center motion-safe:transition-all focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-offset-2 ${
          isDragActive
            ? "border-indigo-400 bg-indigo-50"
            : "hover:border-indigo-300 hover:bg-indigo-50/30"
        } ${isUploading ? "cursor-not-allowed opacity-70" : "cursor-pointer"}`}
        style={{
          borderColor: isDragActive ? undefined : "var(--color-border)",
          backgroundColor: isDragActive ? undefined : "transparent",
        }}
      >
        <input {...getInputProps()} />

        {isUploading ? (
          <>
            <svg
              aria-hidden="true"
              className="mb-5 h-12 w-12 animate-spin"
              style={{ color: "var(--color-accent)" }}
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
              />
            </svg>
            <p
              className="text-lg font-semibold"
              style={{
                fontFamily: "var(--font-display)",
                color: "var(--color-text-primary)",
              }}
            >
              Uploading…
            </p>
          </>
        ) : (
          <>
            {/* SVG upload icon (replaces emoji — no-emoji-icons rule) */}
            <div
              className="mb-5 flex h-16 w-16 items-center justify-center rounded-2xl"
              style={{
                backgroundColor: "var(--color-accent-light)",
                color: "var(--color-accent)",
              }}
              aria-hidden="true"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-8 w-8"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={1.75}
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5"
                />
              </svg>
            </div>

            <p
              className="text-xl font-bold"
              style={{
                fontFamily: "var(--font-display)",
                color: "var(--color-text-primary)",
              }}
            >
              {isDragActive ? "Release to upload" : "Drag & drop your ZIP here"}
            </p>
            <p
              className="mt-2 text-base"
              style={{ color: "var(--color-text-muted)" }}
            >
              or{" "}
              <span
                className="font-medium underline underline-offset-2"
                style={{ color: "var(--color-accent)" }}
              >
                click to browse
              </span>
            </p>
            <p
              className="mt-5 text-xs"
              style={{ color: "var(--color-text-muted)" }}
            >
              Max 500 MB · .zip only
            </p>
          </>
        )}
      </div>

      <UploadProgressBar progress={progress} isUploading={isUploading} />

      <ErrorBanner message={errorMessage} />

      {successMessage && (
        <div
          role="status"
          aria-live="polite"
          className="flex items-start gap-2 rounded-lg border px-4 py-3 text-sm"
          style={{
            backgroundColor: "var(--color-success-bg)",
            borderColor: "var(--color-success-border)",
            color: "var(--color-success)",
          }}
        >
          <span aria-hidden="true" className="mt-0.5 shrink-0">
            ✓
          </span>
          <span>{successMessage}</span>
        </div>
      )}
    </div>
  );
}
