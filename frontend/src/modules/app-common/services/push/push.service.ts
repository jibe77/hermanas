import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { SwPush } from '@angular/service-worker';
import { firstValueFrom, lastValueFrom } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { LoggerService } from '../logger/logger.service';

/**
 * Drives the Web Push opt-in flow. The browser does the heavy lifting (asking
 * the user for the Notification permission, talking to the push service) — we
 * just stash the resulting endpoint + keys on the server so we can later push
 * notifications to it via the VAPID flow.
 */
@Injectable({ providedIn: 'root' })
export class PushService {
    private swPush = inject(SwPush);
    private http = inject(HttpClient);
    private router = inject(Router);
    private logger = inject(LoggerService);

    /**
     * Wires the click-on-notification listener once at startup. We can't do
     * this in the constructor because the ServiceWorker may not be enabled in
     * dev mode (provideServiceWorker disables itself when isDevMode() is true).
     */
    public initialize(): void {
        if (!this.swPush.isEnabled) {
            return;
        }
        this.swPush.notificationClicks.subscribe(evt => {
            const data = evt.notification.data as { url?: string } | undefined;
            const url = data?.url || '/dashboard';
            this.router.navigateByUrl(url).catch(err => {
                this.logger.error(
                    'Failed to navigate after notification click',
                    { url, error: err },
                    'PushService'
                );
            });
        });
    }

    public isEnabled(): boolean {
        return this.swPush.isEnabled;
    }

    /**
     * True when the browser has already granted the Notification permission
     * AND a SwPush subscription is active. Used by the toggle UI to display
     * the current opt-in state.
     */
    public async isSubscribed(): Promise<boolean> {
        if (!this.swPush.isEnabled) {
            return false;
        }
        const sub = await firstValueFrom(this.swPush.subscription);
        return sub !== null;
    }

    /**
     * Requests the Notification permission, asks the browser to create a push
     * subscription using our VAPID public key, then POSTs the resulting keys
     * to the server. Resolves to true on success, false if the user denied or
     * an error occurred.
     */
    public async subscribe(): Promise<boolean> {
        if (!this.swPush.isEnabled) {
            return false;
        }
        try {
            const { publicKey } = await firstValueFrom(
                this.http.get<{ publicKey: string }>(`${environment.apiUrl}/push/vapid-public-key`)
            );
            if (!publicKey) {
                this.logger.warn('Server has no VAPID public key', undefined, 'PushService');
                return false;
            }
            const sub = await this.swPush.requestSubscription({ serverPublicKey: publicKey });
            const payload = this.toServerPayload(sub);
            if (!payload) {
                return false;
            }
            await lastValueFrom(this.http.post(`${environment.apiUrl}/push/subscribe`, payload));
            return true;
        } catch (err) {
            this.logger.error('Push subscription failed', { error: err }, 'PushService');
            return false;
        }
    }

    /** Cancel the browser subscription and tell the server to delete its row. */
    public async unsubscribe(): Promise<boolean> {
        if (!this.swPush.isEnabled) {
            return false;
        }
        try {
            const current = await firstValueFrom(this.swPush.subscription);
            if (!current) {
                return true;
            }
            await this.swPush.unsubscribe();
            await lastValueFrom(
                this.http.post(`${environment.apiUrl}/push/unsubscribe`, {
                    endpoint: current.endpoint,
                })
            );
            return true;
        } catch (err) {
            this.logger.error('Push unsubscription failed', { error: err }, 'PushService');
            return false;
        }
    }

    private toServerPayload(
        sub: PushSubscription
    ): { endpoint: string; p256dh: string; auth: string } | null {
        const p256dh = sub.getKey('p256dh');
        const auth = sub.getKey('auth');
        if (!p256dh || !auth) {
            return null;
        }
        return {
            endpoint: sub.endpoint,
            p256dh: this.arrayBufferToBase64(p256dh),
            auth: this.arrayBufferToBase64(auth),
        };
    }

    private arrayBufferToBase64(buf: ArrayBuffer): string {
        const bytes = new Uint8Array(buf);
        let binary = '';
        for (let i = 0; i < bytes.byteLength; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return btoa(binary);
    }
}
