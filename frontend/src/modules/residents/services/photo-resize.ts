const TARGET_BYTES = 300 * 1024;
const ACCEPTABLE_BYTES = 500 * 1024;
const MAX_DIMENSION = 1280;
const QUALITY_START = 0.85;
const QUALITY_MIN = 0.55;
const QUALITY_STEP = 0.05;

export async function resizePhotoForUpload(file: File): Promise<File> {
    if (!file.type.startsWith('image/')) {
        return file;
    }
    if (file.size <= TARGET_BYTES && file.type === 'image/jpeg') {
        return file;
    }

    const bitmap = await loadBitmap(file);
    const { width, height } = scaledDimensions(bitmap.width, bitmap.height);

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
        return file;
    }
    ctx.drawImage(bitmap, 0, 0, width, height);
    if ('close' in bitmap) {
        (bitmap as ImageBitmap).close();
    }

    let best: Blob | null = null;
    for (let q = QUALITY_START; q >= QUALITY_MIN - 1e-6; q -= QUALITY_STEP) {
        const blob = await canvasToJpeg(canvas, q);
        if (!blob) continue;
        best = blob;
        if (blob.size <= TARGET_BYTES) {
            break;
        }
    }
    if (!best) {
        return file;
    }
    if (best.size > ACCEPTABLE_BYTES && best.size >= file.size) {
        return file;
    }

    const baseName = file.name.replace(/\.[^.]+$/, '') || 'photo';
    return new File([best], `${baseName}.jpg`, {
        type: 'image/jpeg',
        lastModified: Date.now(),
    });
}

function scaledDimensions(w: number, h: number): { width: number; height: number } {
    const largest = Math.max(w, h);
    if (largest <= MAX_DIMENSION) {
        return { width: w, height: h };
    }
    const ratio = MAX_DIMENSION / largest;
    return {
        width: Math.round(w * ratio),
        height: Math.round(h * ratio),
    };
}

async function loadBitmap(file: File): Promise<ImageBitmap | HTMLImageElement> {
    if (typeof createImageBitmap === 'function') {
        try {
            return await createImageBitmap(file);
        } catch {
            /* fallback to <img> */
        }
    }
    return await loadViaImgElement(file);
}

function loadViaImgElement(file: File): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
        const url = URL.createObjectURL(file);
        const img = new Image();
        img.onload = () => {
            URL.revokeObjectURL(url);
            resolve(img);
        };
        img.onerror = () => {
            URL.revokeObjectURL(url);
            reject(new Error('Cannot decode image'));
        };
        img.src = url;
    });
}

function canvasToJpeg(canvas: HTMLCanvasElement, quality: number): Promise<Blob | null> {
    return new Promise(resolve => {
        canvas.toBlob(b => resolve(b), 'image/jpeg', quality);
    });
}
