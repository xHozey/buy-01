import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

interface UploadResponse {
  urls: string[];
}

@Injectable({ providedIn: 'root' })
export class MediaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/v1/media';

  uploadImages(files: File[]): Observable<UploadResponse> {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));
    return this.http.post<UploadResponse>(`${this.apiUrl}/upload/multiple`, formData);
  }
}