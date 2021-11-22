package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.CognitoStack;
import org.wcdevs.blog.cdk.Util;
import software.amazon.awscdk.core.App;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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

    var frontEndCallbackUrls = getCallbackUrls(frontEndCallbacks);
    var coreCallbackUrls = getCallbackUrls(coreCallbacks);

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
        .flowAuthorizationCodeGrantEnabled(true)
        .build();
    var input = CognitoStack.InputParameters
        .builder()
        .loginPageDomainPrefix(appDomainPrefix)
        .userPoolClientConfigurations(List.of(frontEndUserPoolClient, coreUserPoolClient))
        .build();

    CognitoStack.newInstance(app, awsEnvironment, environmentName, input);

    app.synth();
  }

  private static List<String> getCallbackUrls(String callback) {
    return Arrays.stream(Optional.ofNullable(callback)
                                 .orElse(DEFAULT_CALLBACKS)
                                 .split(","))
                 .filter(Objects::nonNull)
                 .map(String::trim)
                 .filter(s -> !s.isEmpty())
                 .collect(Collectors.toList());
  }
}
