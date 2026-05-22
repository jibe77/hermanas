import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'humidity' })
export class HumidityPipe implements PipeTransform {
    transform(temperature: number): string {
        if (temperature === -100) {
            return 'N/A';
        }
        return temperature + '%';
    }
}
