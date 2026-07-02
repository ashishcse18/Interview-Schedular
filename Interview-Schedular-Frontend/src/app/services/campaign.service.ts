import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CampaignService {
  private readonly baseUrl = 'http://localhost:8081/api/messages';

  constructor(private http: HttpClient) {}

  triggerCampaign(batchId: number, templateId: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/send-all`, { batchId, templateId });
  }

  getCampaignStats(batchId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/stats/${batchId}`);
  }

  getLogs(batchId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/status/${batchId}`);
  }
}
