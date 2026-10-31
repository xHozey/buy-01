import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { MediaService } from './media-service';

describe('MediaService', () => {
  let service: MediaService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MediaService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('uploads multiple files as repeated files fields', () => {
    const files = [
      new File(['first'], 'first.png', { type: 'image/png' }),
      new File(['second'], 'second.jpg', { type: 'image/jpeg' }),
    ];
    const response = { urls: ['https://cdn.example/first.png', 'https://cdn.example/second.jpg'] };
    let actual: typeof response | undefined;

    service.uploadImages(files).subscribe((value) => (actual = value));

    const req = httpTesting.expectOne('https://localhost:8080/api/v1/media/upload/multiple');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeInstanceOf(FormData);
    const formData = req.request.body as FormData;
    expect(formData.getAll('files')).toEqual(files);
    req.flush(response);
    expect(actual).toEqual(response);
  });

  it('uploads a single file under the file field', () => {
    const file = new File(['image'], 'image.png', { type: 'image/png' });
    const response = { id: 'media-1', url: 'https://cdn.example/image.png' };
    let actual: typeof response | undefined;

    service.uploadImage(file).subscribe((value) => (actual = value));

    const req = httpTesting.expectOne('https://localhost:8080/api/v1/media/images');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeInstanceOf(FormData);
    expect((req.request.body as FormData).get('file')).toBe(file);
    req.flush(response);
    expect(actual).toEqual(response);
  });
});

