import api from 'services/api';
import { ProductDetailDto, SearchResult } from 'src/types';



export interface SyncStatus {
  status: 'IDLE' | 'IN_PROGRESS' | 'COMPLETED' | 'ERROR';
}

export const getProducts = async (
  page: number,
  size: number,
  sortBy: string,
  direction: string
): Promise<SearchResult> => {
  const effectiveSortBy = (sortBy === 'disponibilidad' || sortBy === 'availability') ? 'disponibilidadRank' : sortBy;
  const params: { [key: string]: string | number } = { page, size, sortBy: effectiveSortBy, direction };
  const response = await api.get('/productos', { params });
  return response.data.data;
};

export const searchProducts = async (
  query: string,
  page: number,
  size: number,
  sortBy: string,
  direction: string,
  categoria?: string,
  productCategory?: string,
  specsFilter?: string
): Promise<SearchResult> => {
  const effectiveSortBy = (sortBy === 'disponibilidad' || sortBy === 'availability') ? 'disponibilidadRank' : sortBy;
  const params: { [key: string]: string | number } = {
    query,
    page,
    size,
    sortBy: effectiveSortBy,
    direction,
  };

  if (categoria) {
    params.categoria = categoria;
  }
  if (productCategory) {
    params.productCategory = productCategory;
  }
  if (specsFilter) {
    params.specsFilter = specsFilter;
  }

  const response = await api.get('/productos/search', { params });
  return response.data.data;
};

export const syncAllProducts = async (): Promise<void> => {
  await api.post('/productos/sync-all');
};

export const getSyncStatus = async (): Promise<SyncStatus> => {
  const response = await api.get('/productos/sync-status');
  return response.data.data;
};

export const getProductDetails = async (id: number): Promise<ProductDetailDto> => {
  const response = await api.get(`/productos/${id}`);
  return response.data.data;
};
