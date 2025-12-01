package io.github.lvoxx.srms.kitchen.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
import io.github.lvoxx.srms.kitchen.models.MenuCategory;
import lombok.extern.slf4j.Slf4j;
import reactor.test.StepVerifier;

@Slf4j
@DataR2dbcTest
@ActiveProfiles("repo")
@DisplayName("MenuCategory Repository Tests")
@Tags({
        @Tag("Repository"), @Tag("Integration")
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MenuCategoryRepositoryTest extends AbstractDatabaseTestContainer {
    @Autowired
    private MenuCategoryRepository repository;

    private MenuCategory rootCategory;
    private MenuCategory childCategory;

    @BeforeEach
    void setUp() {
        // Clean up
        repository.deleteAll().block();

        // Create test data
        rootCategory = MenuCategory.builder()
                .id(UUID.randomUUID())
                .categoryName("Món chính")
                .displayOrder(1)
                .isActive(true)
                .build();

        childCategory = MenuCategory.builder()
                .id(UUID.randomUUID())
                .ctgParentId(rootCategory.getId())
                .categoryName("Món nướng")
                .displayOrder(1)
                .isActive(true)
                .build();

        repository.save(rootCategory).block();
        repository.save(childCategory).block();
    }

    @Test
    void testSaveAndFindById() {
        MenuCategory category = MenuCategory.builder()
                .id(UUID.randomUUID())
                .categoryName("Món tráng miệng")
                .displayOrder(2)
                .isActive(true)
                .build();

        StepVerifier.create(repository.save(category))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isEqualTo(category.getId());
                    assertThat(saved.getCategoryName()).isEqualTo("Món tráng miệng");
                    assertThat(saved.getIsActive()).isTrue();
                })
                .verifyComplete();

        StepVerifier.create(repository.findById(category.getId()))
                .assertNext(found -> {
                    assertThat(found.getCategoryName()).isEqualTo("Món tráng miệng");
                })
                .verifyComplete();
    }

    @Test
    void testFindByCtgParentId() {
        StepVerifier.create(repository.findByCtgParentId(rootCategory.getId()))
                .assertNext(category -> {
                    assertThat(category.getId()).isEqualTo(childCategory.getId());
                    assertThat(category.getCategoryName()).isEqualTo("Món nướng");
                })
                .verifyComplete();
    }

    @Test
    void testFindByIsActiveTrue() {
        MenuCategory inactiveCategory = MenuCategory.builder()
                .id(UUID.randomUUID())
                .categoryName("Ngừng phục vụ")
                .displayOrder(99)
                .isActive(false)
                .build();
        repository.save(inactiveCategory).block();

        StepVerifier.create(repository.findByIsActiveTrue())
                .expectNextCount(2) // rootCategory and childCategory
                .verifyComplete();
    }

    @Test
    void testFindRootCategories() {
        StepVerifier.create(repository.findRootCategories())
                .assertNext(category -> {
                    assertThat(category.getId()).isEqualTo(rootCategory.getId());
                    assertThat(category.getCtgParentId()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void testUpdateCategory() {
        rootCategory.setCategoryName("Món chính (Cập nhật)");
        rootCategory.setDisplayOrder(10);

        StepVerifier.create(repository.save(rootCategory))
                .assertNext(updated -> {
                    assertThat(updated.getCategoryName()).isEqualTo("Món chính (Cập nhật)");
                    assertThat(updated.getDisplayOrder()).isEqualTo(10);
                })
                .verifyComplete();
    }

    @Test
    void testDeleteCategory() {
        UUID categoryId = childCategory.getId();

        StepVerifier.create(repository.deleteById(categoryId))
                .verifyComplete();

        StepVerifier.create(repository.findById(categoryId))
                .verifyComplete();
    }

    @Test
    void testFindCategoryWithChildren() {
        StepVerifier.create(repository.findCategoryWithChildren(rootCategory.getId()))
                .expectNextCount(2) // root + child
                .verifyComplete();
    }
}