import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { LoggerService } from '../logger/logger.service';

/**
 * Minimal typing for the proprietary `beforeinstallprompt` event. The DOM lib
 * does not ship it because it is not in the W3C spec (Chrome/Edge only).
 */
interface BeforeInstallPromptEvent extends Event {
    readonly platforms: string[];
    readonly userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
    prompt(): Promise<void>;
}

const DISMISS_KEY = 'hermanas.pwa.install-dismissed-at';
const DISMISS_COOLDOWN_DAYS = 14;

@Injectable({ providedIn: 'root' })
export class PwaInstallService {
    private logger = inject(LoggerService);
    private deferredPrompt: BeforeInstallPromptEvent | null = null;
    private canInstallSubject = new BehaviorSubject<boolean>(false);

    /**
     * Emits true when the browser fired `beforeinstallprompt` AND the user has
     * not dismissed the banner in the last 14 days AND the app is not already
     * running as an installed PWA.
     */
    public canInstall$: Observable<boolean> = this.canInstallSubject.asObservable();

    public initialize(): void {
        if (typeof window === 'undefined') {
            return;
        }
        if (this.isAlreadyInstalled()) {
            this.logger.debug('Already installed as PWA, skipping', undefined, 'PwaInstallService');
            return;
        }
        window.addEventListener('beforeinstallprompt', (event: Event) => {
            // Prevent the browser's default mini-infobar; we render our own.
            event.preventDefault();
            this.deferredPrompt = event as BeforeInstallPromptEvent;
            if (!this.isInCooldown()) {
                this.canInstallSubject.next(true);
            }
        });
        window.addEventListener('appinstalled', () => {
            this.logger.info('PWA installed', undefined, 'PwaInstallService');
            this.deferredPrompt = null;
            this.canInstallSubject.next(false);
        });
    }

    /**
     * Triggers the browser's install dialog. Resolves to true if the user
     * accepted, false otherwise (dismissed or no prompt available).
     */
    public async promptInstall(): Promise<boolean> {
        if (!this.deferredPrompt) {
            return false;
        }
        await this.deferredPrompt.prompt();
        const choice = await this.deferredPrompt.userChoice;
        this.deferredPrompt = null;
        this.canInstallSubject.next(false);
        return choice.outcome === 'accepted';
    }

    /** User clicked "Not now" — hide the banner for DISMISS_COOLDOWN_DAYS. */
    public dismiss(): void {
        try {
            localStorage.setItem(DISMISS_KEY, String(Date.now()));
        } catch {
            // localStorage may be disabled (private browsing); silently ignore.
        }
        this.canInstallSubject.next(false);
    }

    private isAlreadyInstalled(): boolean {
        // matchMedia `display-mode: standalone` is true when the page is loaded
        // from an installed PWA on Chrome/Edge/Safari. Safari iOS uses the
        // legacy `navigator.standalone` boolean.
        if (window.matchMedia('(display-mode: standalone)').matches) {
            return true;
        }
        const navAny = navigator as unknown as { standalone?: boolean };
        return navAny.standalone === true;
    }

    private isInCooldown(): boolean {
        try {
            const raw = localStorage.getItem(DISMISS_KEY);
            if (!raw) {
                return false;
            }
            const dismissedAt = Number(raw);
            if (Number.isNaN(dismissedAt)) {
                return false;
            }
            const elapsedMs = Date.now() - dismissedAt;
            return elapsedMs < DISMISS_COOLDOWN_DAYS * 24 * 60 * 60 * 1000;
        } catch {
            return false;
        }
    }
}
