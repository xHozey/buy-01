import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

interface UploadResponse {
  urls: string[];
}

interface SingleUploadResponse {
  id: string;
  url: string;
}

@Injectable({ providedIn: 'root' })
export class MediaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/media`;

  uploadImages(files: File[]): Observable<UploadResponse> {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));
    return this.http.post<UploadResponse>(`${this.apiUrl}/upload/multiple`, formData);
  }

  uploadImage(file: File): Observable<SingleUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<SingleUploadResponse>(`${this.apiUrl}/images`, formData);
  }
}
