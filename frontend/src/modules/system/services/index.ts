import { CountryService } from './country.service';
import { TablesService } from './tables.service';
import { VersionService } from './version.service';

export const services = [TablesService, CountryService, VersionService];

export * from './tables.service';
export * from './country.service';
export * from './version.service';
