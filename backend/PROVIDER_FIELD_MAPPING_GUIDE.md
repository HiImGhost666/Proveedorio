## Guía de mapeo de campos entre proveedores externos y modelo interno

Este documento explica cómo se configuran los mapeos de campos entre las APIs externas de proveedores (ficheros JSON del `json-mock-server`) y el modelo interno normalizado usado por el backend.

- **Entidad de mapeo**: `ProviderMapping`  
  - `internalField`: nombre del campo **interno** (normalizado) que usa la aplicación.  
  - `externalField`: ruta o clave del campo **externo** en la respuesta JSON del proveedor.  
  - `transformationType`: indica cómo leer el valor externo (`DIRECT`, `NESTED`, `FIND_IN_ARRAY`, `SPLIT`).  
- **Seeder** principal: `DataSeeder`  
  - Crea proveedores de ejemplo y sus reglas de mapeo usando `createMapping(supplier, internalField, externalField, transformationType)`.  
- **JSON de ejemplo**: `json-mock-server/api1.json`, `api2.json`, `api3.json`, `api4.json`.

La normalización se aplica posteriormente en `UniversalMapperService`, que interpreta `externalField` y `transformationType` para leer el valor correcto en cada JSON.

---

### Proveedor: API 1 (Plana)

- **Seeder**: bloque `"API 1 (Plana)"` en `DataSeeder`.  
- **JSON de ejemplo**: `json-mock-server/api1.json`  
- **Estructura externa** (simplificada):

```json
{
  "products": [
    {
      "id": 5,
      "brand": "AMD",
      "model": "Ryzen 7 7800X3D",
      "mpn": "100-100000910WOF",
      "category": "CPU",
      "price": 12.00,
      "stock": 25,
      "retail_price": 399.00,
      "ean": "0730143314558",
      "moq": 1,
      "condition": "NEW"
    }
  ]
}
```

- **Mapeos principales** (`internalField` → `externalField`):
  - `id` → `id` (`DIRECT`)  
  - `mpn` → `mpn` (`DIRECT`)  
  - `brand` → `brand` (`DIRECT`)  
  - `model` → `model` (`DIRECT`)  
  - `price` → `price` (`DIRECT`)  
  - `stock` → `stock` (`DIRECT`)  
  - `retail_price` → `retail_price` (`DIRECT`)  
  - `ean` → `ean` (`DIRECT`)  
  - `moq` → `moq` (`DIRECT`)  
  - `condition` → `condition` (`DIRECT`)  

- **Especificaciones técnicas (spec_*)**:  
  - `spec_category` → `category` (`DIRECT`)  
  - `spec_socket` → `socket` (`DIRECT`)  
  - `spec_capacity` → `capacity` (`DIRECT`)  
  - `spec_ram_size` → `ram_size` (`DIRECT`)  
  - `spec_vram` → `vram` (`DIRECT`)  
  - `spec_cores` → `cores` (`DIRECT`)  
  - `spec_warranty_months` → `warranty_months` (`DIRECT`)  

Estas claves pueden no existir en todos los productos del JSON de ejemplo; en esos casos el valor interno quedará vacío/null.

---

### Proveedor: API 2 (Jerárquica)

- **Seeder**: bloque `"API 2 (Jerárquica)"` en `DataSeeder`.  
- **JSON de ejemplo**: `json-mock-server/api2.json`  
- **Estructura externa** (simplificada):

```json
{
  "products": [
    {
      "identifier": "PROD-AMD-05",
      "manufacturer": {
        "name": "AMD",
        "part_number": "100-100000910WOF",
        "ean": "0730143314558"
      },
      "marketing_name": "Ryzen 7 7800X3D (V-Cache)",
      "commercial": {
        "price_data": { "amount": 319.00, "currency": "USD" },
        "inventory": { "quantity": 10 },
        "suggested_pvp": 399.00,
        "min_purchase_qty": 1,
        "item_condition": "NEW"
      },
      "specs": {
        "tech": {
          "chipset": "Z790",
          "form_factor": "ATX"
        }
      }
    }
  ]
}
```

- **Mapeos principales**:
  - `id` → `identifier` (`DIRECT`)  
  - `mpn` → `manufacturer.part_number` (`NESTED`)  
  - `brand` → `manufacturer.name` (`NESTED`)  
  - `model` → `marketing_name` (`DIRECT`)  
  - `price` → `commercial.price_data.amount` (`NESTED`)  
  - `stock` → `commercial.inventory.quantity` (`NESTED`)  

- **Campos comerciales adicionales**:
  - `retail_price` → `commercial.suggested_pvp` (`NESTED`)  
  - `ean` → `manufacturer.ean` (`NESTED`)  
  - `moq` → `commercial.min_purchase_qty` (`NESTED`)  
  - `condition` → `commercial.item_condition` (`NESTED`)  

- **Especificaciones técnicas (spec_*, en árbol `specs.tech`)**:
  - `spec_chipset` → `specs.tech.chipset` (`NESTED`)  
  - `spec_form_factor` → `specs.tech.form_factor` (`NESTED`)  
  - `spec_size` → `specs.tech.size` (`NESTED`)  
  - `spec_type` → `specs.tech.type` (`NESTED`)  
  - `spec_kit` → `specs.tech.kit` (`NESTED`)  
  - `spec_speed` → `specs.tech.speed` (`NESTED`)  
  - `spec_memory` → `specs.tech.memory` (`NESTED`)  
  - `spec_clockspeed` → `specs.tech.clockspeed` (`NESTED`)  
  - `spec_p_cores` → `specs.tech.p_cores` (`NESTED`)  
  - `spec_e_cores` → `specs.tech.e_cores` (`NESTED`)  

