import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    HostListener,
    OnInit,
    inject,
} from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { LoginService } from '../../services/login.service';
import { UserService } from '../../services/user.service';
import { LoginModalService } from '../../services/login-modal.service';
import { AuthState } from '../../models';

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
    private loginService = inject(LoginService);
    private userService = inject(UserService);
    private modal = inject(LoginModalService);
    private router = inject(Router);
    private cdr = inject(ChangeDetectorRef);

    readonly open = this.modal.open;

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
        this.modal.hide();
    }

    goToRegister(): void {
        this.close();
        this.router.navigateByUrl('/auth/register');
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
        } else if (outcome === 'pending-validation') {
            this.errorMessage = $localize`:@@signinPendingValidation:Votre compte est en attente de validation par un administrateur.`;
        } else {
            this.errorMessage = $localize`:@@signinFailed:Identifiant ou mot de passe invalide.`;
        }
        this.cdr.markForCheck();
    }
}
