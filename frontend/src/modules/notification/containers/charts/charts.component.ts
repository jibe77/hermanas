import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    inject,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { PushService, ToastService } from '@common/services';
import {
    ChartsService,
    HermanasUser,
    UserCreate,
    UserUpdate,
} from '@modules/notification/services/charts.service';
import { UserService } from '@modules/auth/services';
import { ConfigService } from '@modules/energy/services/config.service';
import { EmailTestService } from '@modules/system/services/email-test.service';
import { Observable, Subject, forkJoin } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { NgClass } from '@angular/common';

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
        ReactiveFormsModule,
        FormsModule,
        NgClass,
    ],
})
export class ChartsComponent implements OnInit, OnDestroy {
    private _chartsService = inject(ChartsService);
    private _userService = inject(UserService);
    private _toastService = inject(ToastService);
    private _pushService = inject(PushService);
    private _configService = inject(ConfigService);
    private _emailTestService = inject(EmailTestService);
    private cdr = inject(ChangeDetectorRef);

    pushSupported = false;
    pushSubscribed = false;
    pushBusy = false;

    me?: HermanasUser;
    isAdmin = false;

    meEmail = '';
    meNotifications = false;
    meLanguage: 'fr' | 'en' | 'ro' = 'fr';
    mePassword = '';
    mePasswordConfirm = '';
    meDirty = false;
    meSaving = false;

    users: HermanasUser[] = [];
    loadingUsers = false;

    showCreate = false;
    newLogin = '';
    newPassword = '';
    newPasswordConfirm = '';
    newEmail = '';
    newRole = 'USER';
    newNotifications = false;
    newLanguage: 'fr' | 'en' | 'ro' = 'fr';
    creating = false;

    editingLogin?: string;
    editEmail = '';
    editRole = 'USER';
    editNotifications = false;
    editLanguage: 'fr' | 'en' | 'ro' = 'fr';
    editPassword = '';
    editPasswordConfirm = '';
    editSaving = false;

    // Email config state (moved from System page)
    emailFrom = '';
    emailSaving = false;
    emailTestSending = false;
    mailHost = '';
    mailPort = 25;
    mailUsername = '';
    mailPasswordInput = '';
    mailPasswordSet = false;
    mailAuth = true;
    mailStartTls = true;
    private persistedMailHost = '';
    private persistedMailPort = 25;
    private persistedMailUsername = '';
    private persistedMailAuth = true;
    private persistedMailStartTls = true;

    private destroy$ = new Subject<void>();

