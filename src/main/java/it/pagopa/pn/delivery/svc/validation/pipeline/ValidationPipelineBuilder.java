package it.pagopa.pn.delivery.svc.validation.pipeline;

import it.pagopa.pn.delivery.svc.validation.validators.AuthorizationValidator;
import it.pagopa.pn.delivery.svc.validation.validators.BusinessValidator;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import it.pagopa.pn.delivery.svc.validation.context.ValidationContext;

import java.util.ArrayList;
import java.util.List;

public class ValidationPipelineBuilder<C extends ValidationContext> {
    private List<FormalValidator<? super C>> formal = new ArrayList<>();
    private List<AuthorizationValidator<? super C>> authorization = new ArrayList<>();
    private List<BusinessValidator<? super C>> business = new ArrayList<>();

    public final ValidationPipelineBuilder<C> authorization(AuthorizationValidator<? super C> validators) {
        this.authorization.add(validators);
        return this;
    }

    public final ValidationPipelineBuilder<C> business(BusinessValidator<? super C> validators) {
        this.business.add(validators);
        return this;
    }

    public final ValidationPipelineBuilder<C> formal(FormalValidator<? super C> validators) {
        this.formal.add(validators);
        return this;
    }

    public ValidationPipeline<C> build() {
        return new ValidationPipeline<>(formal, authorization, business);
    }
}
