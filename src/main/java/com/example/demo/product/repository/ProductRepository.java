package com.example.demo.product.repository;

import com.example.demo.product.entity.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Query("SELECT p FROM Product p WHERE " +
           "(:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) AND " +
           "(:categoryId IS NULL OR p.categoryId = :categoryId)")
    List<Product> filterProducts(@Param("brand") String brand, @Param("categoryId") Integer categoryId, Sort sort);
}
