import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CandidateService {
  private readonly baseUrl = 'http://localhost:8081/api/candidates';

  constructor(private http: HttpClient) {}

  listBatches(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/batches`);
  }

  uploadExcel(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.baseUrl}/upload`, formData);
  }

  listCandidates(batchId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}?batchId=${batchId}`);
  }

  deleteBatch(batchId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/batch/${batchId}`);
  }
}
