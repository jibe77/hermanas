import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    HostListener,
    OnInit,
    effect,
    inject,
} from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { LoginService } from '../../services/login.service';
import { UserService } from '../../services/user.service';
import { LoginModalService } from '../../services/login-modal.service';
import { AuthState } from '../../models';

type ModalView = 'login' | 'register';

@Component({
    selector: 'sb-login-modal',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './login-modal.component.html',
    styleUrls: ['./login-modal.component.scss'],
    imports: [ReactiveFormsModule],
})
export class LoginModalComponent implements OnInit {
    private fb = inject(FormBuilder);
    private http = inject(HttpClient);
    private loginService = inject(LoginService);
    private userService = inject(UserService);
    private modal = inject(LoginModalService);
    private router = inject(Router);
    private cdr = inject(ChangeDetectorRef);

    readonly open = this.modal.open;

    view: ModalView = 'login';

    loginForm: FormGroup;
    registerForm: FormGroup;
    errorMessage = '';
    submitting = false;
    registrationSuccess = false;

    constructor() {
        this.loginForm = this.fb.group({
            username: ['', Validators.required],
            password: ['', Validators.required],
            rememberMe: [true],
        });
        // Seed the language preference with the locale of the bundle currently
        // being served — most users pick the matching language for their email
        // notifications, so this default is rarely wrong.
        const path = window.location.pathname;
        const defaultLanguage = path.startsWith('/fr-FR/')
            ? 'fr'
            : path.startsWith('/ro-RO/')
              ? 'ro'
              : 'en';
        this.registerForm = this.fb.group({
            login: ['', [Validators.required, Validators.minLength(2)]],
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(6)]],
            language: [defaultLanguage, Validators.required],
        });
        // Sync the visible view with the view requested by the caller each time
        // the modal opens — lets `loginModal.show('register')` jump straight to
        // the sign-up form without an extra click.
        effect(() => {
            if (this.modal.open()) {
                this.view = this.modal.initialView();
                this.errorMessage = '';
                this.registrationSuccess = false;
                this.cdr.markForCheck();
            }
        });
    }

    ngOnInit(): void {
        if (this.userService.getCurrentUser().authState === AuthState.SignedIn) {
            this.modal.hide();
        }
    }

    @HostListener('document:keydown.escape')
    onEscape(): void {
        if (this.open() && !this.submitting) {
            this.close();
        }
    }

    close(): void {
        this.errorMessage = '';
        this.loginForm.reset({ username: '', password: '', rememberMe: true });
        const path = window.location.pathname;
        const defaultLanguage = path.startsWith('/fr-FR/')
            ? 'fr'
            : path.startsWith('/ro-RO/')
              ? 'ro'
              : 'en';
        this.registerForm.reset({
            login: '',
            email: '',
            password: '',
            language: defaultLanguage,
        });
        this.view = 'login';
        this.registrationSuccess = false;
        this.modal.hide();
    }

    /** Swap the modal to the sign-up form. */
    goToRegister(): void {
        this.errorMessage = '';
        this.view = 'register';
        this.cdr.markForCheck();
    }

    /** Swap the modal back to the sign-in form (also reused after successful sign-up). */
    goToLogin(): void {
        this.errorMessage = '';
        this.view = 'login';
        this.registrationSuccess = false;
        this.cdr.markForCheck();
    }

    async onSubmit(): Promise<void> {
        if (this.loginForm.invalid || this.submitting) {
            return;
        }
        this.submitting = true;
        this.errorMessage = '';
        const { username, password, rememberMe } = this.loginForm.value;
        const outcome = await this.loginService.login(username, password, !!rememberMe);
        this.submitting = false;
        if (outcome === 'ok') {
            this.close();
            // If the freshly logged-in user prefers a different language than the
            // bundle currently being served, reload the SPA on the right locale.
            // syncLocaleWithPreference() returns true when it triggers a real
            // navigation — in that case the in-place router.navigateByUrl below is
            // skipped, the full page reload will run our component lifecycle anyway.
            if (this.userService.syncLocaleWithPreference()) {
                return;
            }
            // Otherwise re-trigger the current route so components that read
            // `isAdmin` / `isAuthenticated` once in `ngOnInit` are re-created with
            // the new session — see `withRouterConfig({ onSameUrlNavigation: 'reload' })`
            // in app.config.ts.
            const currentUrl = this.router.url;
            this.router.navigateByUrl(currentUrl);
        } else if (outcome === 'pending-validation') {
            this.errorMessage = $localize`:@@signinPendingValidation:Your account is awaiting validation by an administrator.`;
        } else {
            this.errorMessage = $localize`:@@signinFailed:Invalid username or password.`;
        }
        this.cdr.markForCheck();
    }

    async onRegisterSubmit(): Promise<void> {
        if (this.registerForm.invalid || this.submitting) {
            return;
        }
        this.submitting = true;
        this.errorMessage = '';
        try {
            await firstValueFrom(
                this.http.post(`${environment.apiUrl}/auth/register`, this.registerForm.value, {
                    withCredentials: true,
                })
            );
            this.registrationSuccess = true;
        } catch (e: unknown) {
            this.errorMessage = this.translateRegisterError(e);
        } finally {
            this.submitting = false;
            this.cdr.markForCheck();
        }
    }

    private translateRegisterError(e: unknown): string {
        const code =
            e instanceof HttpErrorResponse && e.error && typeof e.error === 'object'
                ? (e.error as { error?: string }).error
                : undefined;
        switch (code) {
            case 'LOGIN_TAKEN':
                return $localize`:@@registerErrorLoginTaken:Cet identifiant est déjà utilisé.`;
            case 'INVALID_LOGIN':
                return $localize`:@@registerErrorInvalidLogin:Identifiant invalide (2-64 caractères, lettres/chiffres/._-).`;
            case 'INVALID_PASSWORD':
                return $localize`:@@registerErrorInvalidPassword:Mot de passe trop court (6 caractères minimum).`;
            case 'INVALID_EMAIL':
                return $localize`:@@registerErrorInvalidEmail:Adresse email invalide.`;
            default:
                return $localize`:@@registerErrorGeneric:Inscription impossible. Veuillez réessayer.`;
        }
    }
}
