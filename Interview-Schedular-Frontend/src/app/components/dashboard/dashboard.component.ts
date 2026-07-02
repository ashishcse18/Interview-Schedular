import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CandidateService } from '../../services/candidate.service';
import { TemplateService } from '../../services/template.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmModalComponent } from '../confirm-modal/confirm-modal.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, ConfirmModalComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  batches: any[] = [];
  templatesCount = 0;
  
  dashboardStats = {
    batches: 0,
    candidates: 0,
    templates: 0
  };

  selectedFile: File | null = null;
  selectedFileName = '';
  isUploading = false;

  // Custom Confirm Modal state
  isDeleteModalOpen = false;
  batchIdToDelete: number | null = null;

  constructor(
    private candidateService: CandidateService,
    private templateService: TemplateService,
    private toastService: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.fetchBatches();
    this.fetchTemplatesCount();
  }

  fetchBatches() {
    this.candidateService.listBatches().subscribe({
      next: (data) => {
        this.batches = data.sort((a, b) => b.id - a.id);
        this.updateDashboardStats();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error fetching batches:', err);
        this.toastService.error('Failed to fetch batches from server.');
        this.cdr.detectChanges();
      }
    });
  }

  fetchTemplatesCount() {
    this.templateService.listTemplates().subscribe({
      next: (data) => {
        this.templatesCount = data.length;
        this.updateDashboardStats();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error fetching templates:', err);
        this.cdr.detectChanges();
      }
    });
  }

  updateDashboardStats() {
    this.dashboardStats.batches = this.batches.length;
    this.dashboardStats.templates = this.templatesCount;
    
    let total = 0;
    this.batches.forEach(b => total += b.totalCandidates);
    this.dashboardStats.candidates = total;
  }

  // --- DELETE CONFIRMATION MODAL HANDLERS ---
  openDeleteModal(batchId: number) {
    this.batchIdToDelete = batchId;
    this.isDeleteModalOpen = true;
    this.cdr.detectChanges();
  }

  closeDeleteModal() {
    this.batchIdToDelete = null;
    this.isDeleteModalOpen = false;
    this.cdr.detectChanges();
  }

  confirmDeleteBatch() {
    if (this.batchIdToDelete === null) return;
    
    const id = this.batchIdToDelete;
    this.candidateService.deleteBatch(id).subscribe({
      next: () => {
        this.toastService.success(`Batch #${id} deleted successfully.`);
        this.fetchBatches();
        this.closeDeleteModal();
      },
      error: (err) => {
        console.error(err);
        this.toastService.error('Failed to delete batch.');
        this.closeDeleteModal();
      }
    });
  }

  // --- EXCEL UPLOAD ---
  onFileChange(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      this.selectedFileName = file.name;
      this.cdr.detectChanges();
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
        this.cdr.detectChanges();
      } else {
        this.toastService.error('Please select only Excel (.xlsx, .xls) files.');
      }
    }
  }

  uploadExcel() {
    if (!this.selectedFile) return;

    this.isUploading = true;
    this.cdr.detectChanges();

    this.candidateService.uploadExcel(this.selectedFile).subscribe({
      next: (batch) => {
        this.toastService.success(`Uploaded successfully! Created Batch #${batch.id} with ${batch.totalCandidates} candidates.`);
        this.selectedFile = null;
        this.selectedFileName = '';
        this.isUploading = false;
        this.fetchBatches();
      },
      error: (err) => {
        console.error(err);
        const errMsg = err.error?.message || 'Failed to parse Excel sheet columns.';
        this.toastService.error(errMsg);
        this.isUploading = false;
        this.cdr.detectChanges();
      }
    });
  }
}
