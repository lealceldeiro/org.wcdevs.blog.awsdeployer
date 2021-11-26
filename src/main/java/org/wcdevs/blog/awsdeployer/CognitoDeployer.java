package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.CognitoStack;
import org.wcdevs.blog.cdk.Util;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.services.cognito.OAuthScope;

import java.util.Collections;
import java.util.List;

public class CognitoDeployer {
  private static final String DEFAULT_CALLBACKS = "";

  public static void main(String[] args) {
    var app = new App();

    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);
    String environmentName = Util.getValueInApp("environmentName", app);

    String domainPrefix = Util.getValueInApp("domainPrefix", app);
    String frontEndApplicationName = Util.getValueInApp("frontEndApplicationName", app);
    String frontEndApplicationUrl = Util.getValueInApp("frontEndApplicationUrl", app);
    var frontEndCallbacks = Util.getValueOrDefault("frontEndCallbacks", app, DEFAULT_CALLBACKS);

    String coreApplicationName = Util.getValueInApp("coreApplicationName", app);
    String coreApplicationUrl = Util.getValueInApp("coreApplicationUrl", app);
    var coreCallbacks = Util.getValueOrDefault("coreCallbacks", app, DEFAULT_CALLBACKS);

    var awsEnvironment = Util.environmentFrom(accountId, region);

    var appDomainPrefix = Util.joinedString(Util.DASH_JOINER, environmentName, domainPrefix);

    var frontEndCallbackUrls = DeployerUtil.valuesFromCommaSeparatedString(frontEndCallbacks,
                                                                           DEFAULT_CALLBACKS);
    var coreCallbackUrls = DeployerUtil.valuesFromCommaSeparatedString(coreCallbacks);

    var frontEndUserPoolClient = CognitoStack.UserPoolClientParameter
        .builder()
        .applicationName(frontEndApplicationName)
        .applicationUrl(frontEndApplicationUrl)
        .userPoolOauthCallBackUrls(frontEndCallbackUrls)
        .flowImplicitCodeGrantEnabled(true)
        .build();
    var coreUserPoolClient = CognitoStack.UserPoolClientParameter
        .builder()
        .applicationName(coreApplicationName)
        .applicationUrl(coreApplicationUrl)
        .userPoolOauthCallBackUrls(coreCallbackUrls)
        .flowClientCredentialsEnabled(true)
        .scopes(Collections.emptyList())
        .build();
    var input = CognitoStack.InputParameters
        .builder()
        .loginPageDomainPrefix(appDomainPrefix)
        .userPoolClientConfigurations(List.of(frontEndUserPoolClient, coreUserPoolClient))
        .build();

    CognitoStack.newInstance(app, awsEnvironment, environmentName, input);

    app.synth();
  }
}
