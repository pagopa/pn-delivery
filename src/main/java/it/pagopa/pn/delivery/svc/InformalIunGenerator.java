package it.pagopa.pn.delivery.svc;

public class InformalIunGenerator extends IunGenerator {
    private static final String VERSION_CHAR = "A";

    @Override
    protected String getVersionChar() {
        return VERSION_CHAR;
    }
}
