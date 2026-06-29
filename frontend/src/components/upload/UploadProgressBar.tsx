interface UploadProgressBarProps {
  progress: number;
  isUploading: boolean;
}

export default function UploadProgressBar({
  progress,
  isUploading,
}: UploadProgressBarProps) {
  if (!isUploading && progress === 0) return null;

  return (
    <div className="w-full">
      <div className="mb-2 flex justify-between text-sm text-gray-700">
        <span>{isUploading ? "Uploading..." : "Upload progress"}</span>
        <span>{progress}%</span>
      </div>

      <div
        className="h-3 w-full overflow-hidden rounded-full bg-gray-200"
        role="progressbar"
        aria-valuenow={progress}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label="Upload progress"
      >
        <div
          className="h-full rounded-full bg-blue-600 motion-safe:transition-all motion-safe:duration-300"
          style={{ width: `${progress}%` }}
        />
      </div>
    </div>
  );
}
