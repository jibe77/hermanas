import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastService } from '@common/services';
import { PhotoEntry, PhotoListing, PhotosService } from '@modules/camera/services/photos.service';

interface Crumb {
    label: string;
    /** Relative path this crumb navigates to. '' for the root. */
    path: string;
}

interface PhotoFile extends PhotoEntry {
    url: string;
}

@Component({
    selector: 'sb-charts',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './charts.component.html',
    styleUrls: ['charts.component.scss'],
})
export class ChartsComponent implements OnInit {
    currentPath = '';
    crumbs: Crumb[] = [{ label: 'photos', path: '' }];
    directories: PhotoEntry[] = [];
    files: PhotoFile[] = [];
    loading = false;
    selected?: PhotoFile;

    constructor(
        private photos: PhotosService,
        private toast: ToastService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.load('');
    }

    enterDir(dir: PhotoEntry): void {
        const next = this.currentPath ? `${this.currentPath}/${dir.name}` : dir.name;
        this.load(next);
    }

    goTo(path: string): void {
        if (path === this.currentPath) {
            return;
        }
        this.load(path);
    }

    selectFile(file: PhotoFile): void {
        this.selected = file;
        this.cdr.markForCheck();
    }

    clearSelection(): void {
        this.selected = undefined;
    }

    private load(path: string): void {
        this.loading = true;
        this.clearSelection();
        this.photos.list(path).subscribe({
            next: (listing: PhotoListing) => {
                this.currentPath = listing.path;
                this.directories = listing.directories;
                this.files = listing.files.map(f => ({
                    ...f,
                    url: this.photos.fileUrl(listing.path ? `${listing.path}/${f.name}` : f.name),
                }));
                this.crumbs = this.buildCrumbs(listing.path);
                this.loading = false;
                this.cdr.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
                this.loading = false;
                const msg = err.error?.error || err.message || 'Cannot list photos';
                this.toast.error(msg, `Camera — HTTP ${err.status}`);
                this.cdr.markForCheck();
            },
        });
    }

    private buildCrumbs(path: string): Crumb[] {
        const result: Crumb[] = [{ label: 'photos', path: '' }];
        if (!path) {
            return result;
        }
        const parts = path.split('/').filter(p => p.length > 0);
        let acc = '';
        for (const part of parts) {
            acc = acc ? `${acc}/${part}` : part;
            result.push({ label: part, path: acc });
        }
        return result;
    }
}
