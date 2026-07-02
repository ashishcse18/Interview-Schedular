import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Subscription, interval } from 'rxjs';
import { takeWhile } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit, OnDestroy {
  // App Config
  private readonly baseUrl = 'http://localhost:8081/api';

  // Navigation State
  activeTab = 'dashboard';
  pageTitle = 'Dashboard Overview';

  // Datasets
  batches: any[] = [];
  templates: any[] = [];
  candidatesMap: { [key: number]: any } = {};

  // Form States & Upload
  selectedFile: File | null = null;
  selectedFileName = '';
  isUploading = false;
  
  newTemplate = {
    id: null as number | null,
    name: '',
    content: ''
  };

  campaign = {
    batchId: '',
    templateId: ''
  };
  
  // Stats & Progress Monitoring
  dashboardStats = {
    batches: 0,
    candidates: 0,
    templates: 0
  };

  campaignStats = {
    batchId: null as number | null,
    fileName: '',
    status: '',
    totalCandidates: 0,
    pendingCount: 0,
    sentCount: 0,
    failedCount: 0,
    deliveredCount: 0
  };

  isSending = false;
  progressBarWidth = '0%';
  private statsPollingSub: Subscription | null = null;

  // Logs View
  logsBatchFilter = '';
  logs: any[] = [];
  isLoadingLogs = false;

  // Notification Toast state
  toasts: { id: string; message: string; type: 'success' | 'error' | 'info' }[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.fetchBatches();
    this.fetchTemplates();
  }

  ngOnDestroy() {
    this.stopPolling();
  }

  // --- NAVIGATION CONTROLLER ---
  switchTab(tabId: string) {
    this.activeTab = tabId;
    switch (tabId) {
      case 'dashboard':
        this.pageTitle = 'Dashboard Overview';
        this.fetchBatches();
        break;
      case 'templates':
        this.pageTitle = 'WhatsApp Templates';
        this.fetchTemplates();
        break;
      case 'send':
        this.pageTitle = 'Launch Campaign';
        this.fetchBatches();
        this.fetchTemplates();
        break;
      case 'logs':
        this.pageTitle = 'Campaign Logs';
        this.fetchBatches();
        this.logsBatchFilter = '';
        this.logs = [];
        break;
    }
  }

  // --- DATA SERVICES (REST CLIENT) ---
  fetchBatches() {
    this.http.get<any[]>(`${this.baseUrl}/candidates/batches`).subscribe({
      next: (data) => {
        this.batches = data;
        this.updateDashboardStats();
      },
      error: (err) => {
        console.error('Error fetching batches:', err);
        this.showToast('Failed to fetch batches from server.', 'error');
      }
    });
  }

  fetchTemplates() {
    this.http.get<any[]>(`${this.baseUrl}/templates`).subscribe({
      next: (data) => {
        this.templates = data;
        this.updateDashboardStats();
      },
      error: (err) => {
        console.error('Error fetching templates:', err);
        this.showToast('Failed to fetch templates.', 'error');
      }
    });
  }

  updateDashboardStats() {
    this.dashboardStats.batches = this.batches.length;
    this.dashboardStats.templates = this.templates.length;
    
    let total = 0;
    this.batches.forEach(b => total += b.totalCandidates);
    this.dashboardStats.candidates = total;
  }

  deleteBatch(batchId: number) {
    if (confirm(`Are you sure you want to delete Batch #${batchId}? This permanently removes all candidates and message logs.`)) {
      this.http.delete(`${this.baseUrl}/candidates/batch/${batchId}`).subscribe({
        next: () => {
          this.showToast(`Batch #${batchId} deleted successfully.`, 'success');
          this.fetchBatches();
          if (this.logsBatchFilter === String(batchId)) {
            this.logsBatchFilter = '';
            this.logs = [];
          }
        },
        error: (err) => {
          console.error(err);
          this.showToast('Failed to delete batch.', 'error');
        }
      });
    }
  }

  // --- EXCEL UPLOAD ---
  onFileChange(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      this.selectedFileName = file.name;
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    const dropzone = document.getElementById('excel-dropzone');
    dropzone?.classList.add('dragover');
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    const dropzone = document.getElementById('excel-dropzone');
    dropzone?.classList.remove('dragover');
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    const dropzone = document.getElementById('excel-dropzone');
    dropzone?.classList.remove('dragover');

    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      if (file.name.endsWith('.xlsx') || file.name.endsWith('.xls')) {
        this.selectedFile = file;
        this.selectedFileName = file.name;
      } else {
        this.showToast('Please select only Excel (.xlsx, .xls) files.', 'error');
      }
    }
  }

  uploadExcel() {
    if (!this.selectedFile) return;

    this.isUploading = true;
    const formData = new FormData();
    formData.append('file', this.selectedFile);

    this.http.post<any>(`${this.baseUrl}/candidates/upload`, formData).subscribe({
      next: (batch) => {
        this.showToast(`Uploaded successfully! Created Batch #${batch.id} with ${batch.totalCandidates} candidates.`, 'success');
        this.selectedFile = null;
        this.selectedFileName = '';
        this.isUploading = false;
        this.fetchBatches();
      },
      error: (err) => {
        console.error(err);
        const errMsg = err.error?.message || 'Failed to parse Excel sheet columns.';
        this.showToast(errMsg, 'error');
        this.isUploading = false;
      }
    });
  }

  // --- TEMPLATE CRUD ---
  saveTemplate() {
    const name = this.newTemplate.name.trim();
    const content = this.newTemplate.content.trim();

    if (!name || !content) return;

    const payload = { name, content };
    const isEditing = this.newTemplate.id !== null;
    const request = isEditing 
      ? this.http.put<any>(`${this.baseUrl}/templates/${this.newTemplate.id}`, payload)
      : this.http.post<any>(`${this.baseUrl}/templates`, payload);

    request.subscribe({
      next: () => {
        this.showToast(`Template successfully ${isEditing ? 'updated' : 'created'}.`, 'success');
        this.clearTemplateForm();
        this.fetchTemplates();
      },
      error: (err) => {
        console.error(err);
        this.showToast('Failed to save template. Template name must be unique.', 'error');
      }
    });
  }

  editTemplate(template: any) {
    this.newTemplate.id = template.id;
    this.newTemplate.name = template.name;
    this.newTemplate.content = template.content;
    
    // Scroll form into view
    const formElement = document.getElementById('template-form');
    formElement?.scrollIntoView({ behavior: 'smooth' });
  }

  deleteTemplate(templateId: number, name: string) {
    if (confirm(`Are you sure you want to delete template "${name}"?`)) {
      this.http.delete(`${this.baseUrl}/templates/${templateId}`).subscribe({
        next: () => {
          this.showToast('Template deleted successfully.', 'success');
          this.fetchTemplates();
        },
        error: (err) => {
          console.error(err);
          this.showToast('Failed to delete template.', 'error');
        }
      });
    }
  }

  clearTemplateForm() {
    this.newTemplate = { id: null, name: '', content: '' };
  }

  insertPlaceholder(placeholder: string) {
    const textarea = document.getElementById('template-content') as HTMLTextAreaElement;
    if (textarea) {
      const startPos = textarea.selectionStart;
      const endPos = textarea.selectionEnd;
      const content = this.newTemplate.content;
      
      this.newTemplate.content = content.substring(0, startPos) + placeholder + content.substring(endPos);
      
      setTimeout(() => {
        textarea.focus();
        textarea.selectionStart = startPos + placeholder.length;
        textarea.selectionEnd = startPos + placeholder.length;
      }, 0);
    }
  }

  // --- CAMPAIGN SEND LOGIC ---
  getSelectedTemplateContent(): string {
    if (!this.campaign.templateId) return '';
    const temp = this.templates.find(t => t.id == this.campaign.templateId);
    return temp ? temp.content : '';
  }

  triggerCampaign() {
    const batchId = Number(this.campaign.batchId);
    const templateId = Number(this.campaign.templateId);

    if (!batchId || !templateId) return;

    this.isSending = true;
    
    const payload = { batchId, templateId };
    this.http.post<any>(`${this.baseUrl}/messages/send-all`, payload).subscribe({
      next: (res) => {
        this.showToast('Bulk campaign triggered in the background. Monitoring progress...', 'success');
        
        // Show and configure campaign progress
        const selectedBatch = this.batches.find(b => b.id == batchId);
        this.campaignStats.fileName = selectedBatch ? selectedBatch.fileName : `Batch #${batchId}`;
        this.campaignStats.batchId = batchId;
        
        this.startPolling(batchId);
      },
      error: (err) => {
        console.error(err);
        this.showToast('Failed to trigger bulk campaign.', 'error');
        this.isSending = false;
      }
    });
  }

  private startPolling(batchId: number) {
    this.stopPolling();
    
    // Poll stats API every 1.5 seconds
    this.statsPollingSub = interval(1500)
      .subscribe(() => {
        this.http.get<any>(`${this.baseUrl}/messages/stats/${batchId}`).subscribe({
          next: (stats) => {
            this.campaignStats = stats;
            
            const total = stats.totalCandidates;
            const processed = stats.sentCount + stats.failedCount + stats.deliveredCount;
            this.progressBarWidth = total > 0 ? `${Math.round((processed / total) * 100)}%` : '0%';

            if (stats.status === 'COMPLETED' || stats.status === 'FAILED') {
              this.stopPolling();
              this.isSending = false;
              this.showToast(`Campaign sending for Batch #${batchId} has finished processing!`, 'info');
              this.fetchBatches();
            }
          },
          error: (err) => {
            console.error('Polling error:', err);
          }
        });
      });
  }

  private stopPolling() {
    if (this.statsPollingSub) {
      this.statsPollingSub.unsubscribe();
      this.statsPollingSub = null;
    }
  }

  // --- LOGS SECTION LOGIC ---
  onLogsFilterChange() {
    const batchId = this.logsBatchFilter;
    if (!batchId) {
      this.logs = [];
      return;
    }

    this.isLoadingLogs = true;
    this.logs = [];

    // First load candidates to get names mapped
    this.http.get<any[]>(`${this.baseUrl}/candidates?batchId=${batchId}`).subscribe({
      next: (candidates) => {
        const cMap: { [key: number]: any } = {};
        candidates.forEach(c => cMap[c.id] = c);
        this.candidatesMap = cMap;

        // Fetch logs
        this.http.get<any[]>(`${this.baseUrl}/messages/status/${batchId}`).subscribe({
          next: (logsData) => {
            // Sort descending by log entry ID (newest first)
            this.logs = logsData.sort((a, b) => b.id - a.id);
            this.isLoadingLogs = false;
          },
          error: (err) => {
            console.error(err);
            this.showToast('Failed to fetch campaign delivery logs.', 'error');
            this.isLoadingLogs = false;
          }
        });
      },
      error: (err) => {
        console.error(err);
        this.showToast('Failed to fetch candidate information.', 'error');
        this.isLoadingLogs = false;
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

  // --- NOTIFICATION TOAST ALERT HELPERS ---
  showToast(message: string, type: 'success' | 'error' | 'info' = 'info') {
    const id = UUID();
    this.toasts.push({ id, message, type });
    
    // Auto remove in 4.5 seconds
    setTimeout(() => {
      this.toasts = this.toasts.filter(t => t.id !== id);
    }, 4500);
  }

  removeToast(id: string) {
    this.toasts = this.toasts.filter(t => t.id !== id);
  }
}

// Generate simple unique ID
function UUID(): string {
  return 'toast-' + Math.random().toString(36).substr(2, 9);
}
