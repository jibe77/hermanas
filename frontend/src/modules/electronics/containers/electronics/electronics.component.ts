import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    LOCALE_ID,
    OnDestroy,
    OnInit,
    inject,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, timer } from 'rxjs';
import { switchMap, takeUntil } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ToastService } from '@common/services';
import { UserService } from '@modules/auth/services';
import { AuthState, User } from '@modules/auth/models';
import {
    DoorReading,
    DoorStatusService,
    ElectronicsService,
    GpioPin,
    SensorReading,
    SensorStatusService,
} from '@modules/electronics/services';
import {
    ButtonName,
    ButtonStatus,
    ButtonStatusService,
} from '@modules/system/services/button-status.service';
import { ServoCalibrationService } from '@modules/system/services/servo-calibration.service';
import { ConfigService } from '@modules/energy/services/config.service';
import { FanService, LightService } from '@modules/dashboard/services';
import { LayoutDashboardComponent } from '../../../navigation/layouts/layout-dashboard/layout-dashboard.component';
import { DashboardHeadComponent } from '../../../navigation/components/dashboard-head/dashboard-head.component';
import { CardComponent } from '../../../app-common/components/card/card.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';

interface ButtonState {
    pressed?: boolean;
    timestamp?: number;
    error: boolean;
}

/** Live state of a relay (light, fan). */
interface RelayState {
    on?: boolean;
    error: boolean;
}

/** Live state of the door servomotor, pushed on /topic/progress (filtered to DOOR). */
interface ServoState {
    reading?: DoorReading;
    error: boolean;
}

/** Live state of the DHT22 sensor, pushed on /topic/sensor. */
interface SensorState {
    reading?: SensorReading;
    error: boolean;
}

@Component({
    selector: 'sb-electronics',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './electronics.component.html',
    styleUrls: ['electronics.component.scss'],
    imports: [
        LayoutDashboardComponent,
        DashboardHeadComponent,
        CardComponent,
        FaIconComponent,
        FormsModule,
        DatePipe,
        DecimalPipe,
    ],
})
export class ElectronicsComponent implements OnInit, OnDestroy {
    private electronicsService = inject(ElectronicsService);
    private buttonStatusService = inject(ButtonStatusService);
    private sensorStatusService = inject(SensorStatusService);
    private doorStatusService = inject(DoorStatusService);
    private configService = inject(ConfigService);
    private servoService = inject(ServoCalibrationService);
    private lightService = inject(LightService);
    private fanService = inject(FanService);
    private userService = inject(UserService);
    private toast = inject(ToastService);
    private cdr = inject(ChangeDetectorRef);
    private localeId = inject(LOCALE_ID);

    pins: GpioPin[] = [];
    pinsLoading = false;
    pinsError = false;

    /** Buttons + servo calibration require admin (hardware operations). */
    isAdmin = false;
    isAuthenticated = false;

    upButton: ButtonState = { error: false };
    bottomButton: ButtonState = { error: false };
    birdhouseButton: ButtonState = { error: false };
    lightRelay: RelayState = { error: false };
    fanRelay: RelayState = { error: false };
    servo: ServoState = { error: false };
    sensor: SensorState = { error: false };

    /** Refresh interval for the relay HTTP polling (ms). Relays do not have a WS topic. */
    private static readonly RELAY_POLL_MS = 15000;

    servoOpeningPosition = 16;
    servoClosingPosition = 5;
    servoOpeningDurationMs = 10000;
    servoClosingDurationMs = 2350;
    servoSaving = false;
    servoNudgeMs = 100;
    servoNudging = false;

    private destroy$ = new Subject<void>();

    /** Whether the live-status subscriptions (HTTP seeds + WS topics) have been started. */
    private liveStatusStarted = false;