    ngOnInit(): void {
        // Route admin detection through UserService.isAdmin() so demo mode
        // (synthetic ADMIN with roles ['ADMIN'], not 'ROLE_ADMIN') and a real
        // signed-in admin both unfold the User administration + Email panels.
        // Subscribe to user$ so the panels appear/disappear on login/logout
        // without a manual page reload.
        this._userService.user$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            const wasAdmin = this.isAdmin;
            this.isAdmin = this._userService.isAdmin();
            if (this.isAdmin && !wasAdmin) {
                this.loadUsers();
                this.loadEmailSettings();
            }
            this.cdr.detectChanges();
        });
        this.loadMe();
        this.refreshPushState();
    }

    private refreshPushState(): void {
        this.pushSupported = this._pushService.isEnabled();
        if (this.pushSupported) {
            this._pushService.isSubscribed().then(active => {
                this.pushSubscribed = active;
                this.cdr.detectChanges();
            });
        }
    }

    async togglePush(): Promise<void> {
        if (this.pushBusy) {
            return;
        }
        this.pushBusy = true;
        try {
            const ok = this.pushSubscribed
                ? await this._pushService.unsubscribe()
                : await this._pushService.subscribe();
            if (!ok) {
                this._toastService.error(
                    this.pushSubscribed
                        ? 'Désabonnement échoué'
                        : 'Abonnement aux notifications échoué',
                    'Notifications'
                );
            } else {
                this.pushSubscribed = !this.pushSubscribed;
                this._toastService.success(
                    this.pushSubscribed ? 'Notifications activées' : 'Notifications désactivées',
                    'Notifications'
                );
            }
        } finally {
            this.pushBusy = false;
            this.cdr.detectChanges();
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
                    this.meLanguage = (u.language ?? 'fr') as 'fr' | 'en' | 'ro';
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
            this.meLanguage !== ((this.me.language ?? 'fr') as 'fr' | 'en' | 'ro') ||
            this.mePassword.length > 0 ||
            this.mePasswordConfirm.length > 0;
    }

    /** True when the user has typed a new password but the confirmation field
     *  does not match. Used to disable Save buttons and surface inline help. */
    get mePasswordMismatch(): boolean {
        return this.mePassword.length > 0 && this.mePassword !== this.mePasswordConfirm;
    }
    get newPasswordMismatch(): boolean {
        return this.newPassword.length > 0 && this.newPassword !== this.newPasswordConfirm;
    }
    get editPasswordMismatch(): boolean {
        return this.editPassword.length > 0 && this.editPassword !== this.editPasswordConfirm;
    }

    saveMe(): void {
        if (!this.me || !this.meDirty || this.meSaving) {
            return;
        }
        if (this.mePasswordMismatch) {
            this._toastService.error(
                $localize`:@@passwordMismatch:The two password fields do not match.`,
                'Users'
            );
            return;
        }
        const payload: UserUpdate = {
            email: this.meEmail,
            notificationsEnabled: this.meNotifications,
            language: this.meLanguage,
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
                    this.mePasswordConfirm = '';
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
            this.newPasswordConfirm = '';
            this.newEmail = '';
            this.newRole = 'USER';
            this.newNotifications = false;
            this.newLanguage = 'fr';
        }
    }

    submitCreate(): void {
        if (this.creating) return;
        if (!this.newLogin.trim() || !this.newPassword) {
            this._toastService.error('Login and password are required', 'Users');
            return;
        }
        if (this.newPasswordMismatch) {
            this._toastService.error(
                $localize`:@@passwordMismatch:The two password fields do not match.`,
                'Users'
            );
            return;
        }
        const payload: UserCreate = {
            login: this.newLogin.trim(),
            password: this.newPassword,
            email: this.newEmail.trim() || null,
            role: this.newRole,
            notificationsEnabled: this.newNotifications,
            language: this.newLanguage,
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
                    this.newPassword = '';
                    this.newPasswordConfirm = '';
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
        this.editLanguage = (user.language ?? 'fr') as 'fr' | 'en' | 'ro';
        this.editPassword = '';
        this.editPasswordConfirm = '';
    }

    cancelEdit(): void {
        this.editingLogin = undefined;
        this.editPassword = '';
        this.editPasswordConfirm = '';
    }

    submitEdit(): void {
        if (!this.editingLogin || this.editSaving) return;
        if (this.editPasswordMismatch) {
            this._toastService.error(
                $localize`:@@passwordMismatch:The two password fields do not match.`,
                'Users'
            );
            return;
        }
        const payload: UserUpdate = {
            email: this.editEmail,
            notificationsEnabled: this.editNotifications,
            role: this.editRole,
            language: this.editLanguage,
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
                    this.editPassword = '';
                    this.editPasswordConfirm = '';
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

    // ─── Email + SMTP config ────────────────────────────────────────────────────

    private loadEmailSettings(): void {
        this._configService
            .getAll()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: cfg => {
                    this.emailFrom = cfg.email_settings.from ?? '';
                    const smtp = cfg.email_smtp;
                    if (smtp) {
                        this.mailHost = smtp.host ?? '';
                        this.mailPort = smtp.port ?? 25;
                        this.mailUsername = smtp.username ?? '';
                        this.mailPasswordSet = smtp.password_set;
                        this.mailAuth = smtp.auth;
                        this.mailStartTls = smtp.starttls;
                        this.persistedMailHost = this.mailHost;
                        this.persistedMailPort = this.mailPort;
                        this.persistedMailUsername = this.mailUsername;
                        this.persistedMailAuth = this.mailAuth;
                        this.persistedMailStartTls = this.mailStartTls;
                    }
                    this.cdr.detectChanges();
                },
                error: () => {
                    /* silent — defaults stay */
                },
            });
    }

    saveEmailSettings(): void {
        if (this.emailSaving) return;
        const from = (this.emailFrom || '').trim();
        // Symmetric to the backend ConfigService check: an address without "@"
        // is rejected with a 400 anyway, but failing fast here gives a clearer
        // toast and avoids a half-applied save (From rejected, SMTP fields
        // persisted) since saveEmailSettings -> saveSmtpChanges runs in series.
        if (!from || !from.includes('@')) {
            this._toastService.error(
                $localize`:@@emailFromInvalid:Please provide a valid sender address before saving.`,
                'Email'
            );
            return;
        }
        // Normalize the model so the textarea stops showing untrimmed input
        // after a save — the value sent to the backend and the value in the
        // form should match exactly.
        this.emailFrom = from;
        this.emailSaving = true;
        this._configService
            .setEmailFrom(from)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: () => this.saveSmtpChanges(),
                error: (err: HttpErrorResponse) => this.onEmailSaveError(err),
            });
    }

    private saveSmtpChanges(): void {
        const tail: Observable<unknown>[] = [];
        if (this.mailHost !== this.persistedMailHost) {
            tail.push(this._configService.setMailHost(this.mailHost));
        }
        if (this.mailPort !== this.persistedMailPort) {
            tail.push(this._configService.setMailPort(this.mailPort));
        }
        if (this.mailUsername !== this.persistedMailUsername) {
            tail.push(this._configService.setMailUsername(this.mailUsername));
        }
        if (this.mailAuth !== this.persistedMailAuth) {
            tail.push(this._configService.setMailAuth(this.mailAuth));
        }
        if (this.mailStartTls !== this.persistedMailStartTls) {
            tail.push(this._configService.setMailStartTls(this.mailStartTls));
        }
        const pwd = this.mailPasswordInput?.trim();
        if (pwd && pwd.length > 0) {
            tail.push(this._configService.setMailPassword(pwd));
        }
        if (tail.length === 0) {
            this.emailSaving = false;
            this._toastService.success('Adresses email enregistrées', 'Email');
            // Re-read from the backend so the textarea reflects the actually
            // persisted value. Without this we'd silently mask a backend that
            // rejected/clamped the address and the operator would only notice
            // when the next sunset notification logs "No 'From' address".
            this.loadEmailSettings();
            this.cdr.detectChanges();
            return;
        }
        forkJoin(tail)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: () => {
                    this.emailSaving = false;
                    this.mailPasswordInput = '';
                    this._toastService.success('Réglages email + SMTP enregistrés', 'Email');
                    this.loadEmailSettings();
                },
                error: (err: HttpErrorResponse) => this.onEmailSaveError(err),
            });
    }

    private onEmailSaveError(err: HttpErrorResponse): void {
        this.emailSaving = false;
        this._toastService.error(
            err.error?.message || err.message || 'Save failed',
            `Email — HTTP ${err.status}`
        );
        this.cdr.detectChanges();
    }

    sendTestEmail(): void {
        if (this.emailTestSending) {
            return;
        }
        this.emailTestSending = true;
        this.cdr.detectChanges();
        this._emailTestService
            .sendTestEmail()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: response => {
                    this.emailTestSending = false;
                    this._toastService.success(response.message || 'Test email sent.', 'Email');
                    this.cdr.detectChanges();
                },
                error: (err: HttpErrorResponse) => {
                    this.emailTestSending = false;
                    const detail = err.error?.message || err.message || 'Unknown error';
                    this._toastService.error(detail, `Email — HTTP ${err.status}`);
                    this.cdr.detectChanges();
                },
            });
    }
}
