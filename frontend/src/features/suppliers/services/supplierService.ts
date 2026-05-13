import api from 'services/api';
import { SupplierRequestDto, SupplierResponseDto, SupplierSimpleDto, SupplierMappingDto, SupplierMappingRequestDto } from 'src/types';

export const getSuppliers = async (): Promise<SupplierSimpleDto[]> => {
  const response = await api.get('/proveedores/list');
  return response.data.data;
};

export const getSupplierDetails = async (id: number): Promise<SupplierResponseDto> => {
  const response = await api.get(`/proveedores/${id}`);
  return response.data.data;
};

export const createSupplier = async (supplier: SupplierRequestDto): Promise<SupplierResponseDto> => {
  const response = await api.post('/proveedores', supplier);
  return response.data.data;
};

export const updateSupplier = async (id: number, supplier: SupplierRequestDto): Promise<SupplierResponseDto> => {
  const response = await api.put(`/proveedores/${id}`, supplier);
  return response.data.data;
};

export const deleteSupplier = async (id: number): Promise<void> => {
  await api.delete(`/proveedores/${id}`);
};

export const getSupplierMappings = async (supplierId: number): Promise<SupplierMappingDto[]> => {
  const response = await api.get(`/proveedores/${supplierId}/mappings`);
  return response.data.data;
};

export const createSupplierMapping = async (supplierId: number, mapping: SupplierMappingRequestDto): Promise<SupplierMappingDto> => {
  const response = await api.post(`/proveedores/${supplierId}/mappings`, mapping);
  return response.data.data;
};

export const updateSupplierMapping = async (mappingId: number, mapping: SupplierMappingRequestDto): Promise<SupplierMappingDto> => {
  const response = await api.put(`/mappings/${mappingId}`, mapping);
  return response.data.data;
};

export const deleteSupplierMapping = async (mappingId: number): Promise<void> => {
    await api.delete(`/mappings/${mappingId}`);
};
