import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, interval } from 'rxjs';
import { CandidateService } from '../../services/candidate.service';
import { TemplateService } from '../../services/template.service';
import { CampaignService } from '../../services/campaign.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-campaign',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './campaign.component.html',
})
export class CampaignComponent implements OnInit, OnDestroy {
  batches: any[] = [];
  templates: any[] = [];

  campaign = {
    batchId: '',
    templateId: '',
  };

  campaignStats = {
    batchId: null as number | null,
    fileName: '',
    status: '',
    totalCandidates: 0,
    pendingCount: 0,
    sentCount: 0,
    failedCount: 0,
  };

  isSending = false;
  progressBarWidth = '0%';
  private statsPollingSub: Subscription | null = null;

  constructor(
    private candidateService: CandidateService,
    private templateService: TemplateService,
    private campaignService: CampaignService,
    private toastService: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.fetchBatches();
    this.fetchTemplates();
  }

  ngOnDestroy() {
    this.stopPolling();
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
      },
    });
  }

  // Fetch templates from service
  fetchTemplates() {
    this.templateService.listTemplates().subscribe({
      next: (data) => {
        this.templates = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error fetching templates:', err);
        this.toastService.error('Failed to fetch templates.');
        this.cdr.detectChanges();
      },
    });
  }

  getSelectedTemplateContent(): string {
    if (!this.campaign.templateId) return '';
    const temp = this.templates.find((t) => t.id == this.campaign.templateId);
    return temp ? temp.content : '';
  }

  triggerCampaign() {
    const batchId = Number(this.campaign.batchId);
    const templateId = Number(this.campaign.templateId);

    if (!batchId || !templateId) return;

    this.isSending = true;
    this.cdr.detectChanges();

    this.campaignService.triggerCampaign(batchId, templateId).subscribe({
      next: (res) => {
        this.toastService.success(
          'Bulk campaign triggered in the background. Monitoring progress...',
        );

        // Show and configure campaign progress
        const selectedBatch = this.batches.find((b) => b.id == batchId);
        this.campaignStats.fileName = selectedBatch ? selectedBatch.fileName : `Batch #${batchId}`;
        this.campaignStats.batchId = batchId;
        this.campaignStats.totalCandidates = selectedBatch ? selectedBatch.totalCandidates : 0;
        this.campaignStats.pendingCount = this.campaignStats.totalCandidates;
        this.campaignStats.sentCount = 0;
        this.campaignStats.failedCount = 0;
        this.progressBarWidth = '0%';

        this.startPolling(batchId);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.toastService.error('Failed to trigger bulk campaign.');
        this.isSending = false;
        this.cdr.detectChanges();
      },
    });
  }

  private startPolling(batchId: number) {
    this.stopPolling();

    // Poll stats API every 1.5 seconds
    this.statsPollingSub = interval(1500).subscribe(() => {
      this.campaignService.getCampaignStats(batchId).subscribe({
        next: (stats) => {
          this.campaignStats = stats;

          const total = stats.totalCandidates;
          const processed = stats.sentCount + stats.failedCount;
          this.progressBarWidth = total > 0 ? `${Math.round((processed / total) * 100)}%` : '0%';

          if (stats.status === 'COMPLETED' || stats.status === 'FAILED') {
            this.stopPolling();
            this.isSending = false;
            this.toastService.info(
              `Campaign sending for Batch #${batchId} has finished processing!`,
            );
            this.fetchBatches();
          }
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Polling error:', err);
        },
      });
    });
  }

  private stopPolling() {
    if (this.statsPollingSub) {
      this.statsPollingSub.unsubscribe();
      this.statsPollingSub = null;
    }
  }
}
