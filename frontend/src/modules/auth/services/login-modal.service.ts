import { Injectable, signal } from '@angular/core';

export type LoginModalView = 'login' | 'register';

@Injectable({ providedIn: 'root' })
export class LoginModalService {
    private readonly _open = signal(false);
    private readonly _initialView = signal<LoginModalView>('login');
    readonly open = this._open.asReadonly();
    readonly initialView = this._initialView.asReadonly();

    show(view: LoginModalView = 'login'): void {
        this._initialView.set(view);
        this._open.set(true);
    }

    hide(): void {
        this._open.set(false);
    }
}
