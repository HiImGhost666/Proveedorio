import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getSupplierDetails, deleteSupplier, getSupplierMappings } from '../services/supplierService';
import { useParams, Link, useNavigate } from 'react-router-dom';

import './SupplierDetailPage.css';
import { Badge } from '../../../components/badge/Badge';
import { SupplierMappingDto } from 'src/types';

const SupplierDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const supplierId = Number(id);

  const { data: supplier, isLoading, isError } = useQuery({
    queryKey: ['supplier', supplierId],
    queryFn: () => getSupplierDetails(supplierId),
    enabled: !!supplierId,
  });

  const { data: mappings, isLoading: isLoadingMappings } = useQuery<SupplierMappingDto[]>({
    queryKey: ['supplierMappings', supplierId],
    queryFn: () => getSupplierMappings(supplierId),
    enabled: !!supplierId,
  });

  const deleteMutation = useMutation({
    mutationFn: deleteSupplier,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
      navigate('/suppliers');
    },
  });

  const handleDelete = () => {
    if (window.confirm('¿Seguro que deseas eliminar este proveedor?')) {
      deleteMutation.mutate(supplierId);
    }
  };

  if (isLoading) {
    return <div className="p-8 text-center text-white">Cargando detalles del proveedor...</div>;
  }

  if (isError) {
    return <div className="p-8 text-center text-red-500">Error al cargar los detalles del proveedor.</div>;
  }

  return (
    <div className="supplier-detail-page">
      <div className="detail-container">
        <div className="detail-header">
          <h1 className="detail-title">{supplier?.name}</h1>
          <Link to="/suppliers" className="back-link">← Volver al listado</Link>
        </div>

        <div className="info-card">
          <h2 className="card-title">Información general</h2>
          <div className="info-grid">
            <div>
              <p className="info-label">Contacto</p>
              <p className="info-value">{supplier?.contact || '—'}</p>
            </div>
            <div>
              <p className="info-label">Email</p>
              <p className="info-value">{supplier?.email || '—'}</p>
            </div>
            <div>
              <p className="info-label">Teléfono</p>
              <p className="info-value">{supplier?.phone || '—'}</p>
            </div>
            <div>
              <p className="info-label">País</p>
              <p className="info-value">{supplier?.country || '—'}</p>
            </div>
            <div>
              <p className="info-label">Moneda</p>
              <p className="info-value" style={{ fontWeight: 600, color: 'var(--accent)' }}>
                {supplier?.defaultCurrency || '—'}
              </p>
            </div>
            <div>
              <p className="info-label">Horario</p>
              <p className="info-value">{supplier?.schedule || '—'}</p>
            </div>
            <div>
              <p className="info-label">Sitio web</p>
              <p className="info-value">
                {supplier?.website
                  ? <a href={supplier.website} target="_blank" rel="noreferrer" style={{ color: 'var(--accent)' }}>{supplier.website}</a>
                  : '—'}
              </p>
            </div>
            <div>
              <p className="info-label">Estado</p>
              <Badge variant={supplier?.active ? 'success' : 'error'} className="mt-1">
                {supplier?.active ? 'Activo' : 'Inactivo'}
              </Badge>
            </div>
            <div style={{ gridColumn: '1 / -1', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <p><span className="info-label">API Base URL:</span> <code className="code-snippet" style={{ marginLeft: '0.5rem' }}>{supplier?.baseUrlApi}</code></p>
              <p><span className="info-label">Bulk Sync:</span> <span style={{ marginLeft: '0.5rem' }}>{supplier?.supportsBulkSync ? 'Soportado' : 'No soportado'}</span></p>
              <p><span className="info-label">Catalog Endpoint:</span> <code className="code-snippet">{supplier?.catalogEndpoint}</code></p>
              <p><span className="info-label">Detail Endpoint:</span> <code className="code-snippet">{supplier?.detailEndpoint}</code></p>
              <p><span className="info-label">Search Endpoint:</span> <code className="code-snippet">{supplier?.searchEndpoint}</code></p>
            </div>
          </div>
        </div>

        <div className="info-card">
          <h2 className="card-title">¿Para qué sirven los Mappings?</h2>
          <p className="info-value" style={{ fontSize: '0.95rem', fontWeight: 400, color: 'var(--text-secondary)', lineHeight: '1.6', margin: 0 }}>
            Los <strong>mappings (mapeos)</strong> permiten referenciar y traducir los nombres de los campos que utiliza este proveedor en su API, 
            a los nombres de los campos internos de nuestro sistema. De esta forma, podemos unificar el 
            formato de los productos de catálogo importando todos bajo un mismo estándar, independientemente del proveedor.
          </p>
        </div>

        <div className="info-card">
          <h2 className="card-title">Guía rápida para configurar mappings</h2>
          <details style={{ width: '100%' }} open>
            <summary style={{ cursor: 'pointer', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.75rem' }}>
              Cómo mapear correctamente los campos de la API del proveedor
            </summary>
            <div style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.7, display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <p style={{ margin: 0 }}>
                Cada fila de mapping conecta un <strong>campo interno</strong> de nuestro modelo con un <strong>campo externo</strong> del JSON del proveedor.
              </p>
              <ul style={{ margin: 0, paddingLeft: '1.1rem' }}>
                <li>
                  <strong>Internal Field</strong>: nombre canonizado que usa el backend (ej.: <code>price</code>, <code>stock</code>, <code>spec_socket</code>).
                </li>
                <li>
                  <strong>External Field</strong>: ruta exacta dentro del JSON del proveedor. Puede ser:
                  <ul style={{ marginTop: '0.25rem', paddingLeft: '1.1rem' }}>
                    <li><strong>Plano (DIRECT)</strong>: una clave simple (ej.: <code>price</code>, <code>stock_level</code>).</li>
                    <li><strong>Jerárquico (NESTED)</strong>: usando notación de puntos (ej.: <code>commercial.price_data.amount</code>).</li>
                    <li><strong>Array clave-valor (FIND_IN_ARRAY)</strong>: patrón <code>attributes[key=socket]</code>, donde se busca en el array el elemento cuya propiedad <code>key</code> sea <code>socket</code> y se toma su <code>val</code>.</li>
                    <li><strong>Texto con posiciones (SPLIT)</strong>: patrón <code>feat[0]</code>, <code>feat[1]</code>... para extraer la posición 0, 1, etc. de una lista separada por <code>;</code> (ej.: <code>feat = "E-ATX;DDR5;USB4"</code>).</li>
                  </ul>
                </li>
                <li>
                  <strong>Transformation</strong>: selecciona cómo debe leer el backend el campo externo. Solo se admiten:
                  <code style={{ marginLeft: '0.25rem' }}>DIRECT</code>, <code>NESTED</code>, <code>FIND_IN_ARRAY</code> y <code>SPLIT</code>.
                </li>
              </ul>
              <p style={{ margin: '0.5rem 0 0 0' }}>
                Recomendación: revisa primero un ejemplo de respuesta JSON del proveedor y, a partir de ahí,
                define los <strong>internalField</strong> que deseas poblar y las rutas <strong>externalField</strong> adecuadas,
                usando el tipo de transformación que corresponda.
              </p>
            </div>
          </details>
        </div>

        <div className="info-card">
          <h2 className="card-title">Mappings configurados</h2>
          <div style={{ marginBottom: '1.5rem', padding: '1rem', backgroundColor: 'var(--bg-primary)', borderRadius: 'var(--radius-md)', border: '1px solid var(--bg-tertiary)' }}>
              <p style={{ margin: '0 0 0.5rem 0', fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.9rem' }}>Detalle de las columnas:</p>
            <ul style={{ margin: 0, paddingLeft: '1.2rem', color: 'var(--text-secondary)', fontSize: '0.875rem', lineHeight: '1.6' }}>
              <li><strong>Internal Field:</strong> El campo destino en nuestra base de datos (ej. precio, stock).</li>
              <li><strong>External Field:</strong> El nombre del campo original tal como lo devuelve la API externa del proveedor.</li>
              <li><strong>Transformation:</strong> Regla o conversión que se le aplica al dato externo antes de guardarlo (ej. conversiones numéricas o de fechas).</li>
            </ul>
          </div>
          <div className="table-container">
              {isLoadingMappings ? (
              <p>Cargando mappings...</p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Internal Field</th>
                    <th>External Field</th>
                    <th>Transformation</th>
                  </tr>
                </thead>
                <tbody>
                  {mappings?.map((mapping) => (
                    <tr key={mapping.mappingId}>
                      <td className="mapping-internal-field">{mapping.internalField}</td>
                      <td className="mapping-external-field">{mapping.externalField}</td>
                      <td>{mapping.transformationType}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        <div className="action-buttons">
          <Link
            to={`/suppliers/${id}/edit`}
            className="btn-action btn-update"
          >
            Actualizar
          </Link>
          <button
            onClick={handleDelete}
            disabled={deleteMutation.isPending}
            className="btn-action btn-delete"
          >
            {deleteMutation.isPending ? 'Eliminando...' : 'Eliminar'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default SupplierDetailPage;
