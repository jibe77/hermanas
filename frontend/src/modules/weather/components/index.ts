import { HumidityPipe } from './pipe/HumidityPipe';
import { TemperaturePipe } from './pipe/TemperaturePipe';
import { WeatherTableAreaComponent } from './weather-table-area/weather-table-area.component';

export const components = [WeatherTableAreaComponent, TemperaturePipe, HumidityPipe];

export * from './weather-table-area/weather-table-area.component';
