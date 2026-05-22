import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { LoginService, UserService } from '@modules/auth/services';
import { AuthState } from '@modules/auth/models';

@Component({
    selector: 'sb-login',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './login.component.html',
    styleUrls: ['login.component.scss'],
})
export class LoginComponent implements OnInit {
    loginForm: FormGroup;
    errorMessage = '';
    submitting = false;

    constructor(
        private fb: FormBuilder,
        private loginService: LoginService,
        private userService: UserService,
        private router: Router,
        private cdr: ChangeDetectorRef
    ) {
        this.loginForm = this.fb.group({
            username: ['', Validators.required],
            password: ['', Validators.required],
        });
    }

    ngOnInit(): void {
        // If the session is already valid, skip the form.
        this.userService.checkAuthState().then(() => {
            const current = this.userService.getCurrentUser();
            if (current.authState === AuthState.SignedIn) {
                this.router.navigate(['dashboard']);
            }
        });
    }

    async onSubmit(): Promise<void> {
        if (this.loginForm.invalid || this.submitting) {
            return;
        }
        this.submitting = true;
        this.errorMessage = '';
        const { username, password } = this.loginForm.value;
        const ok = await this.loginService.login(username, password);
        this.submitting = false;
        if (ok) {
            this.router.navigate(['dashboard']);
        } else {
            this.errorMessage = $localize`:@@signinFailed:Identifiant ou mot de passe invalide.`;
        }
        this.cdr.markForCheck();
    }
}
