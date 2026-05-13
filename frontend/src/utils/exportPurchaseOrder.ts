import ExcelJS from 'exceljs';
import type { CartItem } from 'features/cart/context/CartContext';

// Layout basado en la plantilla Excel "OrdenDeCompra_Plantilla.xlsx"
const TEMPLATE_DATA_START = 6; // Fila 6: primer producto
const TEMPLATE_DATA_ROWS = 5;  // Filas 6–10 reservadas en la plantilla

const COL = {
  index: 1,          // A  #
  mpn: 2,            // B  MPN
  product: 3,        // C  Producto
  url: 4,            // D  URL Producto
  supplier: 5,       // E  Proveedor
  quantity: 6,       // F  Cantidad
  currentStock: 7,   // G  Stock Actual
  partnerPrice: 8,   // H  Precio Partner (columna del total)
  retailPrice: 9,    // I  Precio P.V.P
  notes: 10,         // J  Notas
} as const;

const FMT = {
  integer: '#,##0',
  currency: '#,##0.00\\ "€"',
} as const;

/**
 * Genera un número de pedido con formato PC-YYYYMMDD-NNN.
 */
function generateOrderNumber(): string {
  const now = new Date();
  const yyyymmdd =
    `${now.getFullYear()}` +
    `${String(now.getMonth() + 1).padStart(2, '0')}` +
    `${String(now.getDate()).padStart(2, '0')}`;
  const seq = String(Math.floor(Math.random() * 999) + 1).padStart(3, '0');
  return `PC-${yyyymmdd}-${seq}`;
}

/**
 * Copia el estilo (formato, altura) de una fila origen a una fila destino,
 * clonando estilos para no compartir referencias mutables.
 */
function copyRowStyle(source: ExcelJS.Row, target: ExcelJS.Row): void {
  source.eachCell({ includeEmpty: true }, (srcCell: ExcelJS.Cell, col: number) => {
    const tgtCell = target.getCell(col);
    if (srcCell.style) {
      // Clonar el estilo para no compartir referencias mutables
      tgtCell.style = JSON.parse(JSON.stringify(srcCell.style));
    }
  });
  target.height = source.height;
}

/**
 * Exporta la orden de compra a Excel: carga la plantilla, rellena productos,
 * inserta filas extra si hace falta, limpia solo filas de plantilla no usadas,
 * añade una fila vacía y el total en columna I.
 */
