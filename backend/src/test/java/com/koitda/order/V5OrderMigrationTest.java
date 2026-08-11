package com.koitda.order;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.koitda.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** V5 스키마와 재구매 차단(부분 유니크) 검증. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class V5OrderMigrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	private long[] seedUserPatternItem(String nick) {
		long userId = jdbc.queryForObject("INSERT INTO users(nickname) VALUES (?) RETURNING id", Long.class, nick);
		long sellerUser = jdbc.queryForObject("INSERT INTO users(nickname) VALUES (?) RETURNING id", Long.class, nick + "셀러");
		long sellerId = jdbc.queryForObject("INSERT INTO seller_profile(user_id, brand_name) VALUES (?, ?) RETURNING id",
				Long.class, sellerUser, nick + "브랜드");
		long patternId = jdbc.queryForObject("""
				INSERT INTO selling_pattern(seller_id, title, craft_type, sale_price, product_status, published_at)
				VALUES (?, '유료 도안', 'KNIT', 10000, 'APPROVED', now()) RETURNING id
				""", Long.class, sellerId);
		long orderId = jdbc.queryForObject("""
				INSERT INTO customer_order(order_no, buyer_id, total_amount, payment_amount)
				VALUES (?, ?, 10000, 10000) RETURNING id
				""", Long.class, "ORD-" + nick, userId);
		long itemId = jdbc.queryForObject("""
				INSERT INTO order_item(order_id, pattern_id, seller_id, unit_price, item_amount)
				VALUES (?, ?, ?, 10000, 10000) RETURNING id
				""", Long.class, orderId, patternId, sellerId);
		return new long[] { userId, patternId, itemId };
	}

	@Test
	void 주문_테이블들이_생성된다() {
		List<String> tables = jdbc.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
		assertTrue(tables.containsAll(List.of("customer_order", "order_item", "pattern_library")),
				"생성된 테이블: " + tables);
	}

	@Test
	void 회수되지_않은_사용권은_도안당_하나뿐이고_환불후_재구매가_가능하다() {
		long[] ids = seedUserPatternItem("구매자A");
		long userId = ids[0], patternId = ids[1], itemId = ids[2];

		jdbc.update("INSERT INTO pattern_library(user_id, pattern_id, order_item_id) VALUES (?, ?, ?)",
				userId, patternId, itemId);

		// 같은 (user, pattern) 을 회수되지 않은 상태로 또 넣으면 부분 유니크 위반
		long itemId2 = jdbc.queryForObject("""
				INSERT INTO order_item(order_id, pattern_id, seller_id, unit_price, item_amount)
				SELECT order_id, pattern_id, seller_id, unit_price, item_amount FROM order_item WHERE id = ? RETURNING id
				""", Long.class, itemId);
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
				"INSERT INTO pattern_library(user_id, pattern_id, order_item_id) VALUES (?, ?, ?)",
				userId, patternId, itemId2));

		// 첫 사용권을 회수(revoked_at) 하면 재구매(새 사용권) 가능
		jdbc.update("UPDATE pattern_library SET revoked_at = now() WHERE user_id = ? AND pattern_id = ? AND revoked_at IS NULL",
				userId, patternId);
		assertDoesNotThrow(() -> jdbc.update(
				"INSERT INTO pattern_library(user_id, pattern_id, order_item_id) VALUES (?, ?, ?)",
				userId, patternId, itemId2));
	}
}
