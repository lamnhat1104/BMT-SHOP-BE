package com.example.demo;

import com.example.demo.product.entity.Product;
import com.example.demo.product.repository.ProductRepository;
import com.example.demo.product.service.ProductCrawlerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private ProductCrawlerService productCrawlerService;

	@Autowired
	private ProductRepository productRepository;

	@Test
	void testCrawl() {
		String url = "https://shopvnb.com/giay-cau-long-yonex-court-flow.html";
		String categoryName = "Giày cầu lông";
		System.out.println("Bắt đầu cào dữ liệu từ URL: " + url);
		try {
			int count = productCrawlerService.crawlProducts(url, categoryName);
			System.out.println("Cào dữ liệu thành công! Đã cào được " + count + " sản phẩm.");
		} catch (Exception e) {
			System.err.println("Lỗi khi cào dữ liệu: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Test
	@Transactional
	void testPrintLastProduct() {
		List<Product> products = productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
		if (!products.isEmpty()) {
			Product last = products.get(0);
			System.out.println("====== LAST PRODUCT ======");
			System.out.println("ID: " + last.getId());
			System.out.println("Name: " + last.getName());
			System.out.println("Price: " + last.getPrice());
			System.out.println("Category ID: " + last.getCategoryId());
			System.out.println("Category: " + (last.getCategory() != null ? last.getCategory().getName() : "null"));
			System.out.println("Variants count: " + (last.getVariants() != null ? last.getVariants().size() : 0));
			if (last.getVariants() != null) {
				last.getVariants().forEach(var -> {
					System.out.println("  Variant: Size " + var.getSize() + " | Color: " + var.getColor() + " | Price: " + var.getPrice());
					if (var.getImages() != null) {
						var.getImages().forEach(img -> System.out.println("    Image: " + img.getImageUrl() + " | Color: " + img.getColor() + " | IsMain: " + img.getIsMain()));
					}
				});
			}
			System.out.println("==========================");
		} else {
			System.out.println("No products found in DB!");
		}
	}

}
