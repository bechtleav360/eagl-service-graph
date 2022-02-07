package com.bechtle.eagl.graph.model.wrapper;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;

/**
 * Stores all items for one particular entity.
 *
 *
 *
 */
public class Entity extends AbstractModelWrapper<Entity> {


    public Entity withResult(RepositoryResult<Statement> statements) {
        statements.stream().parallel().forEach(statement -> this.getBuilder().add(statement.getSubject(), statement.getPredicate(), statement.getObject()));
        return this;
    }
}