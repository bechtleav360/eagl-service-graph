package com.bechtle.cougar.graph.domain.validators.checkRequiredType;

import com.bechtle.cougar.graph.domain.model.errors.MissingType;
import com.bechtle.cougar.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.cougar.graph.domain.services.EntityServices;
import com.bechtle.cougar.graph.domain.services.handler.Validator;
import com.bechtle.cougar.graph.repository.EntityStore;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j(topic = "cougar.graph.validator.type")
@Component
@ConditionalOnProperty(name = "application.features.validators.checkRequiredType", havingValue = "true")
public class CheckRequiredType implements Validator {

    @Override
    public Mono<? extends AbstractModel> handle(EntityServices entityServices, AbstractModel model, Map<String, String> parameters) {
        log.trace("(Validator) Checking if type is defined");

        for (Resource obj : model.getModel().subjects()) {
            /* check if each node object has a valid type definition */
            if (!model.getModel().contains(obj, RDF.TYPE, null)) {
                log.error("(Validator) The object {} is missing a type", obj);
                return Mono.error(new MissingType("Missing type definition for object"));
            }
        }
        return Mono.just(model);
    }

}
