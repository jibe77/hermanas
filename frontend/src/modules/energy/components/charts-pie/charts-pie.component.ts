import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    ViewChild,
} from '@angular/core';
import { Chart } from 'chart.js';

@Component({
    selector: 'sb-charts-pie',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './charts-pie.component.html',
    styleUrls: ['charts-pie.component.scss'],
    standalone: false
})
export class ChartsPieComponent implements AfterViewInit {
    @ViewChild('myPieChart') myPieChart!: ElementRef<HTMLCanvasElement>;
    chart: any;

    constructor() {}

    ngAfterViewInit() {
        this.chart = new Chart(this.myPieChart.nativeElement, {
            type: 'pie',
            data: {
                labels: ['Blue', 'Red', 'Yellow', 'Green'],
                datasets: [
                    {
                        data: [12.21, 15.58, 11.25, 8.32],
                        backgroundColor: ['#007bff', '#dc3545', '#ffc107', '#28a745'],
                    },
                ],
            },
        });
    }
}
