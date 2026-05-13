import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  getSupplierDetails, 
  createSupplier, 
  updateSupplier, 
  getSupplierMappings,
  createSupplierMapping,
  updateSupplierMapping,
  deleteSupplierMapping
} from '../services/supplierService';
import { useParams, useNavigate, Link } from 'react-router-dom';
import type { AxiosError } from 'axios';
import { SupplierRequestDto, SupplierMappingRequestDto } from 'src/types';
import { useFieldErrors } from 'src/hooks/useFieldErrors';
import { FieldError } from 'src/components/form/FieldError';
import { FormSection } from 'src/components/form/FormSection';
import { FormGrid } from 'src/components/form/FormGrid';
import { FormActions } from 'src/components/form/FormActions';
import { ArrowLeft, Plus, Trash2 } from 'lucide-react';
import './SupplierFormPage.css';

/**
 * Página de formulario para crear o editar un proveedor y sus reglas de mapeo.
 * Coordina las llamadas al backend y muestra errores de validación en los campos.
 */
const SupplierFormPage = () => {
  type BackendError = { message?: string; errors?: Record<string, string> };
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isEditMode = !!id;
  const supplierId = Number(id);

  const [formData, setFormData] = useState<SupplierRequestDto>({
    name: '',
    baseUrlApi: '',
    contact: '',
    email: '',
    schedule: '',
    apiKey: '',
    active: true,
    supportsBulkSync: true,
    catalogEndpoint: '',
    detailEndpoint: '',
    searchEndpoint: '',
  });

  const [mappings, setMappings] = useState<Array<Partial<SupplierMappingRequestDto> & { mappingId?: number }>>([]);
  const [mappingsToDelete, setMappingsToDelete] = useState<number[]>([]);

  const { data: existingSupplier, isLoading: isLoadingSupplier } = useQuery({
    queryKey: ['supplier', supplierId],
    queryFn: () => getSupplierDetails(supplierId),
    enabled: isEditMode,
  });

  const { data: existingMappings, isLoading: isLoadingMappings } = useQuery({
    queryKey: ['supplierMappings', supplierId],
    queryFn: () => getSupplierMappings(supplierId),
    enabled: isEditMode,
  });

  const {
    errors,
    setFieldError,
    clearFieldError,
    clearAllErrors,
    bindField,
  } = useFieldErrors<'name' | 'baseUrlApi' | 'contact' | 'email' | 'schedule' | 'catalogEndpoint' | 'detailEndpoint' | 'searchEndpoint'>();

  useEffect(() => {
    if (isEditMode && existingSupplier) {
      setFormData({
        name: existingSupplier.name,
        baseUrlApi: existingSupplier.baseUrlApi,
        contact: existingSupplier.contact ?? '',
        email: existingSupplier.email ?? '',
        schedule: existingSupplier.schedule ?? '',
        apiKey: '', // API key is not sent back from server for security
        active: existingSupplier.active,
        supportsBulkSync: existingSupplier.supportsBulkSync,
        catalogEndpoint: existingSupplier.catalogEndpoint,
        detailEndpoint: existingSupplier.detailEndpoint,
        searchEndpoint: existingSupplier.searchEndpoint,
      });
    }
    if (isEditMode && existingMappings) {
      setMappings(existingMappings);
    }
  }, [isEditMode, existingSupplier, existingMappings]);

  const supplierUpdateMutation = useMutation({
    mutationFn: (data: { id: number, supplier: SupplierRequestDto }) => updateSupplier(data.id, data.supplier),
  });

  const supplierCreateMutation = useMutation({
    mutationFn: createSupplier,
  });
  
  const mappingCreateMutation = useMutation({
    mutationFn: (data: { supplierId: number, mapping: SupplierMappingRequestDto }) =>
      createSupplierMapping(data.supplierId, data.mapping),
  });

  const mappingUpdateMutation = useMutation({
    mutationFn: (data: { mappingId: number, mapping: SupplierMappingRequestDto }) =>
      updateSupplierMapping(data.mappingId, data.mapping),
  });

  const mappingDeleteMutation = useMutation({
    mutationFn: deleteSupplierMapping,
  });

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    const checked = (e.target as HTMLInputElement).checked;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
    clearFieldError(name as 'name' | 'baseUrlApi' | 'contact' | 'email' | 'schedule' | 'catalogEndpoint' | 'detailEndpoint' | 'searchEndpoint');
  };

  const handleMappingChange = (index: number, field: keyof SupplierMappingRequestDto, value: string) => {
    const newMappings = [...mappings];
    newMappings[index] = { ...newMappings[index], [field]: value };
    setMappings(newMappings);
  };

  const handleAddMapping = () => {
    setMappings((prev) => ([
      ...prev,
      { internalField: '', externalField: '', transformationType: 'DIRECT' },
    ]));
  };

  const handleRemoveMapping = (index: number) => {
    const mappingToRemove = mappings[index];
    if (mappingToRemove.mappingId) {
      setMappingsToDelete(prev => [...prev, mappingToRemove.mappingId!]);
    }
    const newMappings = [...mappings];
    newMappings.splice(index, 1);
    setMappings(newMappings);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      clearAllErrors();
      let currentSupplierId = supplierId;
      if (isEditMode) {
        await supplierUpdateMutation.mutateAsync({ id: supplierId, supplier: formData });
      } else {
        const newSupplier = await supplierCreateMutation.mutateAsync(formData);
        currentSupplierId = newSupplier.id;
      }

      const mappingPromises = [];

      // Deletions
      for (const mappingId of mappingsToDelete) {
        mappingPromises.push(mappingDeleteMutation.mutateAsync(mappingId));
      }

      // Creations and Updates
      for (const mapping of mappings) {
        const { ...mappingData } = mapping;
        if (mapping.mappingId) {
          // It's an update
          mappingPromises.push(
            mappingUpdateMutation.mutateAsync({
              mappingId: mapping.mappingId,
              mapping: mappingData as SupplierMappingRequestDto,
            }),
          );
        } else {
          // It's a creation
          mappingPromises.push(
            mappingCreateMutation.mutateAsync({
              supplierId: currentSupplierId,
              mapping: mappingData as SupplierMappingRequestDto,
            }),
          );
        }
      }

      await Promise.all(mappingPromises);

      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
      queryClient.invalidateQueries({ queryKey: ['supplier', currentSupplierId] });
      queryClient.invalidateQueries({ queryKey: ['supplierMappings', currentSupplierId] });

      navigate(isEditMode ? `/suppliers/${currentSupplierId}` : '/suppliers');

    } catch (error) {
      const typedError = error as AxiosError<BackendError> & Error;
      const fieldErrors = typedError.response?.data?.errors;

      if (fieldErrors && typeof fieldErrors === 'object') {
        Object.entries(fieldErrors).forEach(([field, message]) => {
          setFieldError(field as 'name' | 'baseUrlApi' | 'contact' | 'email' | 'schedule' | 'catalogEndpoint' | 'detailEndpoint' | 'searchEndpoint', String(message));
        });
      }
      // Además dejamos rastro en consola para depuración.
      console.error('Failed to save supplier and mappings', typedError);
    }
  };

  const isProcessing = 
    supplierUpdateMutation.isPending || 
    supplierCreateMutation.isPending || 
    mappingCreateMutation.isPending ||
    mappingUpdateMutation.isPending ||
    mappingDeleteMutation.isPending;

  if ((isLoadingSupplier || isLoadingMappings) && isEditMode) {
    return <div className="p-8 text-center text-white">Cargando formulario...</div>;
  }

  return (
    <div className="supplier-form-page">
      <div className="form-container">
        <div className="form-header">
          <h1 className="form-title">{isEditMode ? 'Actualizar proveedor' : 'Crear proveedor'}</h1>
          <Link to={isEditMode ? `/suppliers/${id}` : '/suppliers'} className="back-link">
            <ArrowLeft size={16} aria-hidden="true" />
            Volver
          </Link>
        </div>

        <form onSubmit={handleSubmit} className="supplier-form">
          <FormSection title="Información general">
            <FormGrid>
               <div className="form-group">
                <label htmlFor="name">Nombre</label>
                <input
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder="Nombre del proveedor"
                  required
                  className="form-input"
                  {...bindField('name')}
                />
                <FieldError field="name" message={errors.name} />
              </div>
              <div className="form-group">
                <label htmlFor="baseUrlApi">API Base URL</label>
                <input
                  id="baseUrlApi"
                  name="baseUrlApi"
                  value={formData.baseUrlApi}
                  onChange={handleInputChange}
                  placeholder="https://api.supplier.com"
                  required
                  className="form-input"
                  {...bindField('baseUrlApi')}
                />
                <FieldError field="baseUrlApi" message={errors.baseUrlApi} />
              </div>
              <div className="form-group">
                <label htmlFor="contact">Contacto</label>
                <input
                  id="contact"
                  name="contact"
                  value={formData.contact ?? ''}
                  onChange={handleInputChange}
                  placeholder="Nombre de contacto"
                  className="form-input"
                  {...bindField('contact')}
                />
                <FieldError field="contact" message={errors.contact} />
              </div>
              <div className="form-group">
                <label htmlFor="email">Email</label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  value={formData.email ?? ''}
                  onChange={handleInputChange}
                  placeholder="contacto@proveedor.com"
                  className="form-input"
                  {...bindField('email')}
                />
                <FieldError field="email" message={errors.email} />
              </div>
              <div className="form-group">
                <label htmlFor="schedule">Horario</label>
                <input
                  id="schedule"
                  name="schedule"
                  value={formData.schedule ?? ''}
                  onChange={handleInputChange}
                  placeholder="L-V 09:00-18:00"
                  className="form-input"
                  {...bindField('schedule')}
                />
                <FieldError field="schedule" message={errors.schedule} />
              </div>
              <div className="form-group">
                <label htmlFor="apiKey">API Key</label>
                <input
                  id="apiKey"
                  name="apiKey"
                  type="password"
                  value={formData.apiKey}
                  onChange={handleInputChange}
                  placeholder="Dejar en blanco para no modificar"
                  className="form-input"
                />
              </div>
              <div className="form-group">
                <label htmlFor="catalogEndpoint">Catalog Endpoint</label>
                <input
                  id="catalogEndpoint"
                  name="catalogEndpoint"
                  value={formData.catalogEndpoint}
                  onChange={handleInputChange}
                  placeholder="/catalog"
                  className="form-input"
                  {...bindField('catalogEndpoint')}
                />
                <FieldError field="catalogEndpoint" message={errors.catalogEndpoint} />
              </div>
              <div className="form-group">
                <label htmlFor="detailEndpoint">Detail Endpoint</label>
                <input
                  id="detailEndpoint"
                  name="detailEndpoint"
                  value={formData.detailEndpoint}
                  onChange={handleInputChange}
                  placeholder="/products/{id}"
                  className="form-input"
                  {...bindField('detailEndpoint')}
                />
                <FieldError field="detailEndpoint" message={errors.detailEndpoint} />
              </div>
              <div className="form-group">
                <label htmlFor="searchEndpoint">Search Endpoint</label>
                <input
                  id="searchEndpoint"
                  name="searchEndpoint"
                  value={formData.searchEndpoint}
                  onChange={handleInputChange}
                  placeholder="/search"
                  className="form-input"
                  {...bindField('searchEndpoint')}
                />
                <FieldError field="searchEndpoint" message={errors.searchEndpoint} />
              </div>
              <div className="form-group checkbox-group">
                <label>
                  <input name="active" type="checkbox" checked={formData.active} onChange={handleInputChange} />
                  Activo
                </label>
              </div>
              <div className="form-group checkbox-group">
                <label>
                  <input name="supportsBulkSync" type="checkbox" checked={formData.supportsBulkSync} onChange={handleInputChange} />
                  Soporta sincronización masiva
                </label>
              </div>
            </FormGrid>
          </FormSection>

          <FormSection title="Mappings">
            <div className="section-controls">
              <button type="button" onClick={handleAddMapping} className="btn-add-mapping">
                <Plus size={16} aria-hidden="true" />
                Añadir mapping
              </button>
            </div>

            <div className="mappings-list">
              {mappings.map((mapping, index) => (
                <div key={mapping.mappingId || `new-${index}`} className="mapping-item">
                  <input value={mapping.internalField ?? ''} onChange={(e) => handleMappingChange(index, 'internalField', e.target.value)} placeholder="Internal Field" className="form-input" />
                  <input value={mapping.externalField ?? ''} onChange={(e) => handleMappingChange(index, 'externalField', e.target.value)} placeholder="External Field" className="form-input" />
                  <select value={mapping.transformationType ?? ''} onChange={(e) => handleMappingChange(index, 'transformationType', e.target.value)} className="form-select">
                    <option value="DIRECT">DIRECT</option>
                    <option value="NESTED">NESTED</option>
                    <option value="FIND_IN_ARRAY">FIND_IN_ARRAY</option>
                    <option value="SPLIT">SPLIT</option>
                  </select>
                  <button type="button" onClick={() => handleRemoveMapping(index)} className="btn-icon-delete" title="Eliminar">
                    <Trash2 size={18} aria-hidden="true" />
                  </button>
                </div>
              ))}
              {mappings.length === 0 && (
                <div className="no-mappings-message">No hay mappings definidos. Haz clic en &quot;Añadir mapping&quot; para crear uno.</div>
              )}
            </div>
          </FormSection>

          <FormActions>
            <button type="submit" disabled={isProcessing} className="btn-primary">
              {isProcessing ? 'Guardando...' : (isEditMode ? 'Actualizar proveedor' : 'Crear proveedor')}
            </button>
          </FormActions>
        </form>
      </div>
    </div>
  );
};

export default SupplierFormPage;
