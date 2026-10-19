import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  userId: string;
  imageUrls: string[];
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  quantity: number;
  imageUrls: string[];
}

export interface UploadedImage {
  id: string;
  url: string;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly apiUrl = 'http://localhost:8080/api/v1';

  private headers(): HttpHeaders {
    const token = this.authService.getToken();
    return token ? new HttpHeaders({ Authorization: token }) : new HttpHeaders();
  }

  getProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/products`, { headers: this.headers() });
  }

  getSellerProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/products/mine`, { headers: this.headers() });
  }

  createProduct(product: ProductRequest): Observable<Product> {
    return this.http.post<Product>(`${this.apiUrl}/products`, product, { headers: this.headers() });
  }

  updateProduct(id: string, product: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/products/${id}`, product, { headers: this.headers() });
  }

  deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/products/${id}`, { headers: this.headers() });
  }

  uploadImage(file: File): Observable<UploadedImage> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<UploadedImage>(`${this.apiUrl}/media/images`, formData, { headers: this.headers() });
  }

  toGatewayUrl(url: string): string {
    return url.startsWith('http') ? url : `${this.apiUrl.replace('/api/v1', '')}${url}`;
  }
}