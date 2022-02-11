package com.bechtle.eagl.graph.domain.model.wrapper;

import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.model.extensions.NamespacedModelBuilder;
import org.apache.logging.log4j.util.StringBuilders;
import org.eclipse.rdf4j.model.*;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AbstractModel implements NamespaceAware, Serializable {

    private NamespacedModelBuilder modelBuilder;

    protected AbstractModel(Model model) {
        this.modelBuilder = new NamespacedModelBuilder(model, Set.of());
    }

    protected AbstractModel() {
        this.modelBuilder = new NamespacedModelBuilder();
    }


    @Override
    public Set<Namespace> getNamespaces() {
        return this.getModel().getNamespaces();
    }

    public NamespacedModelBuilder getBuilder() {
        return modelBuilder;
    }

    public Model getModel() {
        return getBuilder().build();
    }

    public void reset() {
        this.modelBuilder = new NamespacedModelBuilder();
    }

    public Stream<NamespaceAwareStatement> stream() {
        return this.getModel().stream().map(sts -> NamespaceAwareStatement.wrap(sts, getNamespaces()));
    }

    public Iterable<? extends NamespaceAwareStatement> asStatements() {
        return this.stream().toList();
    }

    public Stream<Value> streamValues(Resource subject, IRI predicate) {
        Iterable<Statement> statements = this.getModel().getStatements(subject, predicate, null);
        return StreamSupport.stream(statements.spliterator(), true).map(Statement::getObject);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        this.getModel().forEach(statement -> sb.append(statement).append('\n'));
        return sb.toString();
    }





}