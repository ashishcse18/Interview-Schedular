import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TemplateService {
  private readonly baseUrl = 'http://localhost:8081/api/templates';

  constructor(private http: HttpClient) {}

  listTemplates(): Observable<any[]> {
    return this.http.get<any[]>(this.baseUrl);
  }

  createTemplate(payload: any): Observable<any> {
    return this.http.post<any>(this.baseUrl, payload);
  }

  updateTemplate(id: number, payload: any): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/${id}`, payload);
  }

  deleteTemplate(id: number): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/${id}`);
  }
}