    ngOnInit(): void {
        this.loadGpioPins();
        // The live status of buttons / relays / servo / sensor is now public on the
        // backend (see SecurityConfig — /buttons/status is exposed read-only). We
        // therefore start the seeds and WebSocket subscriptions for every visitor,
        // not only authenticated admins, so the State column of the GPIO table is
        // populated even when the operator is not signed in.
        this.startLiveStatus();
        this.userService.user$.pipe(takeUntil(this.destroy$)).subscribe((user: User) => {
            this.isAuthenticated = !!user && user.authState === AuthState.SignedIn;
            const wasAdmin = this.isAdmin;
            this.isAdmin = this.userService.isAdmin();
            if (this.isAdmin && !wasAdmin) {
                // Servo calibration is the only piece still gated on ADMIN — its
                // sliders mutate hardware behaviour, so the form values are only
                // fetched when the operator has the rights to edit them.
                this.loadServoPositions();
            }
            this.cdr.detectChanges();
        });
    }

    private startLiveStatus(): void {
        if (this.liveStatusStarted) {
            return;
        }
        this.liveStatusStarted = true;
        this.loadInitialButtonStatus();
        this.subscribeToButtonUpdates();
        this.startRelayPolling();
        this.loadInitialSensorStatus();
        this.subscribeToSensorUpdates();
        this.loadInitialDoorStatus();
        this.subscribeToDoorUpdates();
    }

    isFr(): boolean {
        return this.localeId.startsWith('fr');
    }

    pinDisplayName(p: GpioPin): string {
        return this.isFr() ? p.labelFr : p.label;
    }

    /**
     * Status accessor used by the template — returns the state object matching
     * the pin's kind, or null when no live state is available.
     */
    pinState(p: GpioPin): ButtonState | RelayState | ServoState | SensorState | null {
        switch (p.kind) {
            case 'button':
                if (p.key === 'door.button.up') return this.upButton;
                if (p.key === 'door.button.bottom') return this.bottomButton;
                if (p.key === 'birdhouse.button') return this.birdhouseButton;
                return null;
            case 'light':
                return this.lightRelay;
            case 'fan':
                return this.fanRelay;
            case 'servo':
                return this.servo;
            case 'sensor':
                return this.sensor;
            default:
                return null;
        }
    }

    isButton(p: GpioPin): boolean {
        return p.kind === 'button';
    }

    isRelay(p: GpioPin): boolean {
        return p.kind === 'light' || p.kind === 'fan';
    }

    isServo(p: GpioPin): boolean {
        return p.kind === 'servo';
    }

    isSensor(p: GpioPin): boolean {
        return p.kind === 'sensor';
    }

    /** Pins whose live state is gated on authentication (admin-only here). */
    isLive(p: GpioPin): boolean {
        return this.isButton(p) || this.isRelay(p) || this.isServo(p) || this.isSensor(p);
    }

    asButtonState(
        s: ButtonState | RelayState | ServoState | SensorState | null
    ): ButtonState | null {
        return s && 'pressed' in (s as object) ? (s as ButtonState) : null;
    }

    asRelayState(
        s: ButtonState | RelayState | ServoState | SensorState | null
    ): RelayState | null {
        return s && 'on' in (s as object) ? (s as RelayState) : null;
    }

    asServoState(
        s: ButtonState | RelayState | ServoState | SensorState | null
    ): ServoState | null {
        return s && (s as ServoState).reading !== undefined
            ? (s as ServoState)
            : s && this.isServoLike(s)
              ? (s as ServoState)
              : null;
    }

    asSensorState(
        s: ButtonState | RelayState | ServoState | SensorState | null
    ): SensorState | null {
        return s && (s as SensorState).reading !== undefined
            ? (s as SensorState)
            : s && this.isSensorLike(s)
              ? (s as SensorState)
              : null;
    }

    private isServoLike(s: unknown): boolean {
        // The state objects are seeded with { error: false } and only get a
        // `reading` later — we can't rely on duck-typing the reading. The caller
        // already routed via pinState(p), so trust the instance identity.
        return s === this.servo;
    }

    private isSensorLike(s: unknown): boolean {
        return s === this.sensor;
    }

    /**
     * The Electronics page reports the live state of the *servomotor*, not the
     * door: OPENING/CLOSING means the servo is currently moving, anything else
     * means it is idle. The door's open-vs-closed status itself is shown
     * elsewhere (dashboard, door card) — here we want the hardware-level view.
     */
    doorStateLabel(state: string): string {
        if (state === 'OPENING' || state === 'CLOSING') {
            return this.isFr() ? 'En mouvement' : 'Moving';
        }
        return this.isFr() ? 'Immobile' : 'Idle';
    }

