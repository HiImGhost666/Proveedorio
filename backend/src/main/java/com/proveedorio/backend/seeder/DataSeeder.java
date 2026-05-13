package com.proveedorio.backend.seeder;

import com.proveedorio.backend.entity.ProviderMapping;
import com.proveedorio.backend.entity.Role;
import com.proveedorio.backend.entity.Supplier;
import com.proveedorio.backend.entity.User;
import com.proveedorio.backend.repository.SupplierRepository;
import com.proveedorio.backend.repository.ProviderMappingRepository;
import com.proveedorio.backend.repository.RoleRepository;
import com.proveedorio.backend.repository.UserRepository;
import com.proveedorio.backend.service.CurrencyRateService;
import com.proveedorio.backend.service.EncryptionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

@Component
@Profile("!test")
@Order(2)
public class DataSeeder implements CommandLineRunner {

    private final SupplierRepository supplierRepository;
    private final ProviderMappingRepository mappingRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;
    private final CurrencyRateService currencyRateService;

    public DataSeeder(SupplierRepository supplierRepository,
                      ProviderMappingRepository mappingRepository,
                      UserRepository userRepository,
                      RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder,
                      EncryptionService encryptionService,
                      CurrencyRateService currencyRateService) {
        this.supplierRepository = supplierRepository;
        this.mappingRepository = mappingRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionService = encryptionService;
        this.currencyRateService = currencyRateService;
    }

    @Override
    public void run(String... args) throws Exception {
        seedAdminUser();
        seedProveedores();
        // La siembra inicial de tasas se delega al servicio, que primero intenta
        // obtenerlas en vivo desde la API del BCE (Frankfurter) y si no está
        // disponible usa valores de respaldo. StartupDataSynchronizer las
        // refrescará de nuevo justo antes de la primera sincronización.
        currencyRateService.refreshRates();
    }

    private void seedAdminUser() {
        // Uso de saveIfAbsent para evitar la race condition entre backend1 y backend2
        // que arrancan en paralelo y podrían intentar insertar el mismo rol simultáneamente.
        saveRoleIfAbsent("ROLE_ADMIN");
        saveRoleIfAbsent("ROLE_CLIENTE");

        if (!userRepository.existsByUsername("admin")) {
            Optional<Role> adminRoleOpt = roleRepository.findByName("ROLE_ADMIN");

            if (adminRoleOpt.isPresent()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setEmail("admin@proveedorio.com");
                admin.setFullName("Administrador del Sistema");
                admin.setActive(true);
                admin.setRoles(new HashSet<>(Collections.singletonList(adminRoleOpt.get())));
                userRepository.save(admin);
                System.out.println("Seeder: Usuario ADMIN creado (admin / admin123)");
            }
        }

        if (!userRepository.existsByUsername("cliente")) {
            Optional<Role> clienteRoleOpt = roleRepository.findByName("ROLE_CLIENTE");

            if (clienteRoleOpt.isPresent()) {
                User cliente = new User();
                cliente.setUsername("cliente");
                cliente.setPassword(passwordEncoder.encode("cliente123"));
                cliente.setEmail("cliente@proveedorio.com");
                cliente.setFullName("Cliente de Prueba");
                cliente.setActive(true);
                cliente.setRoles(new HashSet<>(Collections.singletonList(clienteRoleOpt.get())));
                userRepository.save(cliente);
                System.out.println("Seeder: Usuario CLIENTE creado (cliente / cliente123)");
            }
        }
    }