export async function exportPurchaseOrder(items: CartItem[]): Promise<void> {
  // 1) Cargar plantilla desde /public
  // IMPORTANTE: coloca el archivo en Web-App-Inventario-Client/public/OrdenDeCompra_Plantilla.xlsx
  const response = await fetch('/OrdenDeCompra_Plantilla.xlsx');
  if (!response.ok) {
    throw new Error(
      'No se pudo cargar OrdenDeCompra_Plantilla.xlsx. ' +
        'Asegúrate de que el archivo está en public/ y el nombre coincide.',
    );
  }

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(await response.arrayBuffer());
  const sheet = workbook.worksheets[0];

  const selectedItems = items.filter((item) => item.selected);
  const itemCount = selectedItems.length;
  if (itemCount === 0) {
    return;
  }

  // 2) Número de pedido en J1
  const orderNumber = generateOrderNumber();
  sheet.getRow(1).getCell(COL.notes).value = orderNumber; // col 10 = J

  // 3) Inserción dinámica: filas extra para cualquier cantidad de productos.
  //    Si hay más de TEMPLATE_DATA_ROWS, se insertan filas copiando el estilo de la fila 6.
  const styleSourceRow = sheet.getRow(TEMPLATE_DATA_START);
  const extraRows = Math.max(0, itemCount - TEMPLATE_DATA_ROWS);
  const insertStartRow = TEMPLATE_DATA_START + TEMPLATE_DATA_ROWS;

  if (extraRows > 0) {
    for (let i = 0; i < extraRows; i++) {
      sheet.insertRow(insertStartRow + i, []);
      copyRowStyle(styleSourceRow, sheet.getRow(insertStartRow + i));
    }
  }

  // 4) Rellenar filas de datos
  selectedItems.forEach((item, index) => {
    const rowNum = TEMPLATE_DATA_START + index;
    const row = sheet.getRow(rowNum);

    const setCell = (col: number, value: ExcelJS.CellValue, numFmt?: string) => {
      const cell = row.getCell(col);
      cell.value = value;
      if (numFmt) cell.numFmt = numFmt;
    };

    setCell(COL.index, { formula: `ROW()-ROW($A$5)` });
    setCell(COL.mpn, item.mpn ?? '');
    setCell(COL.product, item.productName);
    setCell(
      COL.url,
      item.productUrl ? { text: item.productUrl, hyperlink: item.productUrl } : '',
    );
    setCell(COL.supplier, item.supplierName ?? '');
    setCell(COL.quantity, item.quantity, FMT.integer);
    setCell(COL.currentStock, item.stock ?? '', FMT.integer);
    setCell(COL.retailPrice, item.pvpPrice ?? '', FMT.currency);
    setCell(COL.partnerPrice, item.unitPrice, FMT.currency);
    setCell(COL.notes, item.notes ?? '');

    row.commit();
  });

  // 5) Limpieza selectiva: solo vaciar las filas de plantilla no usadas
  //    (cuando hay menos de TEMPLATE_DATA_ROWS productos). No tocar filas insertadas dinámicamente.
  const usedTemplateRows = Math.min(itemCount, TEMPLATE_DATA_ROWS);
  for (let i = usedTemplateRows; i < TEMPLATE_DATA_ROWS; i++) {
    const row = sheet.getRow(TEMPLATE_DATA_START + i);
    row.eachCell({ includeEmpty: true }, (cell: ExcelJS.Cell) => {
      cell.value = null;
    });
    row.commit();
  }

  // 6) Línea de resumen (fila azul) justo debajo del último producto,
  // reutilizando la fila azul original de la plantilla como plantilla de estilo.
  const lastDataRow = TEMPLATE_DATA_START + itemCount - 1;
  const baseBlueRowIndex = TEMPLATE_DATA_START + TEMPLATE_DATA_ROWS + 1;
  const blueRowIndex = baseBlueRowIndex + extraRows;

  const blueTemplateRow = sheet.getRow(blueRowIndex);
  const summaryRow = sheet.getRow(lastDataRow + 1);

  // Copiar estilo de la fila azul original a la nueva posición de resumen
  copyRowStyle(blueTemplateRow, summaryRow);
  summaryRow.eachCell({ includeEmpty: true }, (cell: ExcelJS.Cell) => {
    cell.value = null;
  });

  // Colocar la sumatoria bajo la columna de observaciones (Notas)
  const totalCell = summaryRow.getCell(COL.notes);
  totalCell.value = {
    // F = cantidad, H = partnerPrice (precio interno)
    formula: `SUMPRODUCT(F${TEMPLATE_DATA_START}:F${lastDataRow},H${TEMPLATE_DATA_START}:H${lastDataRow})`,
  };
  totalCell.numFmt = FMT.currency;
  summaryRow.commit();

  // Limpiar la fila azul original si ha quedado desplazada (para no duplicar la línea)
  if (blueRowIndex !== summaryRow.number) {
    const originalBlueRow = sheet.getRow(blueRowIndex);
    originalBlueRow.eachCell({ includeEmpty: true }, (cell: ExcelJS.Cell) => {
      cell.value = null;
    });
    originalBlueRow.commit();
  }

  // 8) Descargar el archivo
  const buffer = await workbook.xlsx.writeBuffer();
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `OrdenDeCompra_${orderNumber}.xlsx`;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  setTimeout(() => URL.revokeObjectURL(url), 1_000);
}

