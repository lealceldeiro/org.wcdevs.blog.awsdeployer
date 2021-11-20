package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.ApplicationEnvironment;
import org.wcdevs.blog.cdk.CognitoStack;
import org.wcdevs.blog.cdk.Util;
import software.amazon.awscdk.core.App;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class CognitoDeployer {
  private static final String CONSTRUCT_NAME = "cognito-stack";
  private static final String DEFAULT_CALLBACKS = "";

  public static void main(String[] args) {
    var app = new App();

    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);
    String applicationName = Util.getValueInApp("applicationName", app);
    String environmentName = Util.getValueInApp("environmentName", app);

    String applicationUrl = Util.getValueInApp("applicationUrl", app);
    String domainPrefix = Util.getValueInApp("domainPrefix", app);
    var callback = Util.getValueOrDefault("callback", app, DEFAULT_CALLBACKS);

    var awsEnvironment = Util.environmentFrom(accountId, region);
    var appEnv = new ApplicationEnvironment(applicationName, environmentName);

    var appDomainPrefix = Util.joinedString(Util.DASH_JOINER, environmentName, domainPrefix);

    var callbackUrls = getCallbackUrls(callback);
    var input = CognitoStack.InputParameters.builder()
                                            .applicationUrl(applicationUrl)
                                            .loginPageDomainPrefix(appDomainPrefix)
                                            .applicationName(applicationName)
                                            .userPoolOauthCallBackUrls(callbackUrls)
                                            .build();

    CognitoStack.newInstance(app, CONSTRUCT_NAME, awsEnvironment, appEnv, input);

    app.synth();
  }

  private static List<String> getCallbackUrls(final String callback) {
    return Arrays.stream(Optional.ofNullable(callback)
                                 .orElse(DEFAULT_CALLBACKS)
                                 .split(","))
                 .filter(Objects::nonNull)
                 .map(String::trim)
                 .filter(s -> !s.isEmpty())
                 .collect(Collectors.toList());
  }
}
