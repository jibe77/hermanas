import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { AbstractService } from '@common/services';
import { ResidentsService } from '@modules/residents/services';

// Centralized state and triggers for the playful side of the UI.
// One service so every easter egg can be discovered from a single file and
// so the global key/click listeners are installed exactly once.
//
// Triggered effects expose reactive signals consumed by components
// (chickens-strip, top-nav brand) which read them in templates to add
// CSS classes for the visual response.
@Injectable({ providedIn: 'root' })
export class EasterEggsService extends AbstractService {
    private http = inject(HttpClient);
    private residentsService = inject(ResidentsService);

    // Konami code → chickens dance for a short burst.
    readonly dancing = signal(false);
    // 5 clicks on the brand hen → disco mode (flashing lights).
    readonly disco = signal(false);
    // April 1st → walk backwards (set once at boot, never changes).
    readonly aprilFools = signal(false);
    // A pensioner's birthday/arrival anniversary today → confetti.
    readonly birthdayActive = signal<string | null>(null);
    // Advent season (1st Sunday of Advent → Dec 25 inclusive) → snowfall,
    // santa hats on the strolling hens, blinking tree in a corner.
    readonly advent = signal(false);
    // Holy Week → Easter Monday → floating pastel eggs, pastel navbar
    // accent, occasional egg laid by each strolling hen.
    readonly easter = signal(false);
    // Halloween window (Oct 24 → Nov 1 inclusive, covering All Saints' Day
    // and the prior week so the theme has a meaningful presence). Floating
    // pumpkins, bats flying across, spider web in a corner, orange/purple
    // navbar accent, witch hats on the strolling hens.
    readonly halloween = signal(false);

    private konamiBuffer: string[] = [];
    private static readonly KONAMI: readonly string[] = [
        'ArrowUp', 'ArrowUp', 'ArrowDown', 'ArrowDown',
        'ArrowLeft', 'ArrowRight', 'ArrowLeft', 'ArrowRight',
        'b', 'a',
    ];

    private brandClickCount = 0;
    private brandClickResetTimer?: ReturnType<typeof setTimeout>;
    private static readonly BRAND_CLICKS_FOR_DISCO = 5;
    private static readonly BRAND_CLICK_WINDOW_MS = 3000;

    install(): void {
        // Seasonal flags are evaluated once when the SPA boots. We use the
        // user's local clock — running the SPA through midnight on the
        // boundary won't flip the theme until the page reloads, which is an
        // acceptable trade-off for a static check.
        const today = this.todayWithOverride();
        const isAprilFools = today.getMonth() === 3 && today.getDate() === 1;
        const isAdvent = this.isAdvent(today);
        const isEaster = this.isEasterSeason(today);
        const isHalloween = this.isHalloweenSeason(today);
        if (isAprilFools) this.aprilFools.set(true);
        if (isAdvent) this.advent.set(true);
        if (isEaster) this.easter.set(true);
        if (isHalloween) this.halloween.set(true);
        // Surfaced so an operator playing with ?theme-date=YYYY-MM-DD can see
        // in the console which season the reference date is supposed to hit —
        // a date outside any window is silently no-op otherwise.
        if (window.location.search.includes('theme')) {
            const isoDay = today.toISOString().substring(0, 10);
            console.info(
                `[hermanas] seasonal check: reference=${isoDay}, ` +
                `aprilFools=${isAprilFools}, advent=${isAdvent}, ` +
                `easter=${isEaster}, halloween=${isHalloween}`
            );
        }

        window.addEventListener('keydown', (e) => this.onKeyDown(e));

        // Expose the cocorico trigger to the JS console for curious users.
        // We attach it to window with a non-enumerable property so it does
        // not pollute autocompletion lists when introspecting `window`.
        Object.defineProperty(window, 'cocorico', {
            value: () => this.triggerCocorico(),
            writable: false,
            enumerable: false,
            configurable: true,
        });

        this.checkBirthdays();
    }

