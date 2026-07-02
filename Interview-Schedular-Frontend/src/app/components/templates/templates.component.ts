import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TemplateService } from '../../services/template.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmModalComponent } from '../confirm-modal/confirm-modal.component';

@Component({
  selector: 'app-templates',
  standalone: true,
  imports: [CommonModule, FormsModule, ConfirmModalComponent],
  templateUrl: './templates.component.html'
})
export class TemplatesComponent implements OnInit {
  templates: any[] = [];
  
  newTemplate = {
    id: null as number | null,
    name: '',
    content: ''
  };

  originalTemplate = {
    name: '',
    content: ''
  };

  // Custom Confirm Modal state
  isDeleteModalOpen = false;
  templateIdToDelete: number | null = null;
  templateNameToDelete = '';

  // Autocomplete suggestions properties
  availableVariables = ['candidateName', 'companyName', 'role', 'panelTiming', 'gmeetLink', 'interviewerName'];
  showSuggestions = false;
  filteredSuggestions: string[] = [];
  activeSuggestionIndex = 0;
  triggerIndex = -1;

  constructor(
    private templateService: TemplateService,
    private toastService: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.fetchTemplates();
  }

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
      }
    });
  }

  saveTemplate() {
    const name = this.newTemplate.name.trim();
    const content = this.newTemplate.content.trim();

    if (!name || !content) return;

    const isEditing = this.newTemplate.id !== null;

    if (isEditing) {
      if (name === this.originalTemplate.name.trim() && content === this.originalTemplate.content.trim()) {
        this.toastService.error('update or change some text');
        return;
      }
    }

    const payload = { name, content };
    
    const request = isEditing 
      ? this.templateService.updateTemplate(this.newTemplate.id!, payload)
      : this.templateService.createTemplate(payload);

    request.subscribe({
      next: () => {
        this.toastService.success(`Template successfully ${isEditing ? 'updated' : 'created'}.`);
        this.clearTemplateForm();
        this.fetchTemplates();
      },
      error: (err) => {
        console.error(err);
        this.toastService.error('Failed to save template. Template name must be unique.');
        this.cdr.detectChanges();
      }
    });
  }

  editTemplate(template: any) {
    this.newTemplate.id = template.id;
    this.newTemplate.name = template.name;
    this.newTemplate.content = template.content;
    this.originalTemplate.name = template.name;
    this.originalTemplate.content = template.content;
    this.cdr.detectChanges();
    
    // Scroll form into view
    const formElement = document.getElementById('template-form');
    formElement?.scrollIntoView({ behavior: 'smooth' });
  }

  openDeleteModal(templateId: number, name: string) {
    this.templateIdToDelete = templateId;
    this.templateNameToDelete = name;
    this.isDeleteModalOpen = true;
    this.cdr.detectChanges();
  }

  closeDeleteModal() {
    this.templateIdToDelete = null;
    this.templateNameToDelete = '';
    this.isDeleteModalOpen = false;
    this.cdr.detectChanges();
  }

  confirmDeleteTemplate() {
    if (this.templateIdToDelete === null) return;
    
    const id = this.templateIdToDelete;
    this.templateService.deleteTemplate(id).subscribe({
      next: () => {
        this.toastService.success('Template deleted successfully.');
        this.fetchTemplates();
        this.closeDeleteModal();
      },
      error: (err) => {
        console.error(err);
        this.toastService.error('Failed to delete template.');
        this.closeDeleteModal();
      }
    });
  }

  clearTemplateForm() {
    this.newTemplate = { id: null, name: '', content: '' };
    this.originalTemplate = { name: '', content: '' };
    this.cdr.detectChanges();
  }

  insertPlaceholder(placeholder: string) {
    const textarea = document.getElementById('template-content') as HTMLTextAreaElement;
    if (textarea) {
      const startPos = textarea.selectionStart;
      const endPos = textarea.selectionEnd;
      const content = this.newTemplate.content;
      
      this.newTemplate.content = content.substring(0, startPos) + placeholder + content.substring(endPos);
      this.cdr.detectChanges();
      
      setTimeout(() => {
        textarea.focus();
        textarea.selectionStart = startPos + placeholder.length;
        textarea.selectionEnd = startPos + placeholder.length;
        this.cdr.detectChanges();
      }, 0);
    }
  }

  onTextareaInput(event: Event) {
    const textarea = event.target as HTMLTextAreaElement;
    const value = textarea.value;
    const caretPos = textarea.selectionStart;

    const textBeforeCaret = value.substring(0, caretPos);
    const lastDoubleBrace = textBeforeCaret.lastIndexOf('{{');

    if (lastDoubleBrace !== -1 && lastDoubleBrace >= textBeforeCaret.lastIndexOf('}}')) {
      const query = textBeforeCaret.substring(lastDoubleBrace + 2);
      if (!query.includes(' ') && !query.includes('}')) {
        this.filteredSuggestions = this.availableVariables
          .filter(v => v.toLowerCase().includes(query.toLowerCase()))
          .map(v => '{{' + v + '}}');

        if (this.filteredSuggestions.length > 0) {
          this.showSuggestions = true;
          this.triggerIndex = lastDoubleBrace;
          if (this.activeSuggestionIndex >= this.filteredSuggestions.length) {
            this.activeSuggestionIndex = 0;
          }
          this.cdr.detectChanges();
          return;
        }
      }
    }

    this.showSuggestions = false;
    this.cdr.detectChanges();
  }

  onTextareaKeyDown(event: KeyboardEvent) {
    if (!this.showSuggestions) return;

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.activeSuggestionIndex = (this.activeSuggestionIndex + 1) % this.filteredSuggestions.length;
      this.cdr.detectChanges();
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.activeSuggestionIndex = (this.activeSuggestionIndex - 1 + this.filteredSuggestions.length) % this.filteredSuggestions.length;
      this.cdr.detectChanges();
    } else if (event.key === 'Enter') {
      event.preventDefault();
      this.selectSuggestion(this.filteredSuggestions[this.activeSuggestionIndex]);
    } else if (event.key === 'Escape') {
      event.preventDefault();
      this.showSuggestions = false;
      this.cdr.detectChanges();
    }
  }

  selectSuggestion(suggestion: string) {
    const textarea = document.getElementById('template-content') as HTMLTextAreaElement;
    if (textarea) {
      const value = this.newTemplate.content;
      const caretPos = textarea.selectionStart;
      const before = value.substring(0, this.triggerIndex);
      const after = value.substring(caretPos);

      this.newTemplate.content = before + suggestion + after;
      this.showSuggestions = false;
      this.cdr.detectChanges();

      setTimeout(() => {
        textarea.focus();
        const newCursorPos = this.triggerIndex + suggestion.length;
        textarea.selectionStart = newCursorPos;
        textarea.selectionEnd = newCursorPos;
        this.cdr.detectChanges();
      }, 0);
    }
  }
}
