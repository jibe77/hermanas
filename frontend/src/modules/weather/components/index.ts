import { NgBootstrapTableComponent } from './ng-bootstrap-table/ng-bootstrap-table.component';
import { HumidityPipe } from './pipe/HumidityPipe';
import { TemperaturePipe } from './pipe/TemperaturePipe';
import { SortIconComponent } from './sort-icon/sort-icon.component';
import { WeatherTableAreaComponent } from './weather-table-area/weather-table-area.component';

export const components = [
    NgBootstrapTableComponent,
    SortIconComponent,
    WeatherTableAreaComponent,
    TemperaturePipe,
    HumidityPipe,
];

export * from './ng-bootstrap-table/ng-bootstrap-table.component';
export * from './sort-icon/sort-icon.component';
export * from './weather-table-area/weather-table-area.component';
