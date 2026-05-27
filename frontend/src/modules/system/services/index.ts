import { ButtonStatusService } from './button-status.service';
import { CountryService } from './country.service';
import { DiskUsageService } from './disk-usage.service';
import { EmailTestService } from './email-test.service';
import { TablesService } from './tables.service';
import { VersionService } from './version.service';

export const services = [
    TablesService,
    CountryService,
    VersionService,
    ButtonStatusService,
    EmailTestService,
    DiskUsageService,
];

export * from './tables.service';
export * from './country.service';
export * from './version.service';
export * from './button-status.service';
export * from './email-test.service';
export * from './disk-usage.service';
