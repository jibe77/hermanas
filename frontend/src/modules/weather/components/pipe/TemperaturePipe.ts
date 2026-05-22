import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'temperature' })
export class TemperaturePipe implements PipeTransform {
    transform(temperature: number): string {
        if (temperature === -100) {
            return 'N/A';
        }
        return temperature + '°';
    }
}
