package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.ApplicationEnvironment;
import org.wcdevs.blog.cdk.CognitoStack;
import org.wcdevs.blog.cdk.Util;
import software.amazon.awscdk.core.App;

public class CognitoDeployer {
  private static final String CONSTRUCT_NAME = "shared-cognito-stack";

  public static void main(String[] args) {
    var app = new App();

    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);
    String applicationName = Util.getValueInApp("applicationName", app);
    String environmentName = Util.getValueInApp("environmentName", app);

    String applicationUrl = Util.getValueInApp("applicationUrl", app);
    String domainPrefix = Util.getValueInApp("domainPrefix", app);

    var awsEnvironment = Util.environmentFrom(accountId, region);
    var appEnv = new ApplicationEnvironment(applicationName, environmentName);

    var appDomainPrefix = Util.joinedString(Util.DASH_JOINER, environmentName, domainPrefix);

    var input = CognitoStack.InputParameters.builder()
                                            .applicationUrl(applicationUrl)
                                            .loginPageDomainPrefix(appDomainPrefix)
                                            .build();

    CognitoStack.newInstance(app, CONSTRUCT_NAME, awsEnvironment, appEnv, input);

    app.synth();
  }
}
