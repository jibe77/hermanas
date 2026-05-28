import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastService } from '@common/services';
import {
    ChartsService,
    HermanasUser,
    UserCreate,
    UserUpdate,
} from '@modules/notification/services/charts.service';
import { UserService } from '@modules/auth/services';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'sb-charts',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './charts.component.html',
    styleUrls: ['charts.component.scss'],
    standalone: false
})
export class ChartsComponent implements OnInit, OnDestroy {
    me?: HermanasUser;
    isAdmin = false;

    meEmail = '';
    meNotifications = false;
    mePassword = '';
    meDirty = false;
    meSaving = false;

    users: HermanasUser[] = [];
    loadingUsers = false;

    showCreate = false;
    newLogin = '';
    newPassword = '';
    newEmail = '';
    newRole = 'USER';
    newNotifications = false;
    creating = false;

    editingLogin?: string;
    editEmail = '';
    editRole = 'USER';
    editNotifications = false;
    editPassword = '';
    editSaving = false;

    private destroy$ = new Subject<void>();

    constructor(
        private _chartsService: ChartsService,
        private _userService: UserService,
        private _toastService: ToastService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        const current = this._userService.getCurrentUser();
        this.isAdmin = (current.roles ?? []).includes('ROLE_ADMIN');
        this.loadMe();
        if (this.isAdmin) {
            this.loadUsers();
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    private loadMe(): void {
        this._chartsService
            .me()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: u => {
                    this.me = u;
                    this.meEmail = u.email ?? '';
                    this.meNotifications = u.notificationsEnabled;
                    this.mePassword = '';
                    this.meDirty = false;
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => this.showError(err, 'Cannot load profile'),
            });
    }

    onMeFieldChange(): void {
        if (!this.me) {
            return;
        }
        this.meDirty =
            this.meEmail !== (this.me.email ?? '') ||
            this.meNotifications !== this.me.notificationsEnabled ||
            this.mePassword.length > 0;
    }

    saveMe(): void {
        if (!this.me || !this.meDirty || this.meSaving) {
            return;
        }
        const payload: UserUpdate = {
            email: this.meEmail,
            notificationsEnabled: this.meNotifications,
        };
        if (this.mePassword.length > 0) {
            payload.password = this.mePassword;
        }
        this.meSaving = true;
        this._chartsService
            .updateMe(payload)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: u => {
                    this.me = u;
                    this.mePassword = '';
                    this.meSaving = false;
                    this.meDirty = false;
                    this._toastService.success('Profil mis à jour', 'Users');
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.meSaving = false;
                    this.showError(err, 'Cannot update profile');
                },
            });
    }

    private loadUsers(): void {
        this.loadingUsers = true;
        this._chartsService
            .list()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: list => {
                    this.users = list;
                    this.loadingUsers = false;
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.loadingUsers = false;
                    this.showError(err, 'Cannot list users');
                },
            });
    }

    toggleCreate(): void {
        this.showCreate = !this.showCreate;
        if (this.showCreate) {
            this.newLogin = '';
            this.newPassword = '';
            this.newEmail = '';
            this.newRole = 'USER';
            this.newNotifications = false;
        }
    }

    submitCreate(): void {
        if (this.creating) return;
        if (!this.newLogin.trim() || !this.newPassword) {
            this._toastService.error('Login and password are required', 'Users');
            return;
        }
        const payload: UserCreate = {
            login: this.newLogin.trim(),
            password: this.newPassword,
            email: this.newEmail.trim() || null,
            role: this.newRole,
            notificationsEnabled: this.newNotifications,
        };
        this.creating = true;
        this._chartsService
            .create(payload)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: u => {
                    this.users = [...this.users, u];
                    this.creating = false;
                    this.showCreate = false;
                    this._toastService.success(`Utilisateur "${u.login}" créé`, 'Users');
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.creating = false;
                    this.showError(err, 'Cannot create user');
                },
            });
    }

    startEdit(user: HermanasUser): void {
        this.editingLogin = user.login;
        this.editEmail = user.email ?? '';
        this.editRole = user.role;
        this.editNotifications = user.notificationsEnabled;
        this.editPassword = '';
    }

    cancelEdit(): void {
        this.editingLogin = undefined;
    }

    submitEdit(): void {
        if (!this.editingLogin || this.editSaving) return;
        const payload: UserUpdate = {
            email: this.editEmail,
            notificationsEnabled: this.editNotifications,
            role: this.editRole,
        };
        if (this.editPassword.length > 0) {
            payload.password = this.editPassword;
        }
        this.editSaving = true;
        this._chartsService
            .update(this.editingLogin, payload)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: u => {
                    this.users = this.users.map(x => (x.login === u.login ? u : x));
                    this.editSaving = false;
                    this.editingLogin = undefined;
                    this._toastService.success(`Utilisateur "${u.login}" mis à jour`, 'Users');
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.editSaving = false;
                    this.showError(err, 'Cannot update user');
                },
            });
    }

    approveUser(user: HermanasUser): void {
        this._chartsService
            .update(user.login, { role: 'USER' })
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: u => {
                    this.users = this.users.map(x => (x.login === u.login ? u : x));
                    this._toastService.success(`Compte "${u.login}" validé`, 'Users');
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => this.showError(err, 'Cannot approve user'),
            });
    }

    roleBadgeClass(role: string): string {
        switch (role) {
            case 'ADMIN':
                return 'bg-danger';
            case 'NOT_VALIDATED_YET':
                return 'bg-warning text-dark';
            default:
                return 'bg-secondary';
        }
    }

    deleteUser(user: HermanasUser): void {
        if (!confirm(`Supprimer l'utilisateur "${user.login}" ?`)) {
            return;
        }
        this._chartsService
            .delete(user.login)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: () => {
                    this.users = this.users.filter(u => u.login !== user.login);
                    this._toastService.success(`Utilisateur "${user.login}" supprimé`, 'Users');
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => this.showError(err, 'Cannot delete user'),
            });
    }

    private showError(err: HttpErrorResponse, defaultMessage: string): void {
        const msg = err.error?.error || err.error?.message || err.message || defaultMessage;
        this._toastService.error(msg, `Users — HTTP ${err.status}`);
        this.cdr.detectChanges();
    }
}