---

### Proveedor: API 3 (EAV)

- **Seeder**: bloque `"API 3 (EAV)"` en `DataSeeder`.  
- **JSON de ejemplo**: `json-mock-server/api3.json`  
- **Estructura externa** (simplificada):

```json
{
  "products": [
    {
      "id_interno": "EAV-100",
      "vendor_brand": "MSI",
      "model_name": "MAG B650 Tomahawk",
      "factory_code": "7D75-001R",
      "barcode": "4719072977566",
      "item_status": "NEW",
      "attributes": [
        { "key": "socket", "val": "AM5" },
        { "key": "wifi", "val": "Yes" }
      ],
      "pricing": { "retail_price": 219.00, "pvp": 259.00, "min_qty": 1 },
      "stock_level": 22
    }
  ]
}
```

- **Mapeos principales**:
  - `id` → `id_interno` (`DIRECT`)  
  - `mpn` → `factory_code` (`DIRECT`)  
  - `brand` → `vendor_brand` (`DIRECT`)  
  - `model` → `model_name` (`DIRECT`)  
  - `price` → `pricing.retail_price` (`NESTED`)  
  - `stock` → `stock_level` (`DIRECT`)  

- **Campos comerciales adicionales**:
  - `retail_price` → `pricing.pvp` (`NESTED`)  
  - `ean` → `barcode` (`DIRECT`)  
  - `moq` → `pricing.min_qty` (`NESTED`)  
  - `condition` → `item_status` (`DIRECT`)  

- **Especificaciones técnicas vía array de atributos (`FIND_IN_ARRAY`)**:
  - `spec_socket` → `attributes[key=socket]`  
  - `spec_wifi` → `attributes[key=wifi]`  
  - `spec_capacity` → `attributes[key=cap]`  
  - `spec_gen` → `attributes[key=gen]`  
  - `spec_ram_size` → `attributes[key=ram_total]`  
  - `spec_latency` → `attributes[key=latency]`  
  - `spec_watts` → `attributes[key=watts]`  
  - `spec_cert` → `attributes[key=cert]`  
  - `spec_type` → `attributes[key=type]`  
  - `spec_color` → `attributes[key=color]`  

Para estos mapeos, la estrategia `FIND_IN_ARRAY` busca en el array `attributes` el elemento cuya propiedad `key` coincida con el sufijo indicado y toma su `val`.

---

### Proveedor: API 4 (Legacy)

- **Seeder**: bloque `"API 4 (Legacy)"` en `DataSeeder`.  
- **JSON de ejemplo**: `json-mock-server/api4.json`  
- **Estructura externa** (simplificada):

```json
{
  "products": [
    {
      "pk": 9901,
      "mfr": "AsRock",
      "mod_num": "X670E Taichi",
      "upc_ean": "X670E-TC",
      "feat": "E-ATX;DDR5;USB4",
      "val_unit": "489.00",
      "q_avail": "3",
      "trade_info": {
        "msrp": "579.00",
        "gtin": "4718487898800",
        "bulk_min": "1",
        "prod_state": "NEW"
      }
    }
  ]
}
```

- **Mapeos principales**:
  - `id` → `pk` (`DIRECT`)  
  - `mpn` → `upc_ean` (`DIRECT`)  
  - `brand` → `mfr` (`DIRECT`)  
  - `model` → `mod_num` (`DIRECT`)  
  - `price` → `val_unit` (`DIRECT`)  
  - `stock` → `q_avail` (`DIRECT`)  

- **Campos comerciales adicionales**:
  - `retail_price` → `trade_info.msrp` (`NESTED`)  
  - `ean` → `trade_info.gtin` (`NESTED`)  
  - `moq` → `trade_info.bulk_min` (`NESTED`)  
  - `condition` → `trade_info.prod_state` (`NESTED`)  

- **Especificaciones técnicas derivadas de campo semiestructurado (`SPLIT`)**:
  - `spec_feat_0` → `feat[0]` (`SPLIT`)  
  - `spec_feat_1` → `feat[1]` (`SPLIT`)  
  - `spec_feat_2` → `feat[2]` (`SPLIT`)  

En este caso, la estrategia `SPLIT` divide la cadena `feat` (por ejemplo `"E-ATX;DDR5;USB4"`) por un separador (`;`) y mapea cada posición a un campo interno (`spec_feat_0`, `spec_feat_1`, etc.).

---

### Resumen

- Los **nombres de campos externos** (`externalField`) definidos en `DataSeeder` coinciden con las claves y rutas reales de los JSON de ejemplo (`api1.json`–`api4.json`).  
- Los **campos internos** (`internalField`) representan la vista normalizada que la aplicación usa para:
  - Rellenar `MasterProduct` (marca, modelo, mpn).  
  - Rellenar `ProductSupplier` (precio, stock, condiciones comerciales).  
  - Construir el JSON de especificaciones (`techSpecs`) a partir de los prefijos `spec_*`.  
- La **estrategia de transformación** (`transformationType`) indica a `UniversalMapperService` cómo navegar el JSON:
  - `DIRECT`: lectura directa de una clave plana.  
  - `NESTED`: navegación por rutas con puntos (por ejemplo `commercial.price_data.amount`).  
  - `FIND_IN_ARRAY`: búsqueda en arrays por una clave (`attributes[key=socket]`).  
  - `SPLIT`: descomposición de una cadena en partes indexadas (`feat[0]`, `feat[1]`, ...).

Esta guía sirve como referencia para añadir nuevos proveedores: basta con seguir el mismo patrón de `internalField` (modelo interno) y definir correctamente `externalField` + `transformationType` según la estructura del JSON externo.

