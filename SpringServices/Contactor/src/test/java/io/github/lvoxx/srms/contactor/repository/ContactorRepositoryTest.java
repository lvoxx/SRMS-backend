package io.github.lvoxx.srms.contactor.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.ActiveProfiles;

import io.github.lvoxx.srms.contactor.AbstractDatabaseTestContainer;

@AutoConfigureTestDatabase(replace = Replace.NONE) // Dont load String datasource autoconfig
@ActiveProfiles("test")
@DisplayName("Contactor Repository Tests")
@Tags({
                @Tag("Repository"), @Tag("Integration")
})
@DataR2dbcTest
public class ContactorRepositoryTest extends AbstractDatabaseTestContainer {

        @Autowired
        ContactorRepository repository;
}
