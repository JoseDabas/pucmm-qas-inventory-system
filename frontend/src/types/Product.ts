export interface Product {
  id: string;
  name: string;
  skuCode: string;
  description: string;
  category: string;
  price: number;
  initialQuantity: number;
  minimumStock: number;
  stockActual: number;
  isActive: boolean;
}

export interface ProductRequestDTO {
  name: string;
  skuCode: string;
  description: string;
  category: string;
  price: number;
  initialQuantity: number;
  minimumStock: number;
  isActive: boolean;
}
