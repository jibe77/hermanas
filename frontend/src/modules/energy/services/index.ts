import { ChartsService } from './charts.service';

// EnergyService is providedIn: 'root' — exported via energy.service.ts and not
// listed here on purpose, otherwise the EnergyModule providers array would create
// a second instance.
export const services = [ChartsService];

export * from './charts.service';
export * from './energy.service';
