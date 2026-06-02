import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
    inject,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastService } from '@common/services';
import { UserService } from '@modules/auth/services';
import { AuthState } from '@modules/auth/models';
import { Resident, ResidentRequest, ResidentsService } from '@modules/residents/services';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';

interface FormState {
    id: number | null;
    name: string;
    breed: string;
    birthDate: string;
    arrivalDate: string;
    deathDate: string;
    comments: string;
}

@Component({
    selector: 'sb-residents',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './residents.component.html',
    styleUrls: ['residents.component.scss'],
    imports: [
        LayoutDashboardComponent,
        DashboardHeadComponent,
        CardComponent,
        FaIconComponent,
        FormsModule,
        DatePipe,
    ],
})
export class ResidentsComponent implements OnInit {
    private service = inject(ResidentsService);
    private userService = inject(UserService);
    private toast = inject(ToastService);
    private cdr = inject(ChangeDetectorRef);

    residents: Resident[] = [];
    loading = false;
    isAuthenticated = false;

    showModal = false;
    saving = false;
    form: FormState = this.emptyForm();
    photoUploading = false;

    ngOnInit(): void {
        this.isAuthenticated = this.userService.getCurrentUser().authState === AuthState.SignedIn;
        this.userService.user$.subscribe(u => {
            this.isAuthenticated = u.authState === AuthState.SignedIn;
            this.cdr.markForCheck();
        });
        this.load();
    }

    private emptyForm(): FormState {
        const today = new Date().toISOString().slice(0, 10);
        return {
            id: null,
            name: '',
            breed: '',
            birthDate: '',
            arrivalDate: today,
            deathDate: '',
            comments: '',
        };
    }

    load(): void {
        this.loading = true;
        this.service.list().subscribe({
            next: list => {
                this.residents = list;
                this.loading = false;
                this.cdr.markForCheck();
            },
            error: (err: HttpErrorResponse) => {
                this.loading = false;
                this.toast.error(
                    err.error?.message || err.message || 'Cannot load residents',
                    `Pensionnaires — HTTP ${err.status}`
                );
                this.cdr.markForCheck();
            },
        });
    }

    isAlive(r: Resident): boolean {
        return !r.deathDate;
    }

    openCreate(): void {
        this.form = this.emptyForm();
        this.showModal = true;
    }

    openEdit(r: Resident): void {
        this.form = {
            id: r.id,
            name: r.name,
            breed: r.breed ?? '',
            birthDate: r.birthDate ?? '',
            arrivalDate: r.arrivalDate ?? '',
            deathDate: r.deathDate ?? '',
            comments: r.comments ?? '',
        };
        this.showModal = true;
    }

    closeModal(): void {
        if (this.saving) return;
        this.showModal = false;
    }

    save(): void {
        if (this.saving) return;
        if (!this.form.name || this.form.name.trim().length === 0) {
            this.toast.error('Le nom est requis', 'Pensionnaires');
            return;
        }
        this.saving = true;
        const payload: ResidentRequest = {
            name: this.form.name.trim(),
            breed: this.form.breed.trim() || null,
            birthDate: this.form.birthDate || null,
            arrivalDate: this.form.arrivalDate || null,
            deathDate: this.form.deathDate || null,
            comments: this.form.comments.trim() || null,
        };
        const call$ =
            this.form.id === null
                ? this.service.create(payload)
                : this.service.update(this.form.id, payload);
        call$.subscribe({
            next: () => {
                this.saving = false;
                this.showModal = false;
                this.toast.success(
                    this.form.id === null ? 'Pensionnaire ajouté' : 'Pensionnaire mis à jour',
                    'Pensionnaires'
                );
                this.load();
            },
            error: (err: HttpErrorResponse) => {
                this.saving = false;
                this.toast.error(
                    err.error?.message || err.message || 'Save failed',
                    `Pensionnaires — HTTP ${err.status}`
                );
                this.cdr.markForCheck();
            },
        });
    }

    remove(r: Resident): void {
        const ok = window.confirm(
            `Supprimer définitivement ${r.name} ? Cette action est irréversible.`
        );
        if (!ok) return;
        this.service.remove(r.id).subscribe({
            next: () => {
                this.toast.success(`${r.name} supprimé`, 'Pensionnaires');
                this.load();
            },
            error: (err: HttpErrorResponse) =>
                this.toast.error(
                    err.error?.message || err.message || 'Delete failed',
                    `Pensionnaires — HTTP ${err.status}`
                ),
        });
    }

    onPhotoSelected(r: Resident, event: Event): void {
        const input = event.target as HTMLInputElement;
        if (!input.files || input.files.length === 0) return;
        const file = input.files[0];
        this.photoUploading = true;
        this.service.uploadPhoto(r.id, file).subscribe({
            next: () => {
                this.photoUploading = false;
                this.toast.success(`Photo mise à jour pour ${r.name}`, 'Pensionnaires');
                input.value = '';
                this.load();
            },
            error: (err: HttpErrorResponse) => {
                this.photoUploading = false;
                input.value = '';
                this.toast.error(
                    err.error?.message || err.message || 'Upload failed',
                    `Pensionnaires — HTTP ${err.status}`
                );
                this.cdr.markForCheck();
            },
        });
    }

    deletePhoto(r: Resident): void {
        const ok = window.confirm(`Supprimer la photo de ${r.name} ?`);
        if (!ok) return;
        this.service.deletePhoto(r.id).subscribe({
            next: () => {
                this.toast.success(`Photo de ${r.name} supprimée`, 'Pensionnaires');
                this.load();
            },
            error: (err: HttpErrorResponse) =>
                this.toast.error(
                    err.error?.message || err.message || 'Delete failed',
                    `Pensionnaires — HTTP ${err.status}`
                ),
        });
    }

    photoSrc(r: Resident): string | null {
        if (!r.photoUrl) return null;
        return `${window.location.origin}${r.photoUrl}`;
    }
}
