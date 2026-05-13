import { useState, ReactNode, useMemo, useEffect } from 'react';
import { useAuth } from 'features/auth/context/AuthContext';
import { CartContext } from './CartContextStore';

/**
 * Representa un ítem del carrito normalizado y listo para usar en la UI.
 * Incluye identificador compuesto producto-proveedor y campos opcionales para exportación a Excel.
 */
export interface CartItem {
  id: string; // Composite ID, e.g., `${productId}-${providerId}`
  productId: number;
  supplierId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  selected: boolean;
  supplierName: string;
  stock: number;
  // Optional commercial fields kept for Excel / UI
  mpn?: string;
  pvpPrice?: number;
  unit?: string;
  productUrl?: string;
  supplierRef?: string;
  notes?: string;
}

/**
 * Datos mínimos necesarios para añadir un ítem al carrito.
 * Se normalizan internamente al tipo `CartItem` (IDs, stock y cantidades).
 */
export interface AddCartItem {
  productId: number;
  supplierId: number;
  productName: string;
  supplierName: string;
  unitPrice: number;
  quantity: number;
  stock: number;
  mpn?: string;
  pvpPrice?: number;
  unit?: string;
  productUrl?: string;
  supplierRef?: string;
  notes?: string;
}

/**
 * Resultado de intentar añadir un producto al carrito.
 * Indica si se añadió, se actualizó la cantidad o se alcanzó el stock máximo.
 */
export interface AddCartItemResult {
  status: 'added' | 'updated' | 'max-stock-reached';
  quantity: number;
  maxQuantity: number;
}

/**
 * Contrato del contexto del carrito disponible vía `useCart`.
 */
export interface CartContextType {
  items: CartItem[];
  addItem: (itemToAdd: AddCartItem) => AddCartItemResult;
  removeItem: (id: string) => void;
  updateItemQuantity: (id: string, newQuantity: number) => void;
  toggleItemSelection: (id: string) => void;
  totalPrice: number;
  itemCount: number;
}

const getCartStorageKey = (username: string | null): string =>
  username ? `inventory-cart-items:${username}` : 'inventory-cart-items:guest';

/**
 * Normaliza un ítem cargado de localStorage para garantizar tipos y rangos válidos.
 * Devuelve `null` si faltan campos críticos (id, productId, supplierId).
 */
const normalizeCartItem = (item: Partial<CartItem>): CartItem | null => {
  if (!item.id || typeof item.productId !== 'number' || typeof item.supplierId !== 'number') {
    return null;
  }

  const stock = Math.max(0, Number(item.stock) || 0);
  const maxQuantity = Math.max(1, stock || 1);
  const quantity = Math.max(1, Math.min(Number(item.quantity) || 1, maxQuantity));

  return {
    id: String(item.id),
    productId: item.productId,
    supplierId: item.supplierId,
    productName: item.productName ?? '',
    unitPrice: Number(item.unitPrice) || 0,
    quantity,
    selected: item.selected ?? true,
    supplierName: item.supplierName ?? '',
    stock,
    mpn: item.mpn,
    pvpPrice: item.pvpPrice,
    unit: item.unit,
    productUrl: item.productUrl,
    supplierRef: item.supplierRef,
    notes: item.notes,
  };
};

/**
 * Carga el carrito persistido desde localStorage y lo normaliza.
 * Se usa como estado inicial perezoso para hidratar el carrito al montar la app.
 */
const loadStoredItems = (storageKey: string): CartItem[] => {
  if (typeof window === 'undefined') {
    return [];
  }

  try {
    const rawItems = window.localStorage.getItem(storageKey);

    if (!rawItems) {
      return [];
    }

    const parsedItems = JSON.parse(rawItems);

    if (!Array.isArray(parsedItems)) {
      return [];
    }

    return parsedItems
      .map((item) => normalizeCartItem(item))
      .filter((item): item is CartItem => item !== null);
  } catch {
    return [];
  }
};

