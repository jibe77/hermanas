import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
    inject,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MeteoInfo } from '@modules/dashboard/services';
import { WeatherService } from '@modules/weather/services';
import { ToastService } from '@common/services';
import { DatePipe, DecimalPipe } from '@angular/common';

interface DailyPoint {
    date: Date;
    label: string;
    tempAvg: number | null;
    humAvg: number | null;
    samples: number;
}

interface LinePoint {
    x: number;
    y: number;
    raw: number;
}

@Component({
    selector: 'hermanas-monthly-trend-chart',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './monthly-trend-chart.component.html',
    styleUrls: ['monthly-trend-chart.component.scss'],
    imports: [DatePipe, DecimalPipe],
})
export class MonthlyTrendChartComponent implements OnInit, OnChanges {
    private weatherService = inject(WeatherService);
    private toast = inject(ToastService);
    private cdr = inject(ChangeDetectorRef);

    /**
     * Optional: pass data directly to bypass the HTTP fetch (handy for tests
     * and for the dashboard reusing the same readings).
     */
    @Input() data: MeteoInfo[] | null = null;

    readonly viewBoxWidth = 760;
    readonly viewBoxHeight = 320;
    readonly padding = { top: 16, right: 56, bottom: 36, left: 48 };

    loading = false;
    points: DailyPoint[] = [];

    tempLine: LinePoint[] = [];
    humLine: LinePoint[] = [];
    tempAreaPath = '';
    tempLinePath = '';
    humLinePath = '';

    tempMin = 0;
    tempMax = 30;
    humMin = 0;
    humMax = 100;

    tempTicks: { value: number; y: number }[] = [];
    humTicks: { value: number; y: number }[] = [];
    xTicks: { x: number; label: string }[] = [];

    hoverIndex: number | null = null;

    ngOnInit(): void {
        if (this.data) {
            this.computeAggregates(this.data);
        } else {
            this.fetch();
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['data'] && this.data) {
            this.computeAggregates(this.data);
        }
    }

    private fetch(): void {
        this.loading = true;
        this.weatherService.getLastMonth().subscribe({
            next: list => {
                this.loading = false;
                this.computeAggregates(list);
            },
            error: (err: HttpErrorResponse) => {
                this.loading = false;
                this.toast.error(
                    err.error?.message || err.message || 'Cannot load history',
                    `Météo — HTTP ${err.status}`
                );
                this.cdr.markForCheck();
            },
        });
    }

    private computeAggregates(list: MeteoInfo[]): void {
        const buckets = new Map<
            string,
            { date: Date; tempSum: number; tempCount: number; humSum: number; humCount: number }
        >();

        for (const r of list ?? []) {
            if (!r.dateTime) continue;
            const d = new Date(r.dateTime);
            if (isNaN(d.getTime())) continue;
            const key = `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, '0')}-${d
                .getDate()
                .toString()
                .padStart(2, '0')}`;
            let bucket = buckets.get(key);
            if (!bucket) {
                bucket = {
                    date: new Date(d.getFullYear(), d.getMonth(), d.getDate()),
                    tempSum: 0,
                    tempCount: 0,
                    humSum: 0,
                    humCount: 0,
                };
                buckets.set(key, bucket);
            }
            const t = typeof r.temperature === 'string' ? parseFloat(r.temperature) : r.temperature;
            if (typeof t === 'number' && !isNaN(t)) {
                bucket.tempSum += t;
                bucket.tempCount += 1;
            }
            const h = r.humidity;
            if (typeof h === 'number' && !isNaN(h)) {
                bucket.humSum += h;
                bucket.humCount += 1;
            }
        }

        const sorted = Array.from(buckets.values()).sort(
            (a, b) => a.date.getTime() - b.date.getTime()
        );

        this.points = sorted.map(b => ({
            date: b.date,
            label: `${b.date.getDate().toString().padStart(2, '0')}/${(b.date.getMonth() + 1)
                .toString()
                .padStart(2, '0')}`,
            tempAvg: b.tempCount > 0 ? b.tempSum / b.tempCount : null,
            humAvg: b.humCount > 0 ? b.humSum / b.humCount : null,
            samples: Math.max(b.tempCount, b.humCount),
        }));

        this.buildScales();
        this.cdr.markForCheck();
    }

