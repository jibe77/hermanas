import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';

@Component({
    selector: 'sb-register',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './register.component.html',
    styleUrls: ['register.component.scss'],
})
export class RegisterComponent {
    registerForm: FormGroup;
    errorMessage = '';
    submitting = false;
    success = false;

    constructor(
        private fb: FormBuilder,
        private http: HttpClient,
        private router: Router,
        private cdr: ChangeDetectorRef
    ) {
        this.registerForm = this.fb.group({
            login: ['', [Validators.required, Validators.minLength(2)]],
            password: ['', [Validators.required, Validators.minLength(6)]],
            email: ['', [Validators.required, Validators.email]],
        });
    }

    async onSubmit(): Promise<void> {
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
            this.success = true;
        } catch (e: unknown) {
            this.errorMessage = this.translateError(e);
        } finally {
            this.submitting = false;
            this.cdr.markForCheck();
        }
    }

    goToLogin(): void {
        this.router.navigateByUrl('/auth/login');
    }

    private translateError(e: unknown): string {
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