    /**
     * Crea el rol si no existe, absorbiendo el duplicate-key si dos instancias
     * (backend1/backend2) llegan simultáneamente al primer arranque.
     */
    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    protected void saveRoleIfAbsent(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            try {
                roleRepository.save(new Role(roleName));
                System.out.println("Seeder: Rol creado -> " + roleName);
            } catch (DataIntegrityViolationException e) {
                // Otra instancia se adelantó e insertó el rol — situación normal en arranque paralelo
                System.out.println("Seeder: Rol ya existente (insertado por otra instancia) -> " + roleName);
            }
        }
    }

    private void seedProveedores() {
        // API 1 — Plana
        supplierRepository.findByName("API 1 (Plana)").orElseGet(() -> {
            System.out.println("Seeder: Creando proveedor 'API 1 (Plana)'...");
            Supplier p = createProvider("API 1 (Plana)", "http://json-mock-server:3001", "/products",
                    "+34 91 123 45 67", "ventas@proveedor1.es", "Carlos Pérez",
                    "https://www.proveedor1.es", "ES", "EUR");
            supplierRepository.save(p);
            
            createMapping(p, "id", "id", "DIRECT");
            createMapping(p, "mpn", "mpn", "DIRECT");
            createMapping(p, "brand", "brand", "DIRECT");
            createMapping(p, "model", "model", "DIRECT");
            createMapping(p, "price", "price", "DIRECT");
            createMapping(p, "stock", "stock", "DIRECT");
            
            // Nuevos campos comerciales
            createMapping(p, "retail_price", "retail_price", "DIRECT");
            createMapping(p, "ean", "ean", "DIRECT");
            createMapping(p, "moq", "moq", "DIRECT");
            createMapping(p, "condition", "condition", "DIRECT");
            // Categoría canónica y algunas specs
            createMapping(p, "category", "category", "DIRECT");
            createMapping(p, "spec_category", "category", "DIRECT");
            createMapping(p, "spec_socket", "socket", "DIRECT");
            createMapping(p, "spec_capacity", "capacity", "DIRECT");
            createMapping(p, "spec_ram_size", "ram_size", "DIRECT");
            createMapping(p, "spec_vram", "vram", "DIRECT");
            createMapping(p, "spec_cores", "cores", "DIRECT");
            createMapping(p, "spec_warranty_months", "warranty_months", "DIRECT");
            return p;
        });

        // API 2 — Jerárquica
        supplierRepository.findByName("API 2 (Jerárquica)").orElseGet(() -> {
            System.out.println("Seeder: Creando proveedor 'API 2 (Jerárquica)'...");
            Supplier p = createProvider("API 2 (Jerárquica)", "http://json-mock-server:3002", "/products",
                    "+1 800 555 1234", "sales@supplier2.com", "John Smith",
                    "https://www.supplier2.com", "US", "USD");
            supplierRepository.save(p);

            createMapping(p, "id", "identifier", "DIRECT");
            createMapping(p, "mpn", "manufacturer.part_number", "NESTED");
            createMapping(p, "brand", "manufacturer.name", "NESTED");
            createMapping(p, "model", "marketing_name", "DIRECT");
            createMapping(p, "price", "commercial.price_data.amount", "NESTED");
            createMapping(p, "stock", "commercial.inventory.quantity", "NESTED");

            // Nuevos campos comerciales
            createMapping(p, "retail_price", "commercial.suggested_pvp", "NESTED");
            createMapping(p, "ean", "manufacturer.ean", "NESTED");
            createMapping(p, "moq", "commercial.min_purchase_qty", "NESTED");
            createMapping(p, "condition", "commercial.item_condition", "NESTED");

            // Categoría canónica
            createMapping(p, "category", "category", "DIRECT");

            createMapping(p, "spec_chipset", "specs.tech.chipset", "NESTED");
            createMapping(p, "spec_form_factor", "specs.tech.form_factor", "NESTED");
            createMapping(p, "spec_size", "specs.tech.size", "NESTED");
            createMapping(p, "spec_type", "specs.tech.type", "NESTED");
            createMapping(p, "spec_kit", "specs.tech.kit", "NESTED");
            createMapping(p, "spec_speed", "specs.tech.speed", "NESTED");
            createMapping(p, "spec_memory", "specs.tech.memory", "NESTED");
            createMapping(p, "spec_clockspeed", "specs.tech.clockspeed", "NESTED");
            createMapping(p, "spec_p_cores", "specs.tech.p_cores", "NESTED");
            createMapping(p, "spec_e_cores", "specs.tech.e_cores", "NESTED");
            return p;
        });

        // API 3 — EAV
        supplierRepository.findByName("API 3 (EAV)").orElseGet(() -> {
            System.out.println("Seeder: Creando proveedor 'API 3 (EAV)'...");
            Supplier p = createProvider("API 3 (EAV)", "http://json-mock-server:3003", "/products",
                    "+49 30 9876 54 32", "einkauf@lieferant3.de", "Hans Müller",
                    "https://www.lieferant3.de", "DE", "EUR");
            supplierRepository.save(p);

            createMapping(p, "id", "id_interno", "DIRECT");
            createMapping(p, "mpn", "factory_code", "DIRECT");
            createMapping(p, "brand", "vendor_brand", "DIRECT");
            createMapping(p, "model", "model_name", "DIRECT");
            createMapping(p, "price", "pricing.retail_price", "NESTED"); // Coste
            createMapping(p, "stock", "stock_level", "DIRECT");

            // Nuevos campos comerciales
            createMapping(p, "retail_price", "pricing.pvp", "NESTED"); // PVP
            createMapping(p, "ean", "barcode", "DIRECT");
            createMapping(p, "moq", "pricing.min_qty", "NESTED");
            createMapping(p, "condition", "item_status", "DIRECT");

            // Categoría canónica (atributo EAV)
            createMapping(p, "category", "attributes[key=category]", "FIND_IN_ARRAY");

            createMapping(p, "spec_socket", "attributes[key=socket]", "FIND_IN_ARRAY");
            createMapping(p, "spec_wifi", "attributes[key=wifi]", "FIND_IN_ARRAY");
            createMapping(p, "spec_capacity", "attributes[key=cap]", "FIND_IN_ARRAY");
            createMapping(p, "spec_gen", "attributes[key=gen]", "FIND_IN_ARRAY");
            createMapping(p, "spec_ram_size", "attributes[key=ram_total]", "FIND_IN_ARRAY");
            createMapping(p, "spec_latency", "attributes[key=latency]", "FIND_IN_ARRAY");
            createMapping(p, "spec_watts", "attributes[key=watts]", "FIND_IN_ARRAY");
            createMapping(p, "spec_cert", "attributes[key=cert]", "FIND_IN_ARRAY");
            createMapping(p, "spec_type", "attributes[key=type]", "FIND_IN_ARRAY");
            createMapping(p, "spec_color", "attributes[key=color]", "FIND_IN_ARRAY");
            return p;
        });

        // API 4 — Legacy
        supplierRepository.findByName("API 4 (Legacy)").orElseGet(() -> {
            System.out.println("Seeder: Creando proveedor 'API 4 (Legacy)'...");
            Supplier p = createProvider("API 4 (Legacy)", "http://json-mock-server:3004", "/products",
                    "+44 20 7946 09 58", "procurement@supplier4.co.uk", "Emily Watson",
                    "https://www.supplier4.co.uk", "GB", "GBP");
            supplierRepository.save(p);

            createMapping(p, "id", "pk", "DIRECT");
            createMapping(p, "mpn", "upc_ean", "DIRECT");
            createMapping(p, "brand", "mfr", "DIRECT");
            createMapping(p, "model", "mod_num", "DIRECT");
            createMapping(p, "price", "val_unit", "DIRECT");
            createMapping(p, "stock", "q_avail", "DIRECT");

            // Nuevos campos comerciales
            createMapping(p, "retail_price", "trade_info.msrp", "NESTED");
            createMapping(p, "ean", "trade_info.gtin", "NESTED");
            createMapping(p, "moq", "trade_info.bulk_min", "NESTED");
            createMapping(p, "condition", "trade_info.prod_state", "NESTED");

            // Categoría canónica
            createMapping(p, "category", "category", "DIRECT");

            createMapping(p, "spec_feat_0", "feat[0]", "SPLIT");
            createMapping(p, "spec_feat_1", "feat[1]", "SPLIT");
            createMapping(p, "spec_feat_2", "feat[2]", "SPLIT");
            return p;
        });
        
        System.out.println("Seeder: Verificación de proveedores completada.");
    }

    private Supplier createProvider(String nombre, String url, String catalogEndpoint,
                                     String phone, String email, String contact,
                                     String website, String country, String currency) {
        Supplier p = new Supplier();
        p.setName(nombre);
        p.setBaseUrlApi(url);
        p.setApiKey(encryptionService.encrypt("NO_API_KEY"));
        p.setActive(true);
        p.setSupportsBulkSync(true);
        p.setCatalogEndpoint(catalogEndpoint);
        p.setDetailEndpoint(catalogEndpoint + "/{sku}");
        p.setSearchEndpoint(catalogEndpoint + "?q={query}");
        p.setPhone(phone);
        p.setEmail(email);
        p.setContact(contact);
        p.setWebsite(website);
        p.setCountry(country);
        p.setDefaultCurrency(currency);
        return p;
    }

    private void createMapping(Supplier supplier, String campoInterno, String campoExterno, String tipo) {
        ProviderMapping mapping = new ProviderMapping();
        mapping.setSupplier(supplier);
        mapping.setInternalField(campoInterno);
        mapping.setExternalField(campoExterno);
        mapping.setTransformationType(tipo);
        mappingRepository.save(mapping);
    }
}
