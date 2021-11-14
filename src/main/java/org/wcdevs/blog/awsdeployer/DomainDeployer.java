package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.ApplicationEnvironment;
import org.wcdevs.blog.cdk.DomainStack;
import org.wcdevs.blog.cdk.Util;
import software.amazon.awscdk.core.App;

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

    var awsEnvironment = Util.environmentFrom(accountId, region);
    var appEnvironment = new ApplicationEnvironment(applicationName, environmentName);

    DomainStack.newInstance(app, CONSTRUCT_NAME, awsEnvironment, appEnvironment, hostedZoneDomain,
                            applicationDomain);
    app.synth();
  }
}