    onBrandClick(): void {
        this.brandClickCount += 1;
        clearTimeout(this.brandClickResetTimer);
        this.brandClickResetTimer = setTimeout(() => {
            this.brandClickCount = 0;
        }, EasterEggsService.BRAND_CLICK_WINDOW_MS);

        if (this.brandClickCount >= EasterEggsService.BRAND_CLICKS_FOR_DISCO) {
            this.brandClickCount = 0;
            this.triggerDisco();
        }
    }

    // Plays a short synthetic "cluck" via Web Audio so we don't need to
    // ship an audio asset just for this. Two quick pitched bursts approximate
    // a hen's "cot-cot" sound. Safe to call rapidly: each invocation creates
    // and closes its own context-bound nodes.
    playCluck(): void {
        // Some browsers reject AudioContext creation without a user gesture;
        // every caller of playCluck is a click handler, so this is safe.
        try {
            const Ctx = (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext);
            const ctx = new Ctx();
            const now = ctx.currentTime;
            this.cluckBurst(ctx, now, 520);
            this.cluckBurst(ctx, now + 0.12, 600);
            setTimeout(() => ctx.close().catch(() => undefined), 400);
        } catch {
            // Audio API unavailable; silent fallback.
        }
    }

    private cluckBurst(ctx: AudioContext, when: number, freq: number): void {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'square';
        osc.frequency.setValueAtTime(freq, when);
        osc.frequency.exponentialRampToValueAtTime(freq * 0.6, when + 0.08);
        gain.gain.setValueAtTime(0.0001, when);
        gain.gain.exponentialRampToValueAtTime(0.2, when + 0.01);
        gain.gain.exponentialRampToValueAtTime(0.0001, when + 0.09);
        osc.connect(gain).connect(ctx.destination);
        osc.start(when);
        osc.stop(when + 0.1);
    }