    private buildScales(): void {
        const W = this.viewBoxWidth - this.padding.left - this.padding.right;
        const H = this.viewBoxHeight - this.padding.top - this.padding.bottom;
        const n = this.points.length;

        if (n === 0) {
            this.tempLine = [];
            this.humLine = [];
            this.tempAreaPath = '';
            this.tempLinePath = '';
            this.humLinePath = '';
            this.tempTicks = [];
            this.humTicks = [];
            this.xTicks = [];
            return;
        }

        const temps = this.points.map(p => p.tempAvg).filter((v): v is number => v !== null);
        const hums = this.points.map(p => p.humAvg).filter((v): v is number => v !== null);
        const tMinRaw = temps.length ? Math.min(...temps) : 0;
        const tMaxRaw = temps.length ? Math.max(...temps) : 30;
        const hMinRaw = hums.length ? Math.min(...hums) : 0;
        const hMaxRaw = hums.length ? Math.max(...hums) : 100;

        const tempRange = niceRange(tMinRaw, tMaxRaw, 2);
        const humRange = niceRange(hMinRaw, hMaxRaw, 2);
        this.tempMin = tempRange.min;
        this.tempMax = tempRange.max;
        this.humMin = humRange.min;
        this.humMax = humRange.max;

        const xStep = n > 1 ? W / (n - 1) : 0;
        const xOf = (i: number) => this.padding.left + (n > 1 ? i * xStep : W / 2);
        const yTemp = (v: number) =>
            this.padding.top + H - ((v - this.tempMin) / (this.tempMax - this.tempMin)) * H;
        const yHum = (v: number) =>
            this.padding.top + H - ((v - this.humMin) / (this.humMax - this.humMin)) * H;

        this.tempLine = [];
        this.humLine = [];
        this.points.forEach((p, i) => {
            const x = xOf(i);
            if (p.tempAvg !== null) {
                this.tempLine.push({ x, y: yTemp(p.tempAvg), raw: p.tempAvg });
            }
            if (p.humAvg !== null) {
                this.humLine.push({ x, y: yHum(p.humAvg), raw: p.humAvg });
            }
        });

        this.tempLinePath = pathFromPoints(this.tempLine);
        this.humLinePath = pathFromPoints(this.humLine);

        if (this.tempLine.length > 0) {
            const baseline = this.padding.top + H;
            const first = this.tempLine[0];
            const last = this.tempLine[this.tempLine.length - 1];
            const interior = this.tempLine
                .map(p => `L ${p.x.toFixed(2)} ${p.y.toFixed(2)}`)
                .join(' ');
            const bl = baseline.toFixed(2);
            this.tempAreaPath = `M ${first.x.toFixed(2)} ${bl} ${interior} L ${last.x.toFixed(2)} ${bl} Z`;
        } else {
            this.tempAreaPath = '';
        }

        this.tempTicks = tempRange.ticks.map(v => ({ value: v, y: yTemp(v) }));
        this.humTicks = humRange.ticks.map(v => ({ value: v, y: yHum(v) }));

        // X ticks: ~6 evenly distributed
        const tickCount = Math.min(6, n);
        const stride = n <= 1 ? 1 : Math.max(1, Math.round((n - 1) / (tickCount - 1)));
        this.xTicks = [];
        for (let i = 0; i < n; i += stride) {
            this.xTicks.push({ x: xOf(i), label: this.points[i].label });
        }
        // Always include the last point
        if (
            this.xTicks.length === 0 ||
            this.xTicks[this.xTicks.length - 1].label !== this.points[n - 1].label
        ) {
            this.xTicks.push({ x: xOf(n - 1), label: this.points[n - 1].label });
        }
    }

    onPointerMove(event: PointerEvent, svg: SVGSVGElement): void {
        if (this.points.length === 0) return;
        const rect = svg.getBoundingClientRect();
        const ratioX = (event.clientX - rect.left) / rect.width;
        const svgX = ratioX * this.viewBoxWidth;
        const plotLeft = this.padding.left;
        const plotWidth = this.viewBoxWidth - this.padding.left - this.padding.right;
        const n = this.points.length;
        if (n === 1) {
            this.hoverIndex = 0;
        } else {
            const i = Math.round(((svgX - plotLeft) / plotWidth) * (n - 1));
            this.hoverIndex = Math.max(0, Math.min(n - 1, i));
        }
        this.cdr.markForCheck();
    }

    onPointerLeave(): void {
        this.hoverIndex = null;
        this.cdr.markForCheck();
    }

    get hoverPoint(): DailyPoint | null {
        return this.hoverIndex !== null ? this.points[this.hoverIndex] : null;
    }

    get hoverX(): number {
        if (this.hoverIndex === null || this.points.length === 0) return 0;
        const W = this.viewBoxWidth - this.padding.left - this.padding.right;
        const n = this.points.length;
        const step = n > 1 ? W / (n - 1) : 0;
        return this.padding.left + (n > 1 ? this.hoverIndex * step : W / 2);
    }

    get plotTop(): number {
        return this.padding.top;
    }
    get plotBottom(): number {
        return this.viewBoxHeight - this.padding.bottom;
    }
    get plotLeft(): number {
        return this.padding.left;
    }
    get plotRight(): number {
        return this.viewBoxWidth - this.padding.right;
    }
}

function niceRange(
    min: number,
    max: number,
    fallbackPad: number
): { min: number; max: number; ticks: number[] } {
    if (!isFinite(min) || !isFinite(max)) {
        return { min: 0, max: 100, ticks: [0, 25, 50, 75, 100] };
    }
    if (min === max) {
        min -= fallbackPad;
        max += fallbackPad;
    }
    const range = max - min;
    const rough = range / 5;
    const pow = Math.pow(10, Math.floor(Math.log10(rough)));
    const candidates = [1, 2, 2.5, 5, 10].map(c => c * pow);
    const step = candidates.find(c => c >= rough) ?? candidates[candidates.length - 1];
    const niceMin = Math.floor(min / step) * step;
    const niceMax = Math.ceil(max / step) * step;
    const ticks: number[] = [];
    for (let v = niceMin; v <= niceMax + step / 2; v += step) {
        ticks.push(Number(v.toFixed(6)));
    }
    return { min: niceMin, max: niceMax, ticks };
}

function pathFromPoints(pts: LinePoint[]): string {
    if (pts.length === 0) return '';
    return pts
        .map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(2)} ${p.y.toFixed(2)}`)
        .join(' ');
}
