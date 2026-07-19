export interface Category {
  id: string;
  name: string;
  description?: string;
  productCount: number;
}

export interface CategoryRequestDTO {
  name: string;
  description?: string;
}