    private onKeyDown(event: KeyboardEvent): void {
        // Ignore typing in form fields so the konami chord doesn't trigger
        // while the user is filling in a real input.
        const target = event.target as HTMLElement | null;
        if (target && /^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName)) {
            return;
        }
        const key = event.key;
        const expected = EasterEggsService.KONAMI[this.konamiBuffer.length];
        // Compare case-insensitive for letters (B/A vs b/a).
        if (key.toLowerCase() === expected.toLowerCase()) {
            this.konamiBuffer.push(key);
            if (this.konamiBuffer.length === EasterEggsService.KONAMI.length) {
                this.konamiBuffer = [];
                this.triggerDance();
            }
        } else {
            this.konamiBuffer = [];
        }
    }

    private triggerDance(): void {
        this.dancing.set(true);
        setTimeout(() => this.dancing.set(false), 6000);
    }

    private triggerDisco(): void {
        this.disco.set(true);
        setTimeout(() => this.disco.set(false), 8000);
    }

    private triggerCocorico(): void {
        this.http.get<boolean>(`${this.domainBase}/music/cocorico`).subscribe({
            next: () => console.info('🐔 Cocorico!'),
            error: (err) => console.error('Cocorico failed', err),
        });
    }

    // Allow forcing a specific date through ?theme-date=YYYY-MM-DD for
    // manual testing of seasonal themes outside their actual window.
    // Also accepts shorthand: ?theme=advent | ?theme=easter | ?theme=april.
    private todayWithOverride(): Date {
        const params = new URLSearchParams(window.location.search);
        const explicit = params.get('theme-date');
        if (explicit) {
            const d = new Date(explicit);
            if (!isNaN(d.getTime())) return d;
        }
        const shorthand = params.get('theme');
        if (shorthand === 'advent') return new Date(new Date().getFullYear(), 11, 15);
        if (shorthand === 'april')  return new Date(new Date().getFullYear(), 3, 1);
        if (shorthand === 'easter') {
            // Snap to Good Friday of the current year so every easter effect
            // is in range when the operator opens the URL.
            const e = EasterEggsService.computeEaster(new Date().getFullYear());
            const goodFriday = new Date(e);
            goodFriday.setDate(e.getDate() - 2);
            return goodFriday;
        }
        if (shorthand === 'halloween') {
            // Halloween itself — comfortably inside the Oct 24 → Nov 1 window.
            return new Date(new Date().getFullYear(), 9, 31);
        }
        return new Date();
    }

    // Advent: 4 Sundays before Christmas → Dec 25 inclusive. We find the
    // first Sunday of Advent by walking back from Dec 25 (Christmas Day) to
    // its preceding Sunday, then subtracting 3 more weeks.
    private isAdvent(today: Date): boolean {
        const year = today.getMonth() === 0 ? today.getFullYear() - 1 : today.getFullYear();
        const christmas = new Date(year, 11, 25);
        const dow = christmas.getDay(); // 0 = Sunday … 6 = Saturday
        const daysToPrevSunday = dow === 0 ? 7 : dow;
        const firstSunday = new Date(year, 11, 25 - daysToPrevSunday - 21);
        const endOfChristmas = new Date(year, 11, 25, 23, 59, 59);
        return today >= firstSunday && today <= endOfChristmas;
    }

    // Easter Sunday for a Gregorian year, per Butcher's algorithm (1876),
    // the well-known closed-form computation. Returns midnight local time.
    static computeEaster(year: number): Date {
        const a = year % 19;
        const b = Math.floor(year / 100);
        const c = year % 100;
        const d = Math.floor(b / 4);
        const e = b % 4;
        const f = Math.floor((b + 8) / 25);
        const g = Math.floor((b - f + 1) / 3);
        const h = (19 * a + b - d - g + 15) % 30;
        const i = Math.floor(c / 4);
        const k = c % 4;
        const l = (32 + 2 * e + 2 * i - h - k) % 7;
        const m = Math.floor((a + 11 * h + 22 * l) / 451);
        const month = Math.floor((h + l - 7 * m + 114) / 31); // 3 = March, 4 = April
        const day = ((h + l - 7 * m + 114) % 31) + 1;
        return new Date(year, month - 1, day);
    }

    // Halloween window: Oct 24 → Nov 1 inclusive (covers the building
    // anticipation in late October plus All Saints' Day in France). Fixed
    // calendar — no astronomical calculation needed.
    private isHalloweenSeason(today: Date): boolean {
        const year = today.getFullYear();
        const start = new Date(year, 9, 24);
        const end = new Date(year, 10, 1, 23, 59, 59);
        return today >= start && today <= end;
    }

    // Easter season for the UI: Palm Sunday (Easter - 7d, start of Holy
    // Week) → Easter Monday (Easter + 1d) inclusive.
    private isEasterSeason(today: Date): boolean {
        const year = today.getFullYear();
        const easter = EasterEggsService.computeEaster(year);
        const palmSunday = new Date(easter);
        palmSunday.setDate(easter.getDate() - 7);
        const easterMonday = new Date(easter);
        easterMonday.setDate(easter.getDate() + 1);
        easterMonday.setHours(23, 59, 59);
        return today >= palmSunday && today <= easterMonday;
    }

    private checkBirthdays(): void {
        // Compare today's month/day with each resident's birthDate or, when
        // the birth date is unknown, with their arrivalDate. The first
        // matching resident wins (so we can mention them by name).
        //
        // Honour ?theme-date= so the operator can stage a screenshot for the
        // birthday confetti effect, just like the seasonal theme overrides.
        this.residentsService.list().subscribe({
            next: (residents) => {
                const today = this.todayWithOverride();
                const md = `${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
                for (const r of residents) {
                    if (r.deathDate) continue;
                    const ref = r.birthDate ?? r.arrivalDate;
                    if (!ref) continue;
                    // ref is ISO yyyy-mm-dd; take the month-day tail.
                    if (ref.length >= 10 && ref.substring(5, 10) === md) {
                        this.birthdayActive.set(r.name);
                        return;
                    }
                }
            },
            error: () => undefined,
        });
    }
}

function pad(n: number): string {
    return n < 10 ? `0${n}` : String(n);
}