/**
 * Proveedor principal del carrito; expone estado y acciones y persiste en localStorage.
 */
export const CartProvider = ({ children }: { children: ReactNode }) => {
  const { username } = useAuth();
  const storageKey = useMemo(() => getCartStorageKey(username ?? null), [username]);

  const [items, setItems] = useState<CartItem[]>(() => loadStoredItems(storageKey));

  // Cuando cambia el usuario (login/logout), recargar el carrito asociado a ese usuario.
  useEffect(() => {
    setItems(loadStoredItems(storageKey));
  }, [storageKey]);

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    window.localStorage.setItem(storageKey, JSON.stringify(items));
  }, [items, storageKey]);

  /**
   * Añade un producto al carrito o incrementa su cantidad respetando el stock disponible.
   */
  const addItem = (itemToAdd: AddCartItem): AddCartItemResult => {
    const newItemId = `${itemToAdd.productId}-${itemToAdd.supplierId}`;
    const normalizedStock = Math.max(0, itemToAdd.stock);
    const requestedQuantity = Math.max(1, itemToAdd.quantity);
    let result: AddCartItemResult = {
      status: 'added',
      quantity: 0,
      maxQuantity: normalizedStock,
    };

    if (normalizedStock <= 0) {
      return {
        status: 'max-stock-reached',
        quantity: 0,
        maxQuantity: 0,
      };
    }

    setItems((prev) => {
      const existingItem = prev.find((item) => item.id === newItemId);

      if (existingItem) {
        const nextQuantity = Math.min(existingItem.quantity + requestedQuantity, normalizedStock);

        result = {
          status: nextQuantity === existingItem.quantity ? 'max-stock-reached' : 'updated',
          quantity: nextQuantity,
          maxQuantity: normalizedStock,
        };

        return prev.map((item) =>
          item.id === newItemId
            ? {
                ...item,
                ...itemToAdd,
                id: newItemId,
                quantity: nextQuantity,
                stock: normalizedStock,
                selected: item.selected,
              }
            : item,
        );
      } else {
        const nextQuantity = Math.min(requestedQuantity, normalizedStock);

        result = {
          status: 'added',
          quantity: nextQuantity,
          maxQuantity: normalizedStock,
        };

        const newItem: CartItem = {
          ...itemToAdd,
          id: newItemId,
          quantity: nextQuantity,
          stock: normalizedStock,
          selected: true,
        };
        return [...prev, newItem];
      }
    });

    return result;
  };

  /**
   * Elimina un ítem del carrito según su id compuesto.
   */
  const removeItem = (id: string) => {
    setItems((prev) => prev.filter((item) => item.id !== id));
  };

  /**
   * Actualiza la cantidad de un ítem aplicando límites entre 1 y el stock disponible.
   */
  const updateItemQuantity = (id: string, newQuantity: number) => {
    setItems((prev) =>
      prev.map((item) => {
        if (item.id === id) {
          const maxQuantity = Math.max(1, item.stock);
          const clampedQuantity = Math.max(1, Math.min(newQuantity, maxQuantity));
          return { ...item, quantity: clampedQuantity };
        }
        return item;
      }),
    );
  };

  /**
   * Alterna la selección de un ítem para decidir si entra en el total / exportación.
   */
  const toggleItemSelection = (id: string) => {
    setItems((prev) =>
      prev.map((item) =>
        item.id === id ? { ...item, selected: !item.selected } : item,
      ),
    );
  };

  const totalPrice = useMemo(
    () =>
      items
        .filter((item) => item.selected)
        .reduce((acc, item) => acc + item.unitPrice * item.quantity, 0),
    [items],
  );

  const itemCount = useMemo(
    () => items.reduce((acc, item) => acc + item.quantity, 0),
    [items],
  );

  return (
    <CartContext.Provider
      value={{
        items,
        addItem,
        removeItem,
        updateItemQuantity,
        toggleItemSelection,
        totalPrice,
        itemCount,
      }}
    >
      {children}
    </CartContext.Provider>
  );
};


