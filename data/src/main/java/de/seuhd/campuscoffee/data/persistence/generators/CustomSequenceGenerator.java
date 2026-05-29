package de.seuhd.campuscoffee.data.persistence.generators;

import org.hibernate.MappingException;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import java.util.Properties;

/**
 * Hibernate ID generator that derives the sequence name from the entity's table name (table
 * "users" uses sequence "users_seq") and matches the increment of 1 used by the Flyway-created
 * sequences.
 */
public class CustomSequenceGenerator extends SequenceStyleGenerator {

    @Override
    public void configure(GeneratorCreationContext creationContext, Properties parameters)
            throws MappingException {
        String tableName = creationContext.getValue().getTable().getName();
        parameters.setProperty(SEQUENCE_PARAM, tableName + "_seq");
        parameters.setProperty(INCREMENT_PARAM, "1");
        super.configure(creationContext, parameters);
    }
}
