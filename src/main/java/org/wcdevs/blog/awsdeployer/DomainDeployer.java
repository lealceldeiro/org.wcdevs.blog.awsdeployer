package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.ApplicationEnvironment;
import org.wcdevs.blog.cdk.DomainStack;
import org.wcdevs.blog.cdk.Util;
import software.amazon.awscdk.core.App;

import java.util.Optional;

public class DomainDeployer {
  private static final String CONSTRUCT_NAME = "DomainApp";

  public static void main(String[] args) {
    App app = new App();

    String applicationName = Util.getValueInApp("applicationName", app);
    String environmentName = Util.getValueInApp("environmentName", app);
    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);
    String hostedZoneDomain = Util.getValueInApp("hostedZoneDomain", app);
    String applicationDomain = Util.getValueInApp("applicationDomain", app);
    Boolean sslActivated = Util.getValueInApp("isSslActivated", app, false);
    boolean isSslActivated = Optional.ofNullable(sslActivated).orElse(true);

    var awsEnvironment = Util.environmentFrom(accountId, region);
    var appEnvironment = new ApplicationEnvironment(applicationName, environmentName);

    var inputParams = DomainStack.InputParameters.builder()
                                                 .sslCertificateActivated(isSslActivated)
                                                 .build();

    DomainStack.newInstance(app, CONSTRUCT_NAME, awsEnvironment, appEnvironment, hostedZoneDomain,
                            applicationDomain, inputParams);
    app.synth();
  }
}
