package com.bechtle.cougar.graph.domain.transformers.replaceGlobalIdentifiers;

import com.bechtle.cougar.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.cougar.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.cougar.graph.domain.model.extensions.NamespacedModelBuilder;
import com.bechtle.cougar.graph.domain.model.vocabulary.Local;
import com.bechtle.cougar.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.cougar.graph.domain.services.EntityServices;
import com.bechtle.cougar.graph.domain.services.QueryServices;
import com.bechtle.cougar.graph.domain.services.handler.Transformer;
import com.bechtle.cougar.graph.repository.EntityStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Will check any entity IRI in the model, if doesn't conform to the internal schema, a new identifier is generated (we keep the old one)
 *
 * Potential for conflict:
 *
 * the new identifier is a hash of the old identifier to have reproducible results. If the identifier is reused (e.g. example.org/identifier), all
 * new statements are aggregated to one big entity. We could add a random see into it, but that means we cannot reproduce it anymore
 *
 *
 */
@Slf4j(topic = "cougar.graph.transformer.identifiers")
@Component
@ConditionalOnProperty(name = "application.features.transformers.replaceGlobalIdentifiers", havingValue = "true")
public class ReplaceGlobalIdentifiers implements Transformer {

    @Override
    public Mono<? extends AbstractModel> handle(AbstractModel triples, Map<String, String> parameters, Authentication authentication) {


        log.trace("(Transformer) Regenerating identifiers");

        List<NamespaceAwareStatement> copy = Collections.unmodifiableList(triples.streamNamespaceAwareStatements().toList());

        Map<Resource, GeneratedIdentifier> mappings = triples.getModel().subjects().stream()
                .filter(Value::isIRI)
                .filter(iri -> !GeneratedIdentifier.is((IRI) iri, Local.Entities.NAMESPACE))
                .map(iri -> Pair.of(iri, new GeneratedIdentifier(Local.Entities.NAMESPACE, iri)))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        triples.reset();

        NamespacedModelBuilder builder = triples.getBuilder();

        for (Statement st : copy) {
            if (mappings.containsKey(st.getSubject())) {
                builder.add(mappings.get(st.getSubject()), st.getPredicate(), st.getObject());
            } else if (st.getObject().isIRI() && mappings.containsKey((IRI) st.getObject())) {
                builder.add(st.getSubject(), st.getPredicate(), mappings.get((IRI) st.getObject()));
            } else {
                builder.add(st.getSubject(), st.getPredicate(), st.getObject());
            }
        }

        // preserve old ids
        mappings.forEach((key, value) -> {
            builder.add(value, Local.ORIGINAL_IDENTIFIER, key);
        });

        return Mono.just(triples);

    }

    @Override
    public void registerEntityService(EntityServices entityServices) {

    }

    @Override
    public void registerQueryService(QueryServices queryServices) {

    }
}
