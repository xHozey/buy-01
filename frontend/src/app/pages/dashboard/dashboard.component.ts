import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { PLATFORM_ID } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Product, ProductRequest, ProductService } from '../../services/product.service';

interface PendingImage {
  file: File;
  previewUrl: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly productService = inject(ProductService);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', Validators.required],
    description: ['', Validators.required],
    price: [0, [Validators.required, Validators.min(0.01)]],
    quantity: [0, [Validators.required, Validators.min(0)]],
  });
  protected products: Product[] = [];
  protected editingId: string | null = null;
  protected imageUrls: string[] = [];
  protected pendingImages: PendingImage[] = [];
  protected loading = true;
  protected saving = false;
  protected errorMessage = '';

  ngOnInit(): void {
    this.loadProducts();
  }

  ngOnDestroy(): void {
    this.revokePreviews();
  }

  protected loadProducts(): void {
    if (!this.isBrowser) {
      this.loading = false;
      return;
    }
    this.loading = true;
    this.productService.getSellerProducts().subscribe({
      next: (products) => {
        this.products = products;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Products could not be loaded. Check that you are signed in as a seller.';
        this.loading = false;
      },
    });
  }

  protected selectProduct(product: Product): void {
    this.editingId = product.id;
    this.form.setValue({
      name: product.name,
      description: product.description,
      price: product.price,
      quantity: product.quantity,
    });
    this.imageUrls = [...(product.imageUrls ?? [])];
    this.clearPendingImages();
    if (this.isBrowser) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  protected startNewProduct(): void {
    this.editingId = null;
    this.form.reset({ name: '', description: '', price: 0, quantity: 0 });
    this.imageUrls = [];
    this.clearPendingImages();
    this.errorMessage = '';
  }

  protected onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    Array.from(input.files ?? []).forEach((file) => {
      if (!file.type.startsWith('image/')) {
        this.errorMessage = 'Only image files can be attached.';
        return;
      }
      if (file.size > 2 * 1024 * 1024) {
        this.errorMessage = 'Each image must be 2 MB or smaller.';
        return;
      }
      if (this.isBrowser) {
        this.pendingImages.push({ file, previewUrl: URL.createObjectURL(file) });
      }
    });
    input.value = '';
  }

  protected removeExistingImage(index: number): void {
    this.imageUrls.splice(index, 1);
  }

  protected removePendingImage(index: number): void {
    const [removed] = this.pendingImages.splice(index, 1);
    if (removed && this.isBrowser) {
      URL.revokeObjectURL(removed.previewUrl);
    }
  }

  protected async saveProduct(): Promise<void> {
    this.errorMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    try {
      const uploadedUrls = await Promise.all(
        this.pendingImages.map(async ({ file }) => {
          const response = await firstValueFrom(this.productService.uploadImage(file));
          return this.productService.toGatewayUrl(response.url);
        }),
      );
      const request: ProductRequest = { ...this.form.getRawValue(), imageUrls: [...this.imageUrls, ...uploadedUrls] };
      const saved = this.editingId
        ? await firstValueFrom(this.productService.updateProduct(this.editingId, request))
        : await firstValueFrom(this.productService.createProduct(request));
      this.products = this.editingId
        ? this.products.map((product) => (product.id === saved.id ? saved : product))
        : [saved, ...this.products];
      this.startNewProduct();
    } catch (error: any) {
      this.errorMessage = error?.error?.message ?? 'The product could not be saved.';
    } finally {
      this.saving = false;
    }
  }

  protected deleteProduct(product: Product): void {
    if (!this.isBrowser || !window.confirm(`Delete “${product.name}”?`)) return;
    this.productService.deleteProduct(product.id).subscribe({
      next: () => {
        this.products = this.products.filter((item) => item.id !== product.id);
        if (this.editingId === product.id) this.startNewProduct();
      },
      error: () => (this.errorMessage = 'The product could not be deleted.'),
    });
  }

  protected imageSrc(url: string): string {
    return this.productService.toGatewayUrl(url);
  }

  private clearPendingImages(): void {
    this.revokePreviews();
    this.pendingImages = [];
  }

  private revokePreviews(): void {
    if (this.isBrowser) {
      this.pendingImages.forEach((image) => URL.revokeObjectURL(image.previewUrl));
    }
  }
}