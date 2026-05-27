package com.example.demo.config;

import org.springframework.jdbc.core.JdbcTemplate;
import com.example.demo.product.entity.Product;
import com.example.demo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN avatar LONGTEXT");
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN address TEXT");
        } catch (Exception e) {
            System.err.println("DatabaseSeeder - Failed to alter columns explicitly: " + e.getMessage());
        }

        if (productRepository.count() == 0) {
            Product p1 = Product.builder()
                    .name("Vợt Cầu Lông Yonex Astrox 99 Pro (White Tiger)")
                    .description("Dòng vợt cao cấp hỗ trợ tấn công mạnh mẽ cho các vận động viên chuyên nghiệp.")
                    .price(3450000.0)
                    .stock(15)
                    .imageUrl("/racket_product_1.png")
                    .brand("Yonex")
                    .categoryId(1)
                    .categoryName("Vợt Cầu Lông")
                    .build();

            Product p2 = Product.builder()
                    .name("Giày Cầu Lông Lining Halberd III Lite Trắng Cam")
                    .description("Giày siêu êm ái, bám sân cực tốt, bảo vệ khớp cổ chân tối ưu.")
                    .price(1250000.0)
                    .stock(20)
                    .imageUrl("/shoe_product_1.png")
                    .brand("Lining")
                    .categoryId(2)
                    .categoryName("Giày Cầu Lông")
                    .build();

            Product p3 = Product.builder()
                    .name("Vợt Cầu Lông Victor Thruster Ryuga II (Mã JP)")
                    .description("Dòng vợt thiên công mạnh mẽ được Lee Zii Jia tin dùng.")
                    .price(3100000.0)
                    .stock(10)
                    .imageUrl("/racket_product_1.png")
                    .brand("Victor")
                    .categoryId(1)
                    .categoryName("Vợt Cầu Lông")
                    .build();

            Product p4 = Product.builder()
                    .name("Giày Cầu Lông Yonex Power Cushion 65Z3 Men")
                    .description("Mẫu giày cầu lông bán chạy nhất thế giới với công nghệ Power Cushion độc quyền.")
                    .price(2850000.0)
                    .stock(8)
                    .imageUrl("/shoe_product_1.png")
                    .brand("Yonex")
                    .categoryId(2)
                    .categoryName("Giày Cầu Lông")
                    .build();

            Product p5 = Product.builder()
                    .name("Áo Cầu Lông Lining Thi Đấu 2026")
                    .description("Chất liệu mè thun mát lạnh, co giãn 4 chiều và thấm hút mồ hôi cực đỉnh.")
                    .price(350000.0)
                    .stock(100)
                    .imageUrl("/shirt_product_1.png")
                    .brand("Lining")
                    .categoryId(3)
                    .categoryName("Áo Cầu Lông")
                    .build();

            Product p6 = Product.builder()
                    .name("Túi Cầu Lông Victor Chuyên Nghiệp BR9609")
                    .description("Kiểu dáng thể thao, đựng được nhiều vợt, quần áo và có ngăn giày riêng.")
                    .price(950000.0)
                    .stock(12)
                    .imageUrl("/bag_product_1.png")
                    .brand("Victor")
                    .categoryId(4)
                    .categoryName("Túi Cầu Lông")
                    .build();

            Product p7 = Product.builder()
                    .name("Hộp Cầu Lông Victor Lark 5 (12 Quả)")
                    .description("Cầu bay đầm, độ bền cao, thích hợp cho cả tập luyện và thi đấu.")
                    .price(220000.0)
                    .stock(150)
                    .imageUrl("/shuttlecock_product_1.png")
                    .brand("Victor")
                    .categoryId(5)
                    .categoryName("Cầu Lông")
                    .build();

            Product p8 = Product.builder()
                    .name("Quấn Cán Vợt Yonex AC102EX (Vỉ 3 Cái)")
                    .description("Được sản xuất tại Nhật Bản, độ bám cao và êm tay tuyệt đối.")
                    .price(45000.0)
                    .stock(500)
                    .imageUrl("/accessory_product_1.png")
                    .brand("Yonex")
                    .categoryId(6)
                    .categoryName("Phụ kiện Cầu Lông")
                    .build();

            Product p9 = Product.builder()
                    .name("Giày Cầu Lông Kawasaki K-061 Đỏ Đen")
                    .description("Giày cầu lông phân khúc tầm trung siêu bền bỉ và bám sân tốt.")
                    .price(680000.0)
                    .stock(30)
                    .imageUrl("/shoe_product_1.png")
                    .brand("Kawasaki")
                    .categoryId(2)
                    .categoryName("Giày Cầu Lông")
                    .build();

            Product p10 = Product.builder()
                    .name("Vợt Cầu Lông Mizuno Altrax 85")
                    .description("Thiết kế khí động học cao cấp, đem lại khả năng phản tạt nhanh như chớp.")
                    .price(1650000.0)
                    .stock(6)
                    .imageUrl("/racket_product_1.png")
                    .brand("Mizuno")
                    .categoryId(1)
                    .categoryName("Vợt Cầu Lông")
                    .build();

            productRepository.saveAll(Arrays.asList(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10));
        }
    }
}
