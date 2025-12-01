package io.github.lvoxx.srms.kitchen.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import io.github.lvoxx.srms.kitchen.AbstractDatabaseTestContainer;
import io.github.lvoxx.srms.kitchen.models.KitchenMenu;
import io.github.lvoxx.srms.kitchen.models.MenuCategory;
import lombok.extern.slf4j.Slf4j;
import reactor.test.StepVerifier;

@Slf4j
@DataR2dbcTest
@ActiveProfiles("repo")
@DisplayName("KitchenMenu Repository Tests")
@Tags({
        @Tag("Repository"), @Tag("Integration")
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class KitchenMenuRepositoryTest extends AbstractDatabaseTestContainer {
    @Autowired
    private KitchenMenuRepository menuRepository;

    @Autowired
    private MenuCategoryRepository categoryRepository;

    private MenuCategory testCategory;
    private KitchenMenu availableMenu;
    private KitchenMenu unavailableMenu;

    @BeforeEach
    void setUp() {
        menuRepository.deleteAll().block();
        categoryRepository.deleteAll().block();

        // Create test category
        testCategory = MenuCategory.builder()
                .id(UUID.randomUUID())
                .categoryName("Món nướng")
                .isActive(true)
                .build();
        categoryRepository.save(testCategory).block();

        // Create available menu
        availableMenu = KitchenMenu.builder()
                .id(UUID.randomUUID())
                .menuName("Gà nướng")
                .menuCtgId(testCategory.getId())
                .price(new BigDecimal("150000"))
                .minQuantity(1)
                .maxQuantity(10)
                .unit("phần")
                .isAvailable(true)
                .preparationTimeMinutes(30)
                .build();

        // Create unavailable menu
        unavailableMenu = KitchenMenu.builder()
                .id(UUID.randomUUID())
                .menuName("Hải sản nướng")
                .menuCtgId(testCategory.getId())
                .price(new BigDecimal("250000"))
                .minQuantity(1)
                .maxQuantity(5)
                .unit("phần")
                .isAvailable(false)
                .build();

        menuRepository.save(availableMenu).block();
        menuRepository.save(unavailableMenu).block();
    }

    @Test
    void testSaveAndFindById() {
        KitchenMenu newMenu = KitchenMenu.builder()
                .id(UUID.randomUUID())
                .menuName("Bò nướng")
                .menuCtgId(testCategory.getId())
                .price(new BigDecimal("180000"))
                .minQuantity(1)
                .maxQuantity(10)
                .unit("phần")
                .isAvailable(true)
                .build();

        StepVerifier.create(menuRepository.save(newMenu))
                .assertNext(saved -> {
                    assertThat(saved.getMenuName()).isEqualTo("Bò nướng");
                    assertThat(saved.getPrice()).isEqualByComparingTo(new BigDecimal("180000"));
                })
                .verifyComplete();

        StepVerifier.create(menuRepository.findById(newMenu.getId()))
                .assertNext(found -> {
                    assertThat(found.getMenuName()).isEqualTo("Bò nướng");
                })
                .verifyComplete();
    }

    @Test
    void testFindByMenuCtgId() {
        StepVerifier.create(menuRepository.findByMenuCtgId(testCategory.getId()))
                .expectNextCount(2) // availableMenu and unavailableMenu
                .verifyComplete();
    }

    @Test
    void testFindByIsAvailableTrue() {
        StepVerifier.create(menuRepository.findByIsAvailableTrue())
                .assertNext(menu -> {
                    assertThat(menu.getIsAvailable()).isTrue();
                    assertThat(menu.getMenuName()).isEqualTo("Gà nướng");
                })
                .verifyComplete();
    }

    @Test
    void testFindByMenuCtgIdAndIsAvailableTrue() {
        StepVerifier.create(
                menuRepository.findByMenuCtgIdAndIsAvailableTrue(
                        testCategory.getId(),
                        true))
                .assertNext(menu -> {
                    assertThat(menu.getIsAvailable()).isTrue();
                    assertThat(menu.getMenuCtgId()).isEqualTo(testCategory.getId());
                })
                .verifyComplete();
    }

    @Test
    void testFindAvailableMenuByCategory() {
        StepVerifier.create(menuRepository.findAvailableMenuByCategory(testCategory.getId()))
                .assertNext(menu -> {
                    assertThat(menu.getIsAvailable()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void testCountByCategory() {
        StepVerifier.create(menuRepository.countByCategory(testCategory.getId()))
                .assertNext(count -> {
                    assertThat(count).isEqualTo(2L);
                })
                .verifyComplete();
    }

    @Test
    void testUpdateMenu() {
        availableMenu.setPrice(new BigDecimal("160000"));
        availableMenu.setIsAvailable(false);

        StepVerifier.create(menuRepository.save(availableMenu))
                .assertNext(updated -> {
                    assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("160000"));
                    assertThat(updated.getIsAvailable()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void testDeleteMenu() {
        UUID menuId = unavailableMenu.getId();

        StepVerifier.create(menuRepository.deleteById(menuId))
                .verifyComplete();

        StepVerifier.create(menuRepository.findById(menuId))
                .verifyComplete();
    }

    @Test
    void testMenuWithNullPrice() {
        KitchenMenu freeMenu = KitchenMenu.builder()
                .id(UUID.randomUUID())
                .menuName("Khăn lạnh")
                .menuCtgId(testCategory.getId())
                .price(null)
                .minQuantity(1)
                .maxQuantity(100)
                .unit("cái")
                .isAvailable(true)
                .build();

        StepVerifier.create(menuRepository.save(freeMenu))
                .assertNext(saved -> {
                    assertThat(saved.getPrice()).isNull();
                    assertThat(saved.getMenuName()).isEqualTo("Khăn lạnh");
                })
                .verifyComplete();
    }
}