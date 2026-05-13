import { Link } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { useCart } from '../context/useCart';
import { exportPurchaseOrder } from '@/utils/exportPurchaseOrder';
import { formatCurrency } from '@/utils/formatting';
import { ArrowRight, Download, FileText, ShoppingCart, Trash2 } from 'lucide-react';
import './CartPage.css';

/**
 * Página de carrito: muestra los productos añadidos, permite ajustar cantidades,
 * seleccionar qué se va a comprar y exportar la orden a Excel.
 */
const CartPage = () => {
  const { items, removeItem, toggleItemSelection, totalPrice, updateItemQuantity } = useCart();

  const selectedItems = items.filter((item) => item.selected);
  const selectedCount = selectedItems.length;
  const selectedTotal = selectedItems.reduce(
    (acc, item) => acc + item.unitPrice * item.quantity,
    0,
  );

  /**
   * Genera un Excel de orden de compra solo con los ítems seleccionados.
   */
  const handleExportToExcel = async () => {
    if (selectedCount === 0) {
      toast.error('No hay productos seleccionados para exportar.');
      return;
    }

    try {
      await exportPurchaseOrder(selectedItems);
      toast.success('Orden de compra generada con éxito.');
    } catch (err) {
      console.error(err);
      toast.error('Error al generar el Excel. Revisa la consola.');
    }
  };

  const hasItems = items.length > 0;
  const hasSelected = selectedCount > 0;

  return (
    <div className="carrito-container">
      <div className="cart-header">
        <div>
          <div className="carrito-title-wrap">
            <ShoppingCart size={24} className="carrito-title-icon" aria-hidden="true" />
            <h1 className="carrito-title">Carrito de la Compra</h1>
          </div>
          <p className="cart-subtitle">Revisa cantidades, selecciona productos y exporta tu orden de compra.</p>
        </div>

        {hasItems && (
          <div className="cart-metrics">
            <div className="cart-metric">
              <span className="cart-metric-label">Productos totales</span>
              <span className="cart-metric-value">{items.length}</span>
            </div>
            <div className="cart-metric">
              <span className="cart-metric-label">Seleccionados</span>
              <span className="cart-metric-value">{selectedCount}</span>
            </div>
            <div className="cart-metric">
              <span className="cart-metric-label">Total seleccionado</span>
              <span className="cart-metric-value">
                {hasSelected ? formatCurrency(selectedTotal) : '—'}
              </span>
            </div>
          </div>
        )}
      </div>

      <div className={`carrito-content ${!hasItems ? 'carrito-vacio-grid' : ''}`}>
        <div className="carrito-items">
          {!hasItems ? (
            <div className="carrito-vacio">
              <p>Tu carrito está vacío.</p>
              <Link to="/" className="seguir-comprando">
                <ArrowRight size={16} aria-hidden="true" />
                Continuar comprando
              </Link>
            </div>
          ) : (
            items.map((item) => (
              <div
                key={item.id}
                className={`carrito-item ${!item.selected ? 'deseleccionado' : ''}`}
              >
                <div className="carrito-item-checkbox">
                  <input
                    type="checkbox"
                    checked={item.selected}
                    onChange={() => toggleItemSelection(item.id)}
                    title="Seleccionar para comprar"
                  />
                </div>
                <div className="carrito-item-detalles">
                  <h3 className="carrito-item-nombre">{item.productName}</h3>
                  <div className="carrito-item-linea-secundaria">
                    <p className="carrito-item-proveedor">
                      Proveedor: {item.supplierName}
                    </p>
                    <div className="control-cantidad">
                      <button
                        onClick={() =>
                          updateItemQuantity(item.id, item.quantity - 1)
                        }
                        disabled={item.quantity <= 1}
                      >
                        -
                      </button>
                      <input
                        type="number"
                        value={item.quantity}
                        onChange={(e) =>
                          updateItemQuantity(
                            item.id,
                            parseInt(e.target.value, 10) || 1,
                          )
                        }
                        onBlur={(e) => {
                          if (parseInt(e.target.value, 10) < 1) {
                            updateItemQuantity(item.id, 1);
                          }
                        }}
                      />
                      <button
                        onClick={() =>
                          updateItemQuantity(item.id, item.quantity + 1)
                        }
                        disabled={item.quantity >= item.stock}
                      >
                        +
                      </button>
                    </div>
                    <div className="carrito-item-precio">
                      {formatCurrency(item.unitPrice * item.quantity)}
                    </div>
                  </div>
                </div>
                <button
                  className="boton-eliminar"
                  onClick={() => removeItem(item.id)}
                  title="Eliminar producto"
                >
                  <Trash2 size={18} aria-hidden="true" />
                </button>
              </div>
            ))
          )}
        </div>

        {hasItems && (
          <div className="carrito-resumen">
            <h2 className="summary-title">
              <FileText size={20} aria-hidden="true" />
              Resumen del Pedido
            </h2>

            {!hasSelected ? (
              <p className="summary-empty-message">
                No hay productos seleccionados. Marca al menos uno para incluirlo en la
                orden de compra.
              </p>
            ) : (
              <div className="resumen-items">
                {selectedItems.map((item) => (
                  <div className="resumen-fila" key={item.id}>
                    <span className="resumen-item-nombre">
                      {item.productName}{' '}
                      <span className="resumen-item-cantidad">
                        (x{item.quantity})
                      </span>
                    </span>
                    <span>{formatCurrency(item.unitPrice * item.quantity)}</span>
                  </div>
                ))}
              </div>
            )}

            <div className="resumen-fila total">
              <span>Total carrito</span>
              <span>{formatCurrency(totalPrice)}</span>
            </div>

            <button
              className="boton-tramitar"
              onClick={handleExportToExcel}
              disabled={!hasSelected}
              title={
                hasSelected
                  ? 'Generar y descargar un archivo Excel con los productos seleccionados'
                  : 'Selecciona al menos un producto para poder exportar la orden de compra'
              }
            >
              <Download size={18} aria-hidden="true" />
              <span>Generar Excel</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default CartPage;