    doorStateBadge(state: string): string {
        if (state === 'OPENING' || state === 'CLOSING') {
            return 'text-bg-info';
        }
        return 'text-bg-secondary';
    }

    /**
     * The birdhouse button is wired with inverted logic (pressed when the GPIO
     * reads HIGH). Mirror the convention used in the buttons panel.
     */
    isButtonPressed(p: GpioPin, state: ButtonState): boolean | undefined {
        if (state.pressed === undefined) return undefined;
        return p.key === 'birdhouse.button' ? !state.pressed : state.pressed;
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    // ─── GPIO listing ───────────────────────────────────────────────────────────

    private loadGpioPins(): void {
        this.pinsLoading = true;
        this.pinsError = false;
        this.electronicsService
            .listGpioPins()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: data => {
                    this.pins = data;
                    this.pinsLoading = false;
                    this.cdr.markForCheck();
                },
                error: () => {
                    this.pinsError = true;
                    this.pinsLoading = false;
                    this.cdr.markForCheck();
                },
            });
    }

    // ─── Button live status ─────────────────────────────────────────────────────

    private loadInitialButtonStatus(): void {
        this.buttonStatusService
            .getInitialStatus()
            .pipe(takeUntil(this.destroy$))
            .subscribe(
                statuses => {
                    statuses.forEach(s => this.applyStatus(s));
                    this.upButton.error = false;
                    this.bottomButton.error = false;
                    this.birdhouseButton.error = false;
                    this.cdr.detectChanges();
                },
                () => {
                    this.upButton = { error: true };
                    this.bottomButton = { error: true };
                    this.birdhouseButton = { error: true };
                    this.cdr.detectChanges();
                }
            );
    }

    private subscribeToButtonUpdates(): void {
        this.buttonStatusService
            .observeUpdates()
            .pipe(takeUntil(this.destroy$))
            .subscribe(update => {
                this.applyStatus(update);
                this.cdr.detectChanges();
            });
    }

    private applyStatus(status: ButtonStatus): void {
        const target = this.targetFor(status.button);
        target.pressed = status.pressed;
        target.timestamp = status.timestamp;
        target.error = false;
    }

    private targetFor(button: ButtonName): ButtonState {
        if (button === 'UP') return this.upButton;
        if (button === 'BIRDHOUSE') return this.birdhouseButton;
        return this.bottomButton;
    }

    // ─── Relay live status (light, fan) ─────────────────────────────────────────

    private startRelayPolling(): void {
        timer(0, ElectronicsComponent.RELAY_POLL_MS)
            .pipe(
                switchMap(() => this.lightService.getStatus()),
                takeUntil(this.destroy$)
            )
            .subscribe({
                next: status => {
                    this.lightRelay = { on: status.statusEnum === 'ON', error: false };
                    this.cdr.detectChanges();
                },
                error: () => {
                    this.lightRelay = { error: true };
                    this.cdr.detectChanges();
                },
            });
        timer(0, ElectronicsComponent.RELAY_POLL_MS)
            .pipe(
                switchMap(() => this.fanService.getStatus()),
                takeUntil(this.destroy$)
            )
            .subscribe({
                next: status => {
                    this.fanRelay = { on: status.statusEnum === 'ON', error: false };
                    this.cdr.detectChanges();
                },
                error: () => {
                    this.fanRelay = { error: true };
                    this.cdr.detectChanges();
                },
            });
    }

    // ─── Servo live status (door) ───────────────────────────────────────────────

    private loadInitialDoorStatus(): void {
        this.doorStatusService
            .getInitialStatus()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: reading => {
                    this.servo = { reading, error: false };
                    this.cdr.detectChanges();
                },
                error: () => {
                    this.servo = { error: true };
                    this.cdr.detectChanges();
                },
            });
    }

    private subscribeToDoorUpdates(): void {
        this.doorStatusService
            .observeUpdates()
            .pipe(takeUntil(this.destroy$))
            .subscribe(reading => {
                this.servo = { reading, error: false };
                this.cdr.detectChanges();
            });
    }

    // ─── Sensor live status (DHT22) ─────────────────────────────────────────────

    private loadInitialSensorStatus(): void {
        this.sensorStatusService
            .getInitialStatus()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: reading => {
                    this.sensor = { reading, error: false };
                    this.cdr.detectChanges();
                },
                error: () => {
                    this.sensor = { error: true };
                    this.cdr.detectChanges();
                },
            });
    }

    private subscribeToSensorUpdates(): void {
        this.sensorStatusService
            .observeUpdates()
            .pipe(takeUntil(this.destroy$))
            .subscribe(reading => {
                this.sensor = { reading, error: false };
                this.cdr.detectChanges();
            });
    }

    // ─── Servo calibration ──────────────────────────────────────────────────────

    private loadServoPositions(): void {
        this.configService
            .getAll()
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: cfg => {
                    this.servoOpeningPosition = cfg.servo_positions.door_opening_position;
                    this.servoClosingPosition = cfg.servo_positions.door_closing_position;
                    this.servoOpeningDurationMs = cfg.servo_positions.door_opening_duration_ms;
                    this.servoClosingDurationMs = cfg.servo_positions.door_closing_duration_ms;
                    this.cdr.detectChanges();
                },
                error: () => {
                    /* keep defaults */
                },
            });
    }

    saveOpeningPosition(): void {
        this.saveServoConfig(
            this.configService.setDoorOpeningPosition(this.servoOpeningPosition),
            `Position ouverte : ${this.servoOpeningPosition}`
        );
    }

    saveClosingPosition(): void {
        this.saveServoConfig(
            this.configService.setDoorClosingPosition(this.servoClosingPosition),
            `Position fermée : ${this.servoClosingPosition}`
        );
    }

    saveOpeningDuration(): void {
        this.saveServoConfig(
            this.configService.setDoorOpeningDuration(this.servoOpeningDurationMs),
            `Durée d'ouverture : ${this.servoOpeningDurationMs} ms`
        );
    }

    saveClosingDuration(): void {
        this.saveServoConfig(
            this.configService.setDoorClosingDuration(this.servoClosingDurationMs),
            `Durée de fermeture : ${this.servoClosingDurationMs} ms`
        );
    }

    private saveServoConfig(call$: import('rxjs').Observable<unknown>, successMsg: string): void {
        if (this.servoSaving) return;
        this.servoSaving = true;
        call$.pipe(takeUntil(this.destroy$)).subscribe({
            next: () => {
                this.servoSaving = false;
                this.toast.success(successMsg, 'Servo');
                this.cdr.detectChanges();
            },
            error: (err: HttpErrorResponse) => {
                this.servoSaving = false;
                this.toast.error(
                    err.error?.message || err.message || 'Save failed',
                    `Servo — HTTP ${err.status}`
                );
                this.cdr.detectChanges();
            },
        });
    }

    nudgeClockwise(): void {
        this.nudge(true);
    }

    nudgeCounterClockwise(): void {
        this.nudge(false);
    }

    private nudge(clockwise: boolean): void {
        if (this.servoNudging) return;
        const ms = Math.max(1, Math.min(30000, this.servoNudgeMs || 100));
        this.servoNudging = true;
        const call$ = clockwise
            ? this.servoService.turnClockwise(ms)
            : this.servoService.turnCounterClockwise(ms);
        call$.pipe(takeUntil(this.destroy$)).subscribe({
            next: () => {
                this.servoNudging = false;
                this.toast.success(
                    `Servo ${clockwise ? 'clockwise' : 'counter-clockwise'} ${ms} ms`,
                    'Servo'
                );
                this.cdr.detectChanges();
            },
            error: (err: HttpErrorResponse) => {
                this.servoNudging = false;
                this.toast.error(
                    err.error?.message || err.message || 'Nudge failed',
                    `Servo — HTTP ${err.status}`
                );
                this.cdr.detectChanges();
            },
        });
    }
}
