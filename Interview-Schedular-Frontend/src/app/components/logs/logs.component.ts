import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CandidateService } from '../../services/candidate.service';
import { CampaignService } from '../../services/campaign.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './logs.component.html'
})
export class LogsComponent implements OnInit {
  batches: any[] = [];
  logs: any[] = [];
  logsBatchFilter = '';
  candidatesMap: { [key: number]: any } = {};
  isLoadingLogs = false;

  constructor(
    private candidateService: CandidateService,
    private campaignService: CampaignService,
    private toastService: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.fetchBatches();
  }

  fetchBatches() {
    this.candidateService.listBatches().subscribe({
      next: (data) => {
        this.batches = data.sort((a, b) => b.id - a.id);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error fetching batches:', err);
        this.toastService.error('Failed to fetch candidate batches.');
        this.cdr.detectChanges();
      }
    });
  }

  onLogsFilterChange() {
    const batchId = this.logsBatchFilter;
    if (!batchId) {
      this.logs = [];
      return;
    }

    this.isLoadingLogs = true;
    this.logs = [];
    this.cdr.detectChanges();

    // First load candidates to get names mapped
    this.candidateService.listCandidates(Number(batchId)).subscribe({
      next: (candidates) => {
        const cMap: { [key: number]: any } = {};
        candidates.forEach(c => cMap[c.id] = c);
        this.candidatesMap = cMap;

        // Fetch logs
        this.campaignService.getLogs(Number(batchId)).subscribe({
          next: (logsData) => {
            this.logs = logsData.sort((a, b) => b.id - a.id);
            this.isLoadingLogs = false;
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error(err);
            this.toastService.error('Failed to fetch campaign delivery logs.');
            this.isLoadingLogs = false;
            this.cdr.detectChanges();
          }
        });
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.toastService.error('Failed to fetch candidate information.');
        this.isLoadingLogs = false;
        this.cdr.detectChanges();
      }
    });
  }

  getCandidateName(candidateId: number): string {
    const candidate = this.candidatesMap[candidateId];
    return candidate ? candidate.name : `Candidate #${candidateId}`;
  }

  getCandidatePhone(candidateId: number): string {
    const candidate = this.candidatesMap[candidateId];
    return candidate ? candidate.whatsappNumber : 'N/A';
  }
}
