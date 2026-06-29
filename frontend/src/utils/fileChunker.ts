export const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB

export interface FileChunk {
  index: number;
  totalChunks: number;
  blob: Blob;
  start: number;
  end: number;
}

export function createFileChunks(file: File, chunkSize: number = CHUNK_SIZE): FileChunk[] {
  const chunks: FileChunk[] = [];
  const totalChunks = Math.ceil(file.size / chunkSize);

  for (let index = 0; index < totalChunks; index++) {
    const start = index * chunkSize;
    const end = Math.min(start + chunkSize, file.size);

    chunks.push({
      index,
      totalChunks,
      blob: file.slice(start, end),
      start,
      end,
    });
  }

  return chunks;
}
