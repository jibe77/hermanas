import { Injectable, inject } from '@angular/core';
import { SwUpdate, VersionReadyEvent } from '@angular/service-worker';
import { filter } from 'rxjs/operators';
import { LoggerService } from '../logger/logger.service';
import { ToastService } from '../toast/toast.service';

/**
 * Wires Angular's `SwUpdate` to a user-facing toast: as soon as the service
 * worker has prefetched a new bundle, ask the user to refresh (we do NOT
 * auto-reload — that would interrupt a running door command).
 */
@Injectable({ providedIn: 'root' })
export class SwUpdateService {
    private swUpdate = inject(SwUpdate);
    private toasts = inject(ToastService);
    private logger = inject(LoggerService);

    public initialize(): void {
        if (!this.swUpdate.isEnabled) {
            return;
        }
        this.swUpdate.versionUpdates
            .pipe(filter((evt): evt is VersionReadyEvent => evt.type === 'VERSION_READY'))
            .subscribe(evt => {
                this.logger.info(
                    'New app version ready',
                    { hash: evt.latestVersion.hash },
                    'SwUpdateService'
                );
                this.toasts.info(
                    $localize`:@@swUpdateAvailableBody:A new version of Hermanas is ready. Reload?`,
                    $localize`:@@swUpdateAvailableTitle:Update available`,
                    0 // sticky — only dismissed by user
                );
            });
    }

    /** Force the SW to activate the new version and reload the page. */
    public applyUpdate(): void {
        if (!this.swUpdate.isEnabled) {
            return;
        }
        this.swUpdate.activateUpdate().then(() => document.location.reload());
    }
}
