import { DecimalPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Product, ProductService } from '../../services/product-service';
import { UserContextService } from '../../services/user-context-service';

@Component({
  selector: 'app-product-detail',
  imports: [DecimalPipe, RouterLink],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.css',
})
export class ProductDetail implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly route = inject(ActivatedRoute);
  private readonly userContext = inject(UserContextService);

  readonly user = this.userContext.user;
  readonly product = signal<Product | null>(null);
  readonly selectedImage = signal('');
  readonly isLoading = signal(true);
  readonly hasError = signal(false);

  ngOnInit(): void {
    const productId = this.route.snapshot.paramMap.get('id');
    if (!productId) {
      this.isLoading.set(false);
      this.hasError.set(true);
      return;
    }

    this.productService.getProduct(productId).subscribe({
      next: (product) => {
        this.product.set(product);
        this.selectedImage.set(product.imageUrls[0] ?? '');
        this.isLoading.set(false);
      },
      error: () => {
        this.hasError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  selectImage(imageUrl: string): void {
    this.selectedImage.set(imageUrl);
  }
}
