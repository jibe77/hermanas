import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
    inject,
} from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LoginService, UserService } from '@modules/auth/services';
import { AuthState } from '@modules/auth/models';
import { LayoutAuthComponent } from '../../../navigation/layouts/layout-auth/layout-auth.component';

@Component({
    selector: 'sb-login',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './login.component.html',
    styleUrls: ['login.component.scss'],
    imports: [
        LayoutAuthComponent,
        ReactiveFormsModule,
        RouterLink,
    ],
})
export class LoginComponent implements OnInit {
    private fb = inject(FormBuilder);
    private loginService = inject(LoginService);
    private userService = inject(UserService);
    private router = inject(Router);
    private cdr = inject(ChangeDetectorRef);

    loginForm: FormGroup;
    errorMessage = '';
    submitting = false;

    constructor() {
        this.loginForm = this.fb.group({
            username: ['', Validators.required],
            password: ['', Validators.required],
            rememberMe: [true],
        });
    }

    ngOnInit(): void {
        // APP_INITIALIZER already resolved /auth/me before bootstrap, so this is a synchronous
        // read — no race with the HTTP probe and no extra round-trip.
        if (this.userService.getCurrentUser().authState === AuthState.SignedIn) {
            this.goToDashboard();
        }
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
            this.goToDashboard();
        } else if (outcome === 'pending-validation') {
            this.errorMessage = $localize`:@@signinPendingValidation:Votre compte est en attente de validation par un administrateur.`;
        } else {
            this.errorMessage = $localize`:@@signinFailed:Identifiant ou mot de passe invalide.`;
        }
        this.cdr.markForCheck();
    }

    private goToDashboard(): void {
        this.router
            .navigateByUrl('/dashboard')
            .then(success => {
                if (!success) {
                    window.location.href = 'dashboard';
                }
            })
            .catch(() => {
                window.location.href = 'dashboard';
            });
    }
}
