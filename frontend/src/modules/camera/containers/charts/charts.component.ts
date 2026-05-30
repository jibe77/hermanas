import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
    inject,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastService } from '@common/services';
import { PhotoEntry, PhotoListing, PhotosService } from '@modules/camera/services/photos.service';
import { ConfigService } from '@modules/energy/services/config.service';
import { UserService } from '@modules/auth/services';
import { forkJoin } from 'rxjs';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms';

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
    imports: [
        LayoutDashboardComponent,
        DashboardHeadComponent,
        CardComponent,
        FaIconComponent,
        FormsModule,
    ],
})
export class ChartsComponent implements OnInit {
    private photos = inject(PhotosService);
    private configService = inject(ConfigService);
    private userService = inject(UserService);
    private toast = inject(ToastService);
    private cdr = inject(ChangeDetectorRef);

    currentPath = '';
    crumbs: Crumb[] = [{ label: 'photos', path: '' }];
    directories: PhotoEntry[] = [];
    files: PhotoFile[] = [];
    loading = false;
    selected?: PhotoFile;

    isAdmin = false;
    cameraBrightness = 60;
    cameraRotation = 180;
    cameraSaving = false;

    ngOnInit(): void {
        this.isAdmin = this.userService.isAdmin();
        if (this.isAdmin) {
            this.loadCameraSettings();
        }
        this.load('');
    }

    private loadCameraSettings(): void {
        this.configService.getAll().subscribe({
            next: cfg => {
                this.cameraBrightness = cfg.camera_settings.brightness;
                this.cameraRotation = cfg.camera_settings.rotation;
                this.cdr.markForCheck();
            },
            error: () => {
                /* keep defaults */
            },
        });
    }

    saveCameraSettings(): void {
        if (this.cameraSaving) return;
        this.cameraSaving = true;
        forkJoin({
            brightness: this.configService.setCameraBrightness(this.cameraBrightness),
            rotation: this.configService.setCameraRotation(this.cameraRotation),
        }).subscribe({
            next: () => {
                this.cameraSaving = false;
                this.toast.success('Réglages enregistrés (effet au prochain reboot).', 'Caméra');
                this.cdr.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
                this.cameraSaving = false;
                this.toast.error(
                    err.error?.message || err.message || 'Save failed',
                    `Caméra — HTTP ${err.status}`
                );
                this.cdr.markForCheck();
            },
        });
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
