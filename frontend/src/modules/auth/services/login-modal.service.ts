import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoginModalService {
    private readonly _open = signal(false);
    readonly open = this._open.asReadonly();

    show(): void {
        this._open.set(true);
    }

    hide(): void {
        this._open.set(false);
    }
}
