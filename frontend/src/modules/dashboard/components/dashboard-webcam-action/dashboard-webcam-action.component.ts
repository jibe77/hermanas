import { Component, EventEmitter, Output } from '@angular/core';

@Component({
    selector: 'sb-dashboard-webcam-action',
    templateUrl: './dashboard-webcam-action.component.html',
    styleUrls: ['dashboard-webcam-action.component.scss'],
    standalone: false
})
export class DashboardWebcamActionComponent {
    @Output() refreshWebcamEvent = new EventEmitter();

    constructor() {}

    public refreshPicture() {
        this.refreshWebcamEvent.emit('picture');
    }
}
