package com.laberit.backend.repository;

import com.laberit.backend.entity.ProviderMapping;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderMappingRepository extends JpaRepository<ProviderMapping, Integer> {

    List<ProviderMapping> findBySupplier_Id(Integer supplierId);

    Optional<ProviderMapping> findBySupplier_IdAndInternalField(Integer supplierId, String internalField);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProviderMapping p WHERE p.supplier.id = :id")
    void deleteBySupplierId(@Param("id") Integer id);

    boolean existsBySupplier_Id(Integer supplierId);
}
