package com.bechtle.cougar.graph.repository.rdf4j.repository;

import com.bechtle.cougar.graph.repository.rdf4j.config.RepositoryConfiguration;
import com.bechtle.cougar.graph.domain.model.wrapper.Entity;
import com.bechtle.cougar.graph.repository.EntityStore;
import com.bechtle.cougar.graph.repository.rdf4j.repository.util.AbstractRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class EntityRepository extends AbstractRepository implements EntityStore {


    public EntityRepository() {
        super(RepositoryConfiguration.RepositoryType.ENTITIES);
    }


    public Mono<Entity> getEntity(IRI id, Authentication authentication) {
            try (RepositoryConnection connection = getConnection(authentication)) {
                log.trace("(Store) Loading entity with id '{}' from repository {}", id,  connection.getRepository().toString());

                RepositoryResult<Statement> statements = connection.getStatements(id, null, null);
                if (!statements.hasNext()) {
                    if (log.isDebugEnabled()) log.debug("(Store) Found no statements for IRI: <{}>.", id);
                    return Mono.empty();
                }


                Entity entity = new Entity().withResult(statements);

                // embedded level 1
                entity.getModel().objects().stream()
                        .filter(Value::isIRI)
                        .map(value -> connection.getStatements((IRI) value, null, null))
                        .toList()
                        .forEach(entity::withResult);


                if (log.isDebugEnabled())
                    log.debug("(Store) Loaded {} statements for entity with IRI: <{}>.", entity.getModel().size(), id);
                return Mono.just(entity);

            } catch (Exception e) {
                log.error("Unknown error while running query", e);
                return Mono.error(e);
            }
    }




}
