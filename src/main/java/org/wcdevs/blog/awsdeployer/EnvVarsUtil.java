package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.ApplicationEnvironment;
import org.wcdevs.blog.cdk.CognitoStack;
import org.wcdevs.blog.cdk.Util;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;

public final class EnvVarsUtil {
  private static final String COGNITO_PROVIDER_URL = "COGNITO_PROVIDER_URL";
  private static final String COGNITO_CLIENT_ID = "COGNITO_CLIENT_ID";
  private static final String COGNITO_CLIENT_NAME = "COGNITO_CLIENT_NAME";
  private static final String COGNITO_CLIENT_SECRET = "COGNITO_CLIENT_SECRET";

  private static final String PARAMETERS_STACK = "parameters";
  private EnvVarsUtil() {

  }

  static Map<String, String> cognitoEnvVars(Stack scope, ApplicationEnvironment appEnv,
                                            CognitoStack.OutputParameters cognitoParams) {
    var cognitoClientSecret = CognitoStack.getUserPoolClientSecret(scope, appEnv);
    var cognitoClientSecretValue
        = cognitoClientSecret.secretValueFromJson(CognitoStack.USER_POOL_CLIENT_SECRET_HOLDER)
                             .toString();
    var cognitoClientId
        = cognitoClientSecret.secretValueFromJson(CognitoStack.USER_POOL_CLIENT_ID_HOLDER)
                             .toString();
    var cognitoClientName
        = cognitoClientSecret.secretValueFromJson(CognitoStack.USER_POOL_CLIENT_NAME_HOLDER)
                             .toString();

    return Map.ofEntries(entry(COGNITO_PROVIDER_URL, cognitoParams.getProviderUrl()),
                         entry(COGNITO_CLIENT_ID, cognitoClientId),
                         entry(COGNITO_CLIENT_NAME, cognitoClientName),
                         entry(COGNITO_CLIENT_SECRET, cognitoClientSecretValue));
  }


  static Map<String, String> environmentVariables(Map<String, String> commonEnvVar,
                                                  Map<String, String> dbEnvVar,
                                                  Map<String, String> cognitoEnvVar) {
    return Stream.concat(commonEnvVar.entrySet().stream(),
                         Stream.concat(dbEnvVar.entrySet().stream(),
                                       cognitoEnvVar.entrySet().stream()))
                 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  static Map<String, String> environmentVariables(Map<String, String> commonEnvVar,
                                                          Map<String, String> cognitoEnvVar) {
    return Stream.concat(commonEnvVar.entrySet().stream(),
                         cognitoEnvVar.entrySet().stream())
                 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  static Stack parametersStack(App app, String stackId, ApplicationEnvironment applicationEnvironment,
                               Environment awsEnvironment) {
    var timeId = getTimeId();
    var pStackName = Util.joinedString(Util.DASH_JOINER, PARAMETERS_STACK, stackId, timeId);
    var prefixedParamsStackName = applicationEnvironment.prefixed(pStackName);

    return new Stack(app, prefixedParamsStackName + timeId,
                     StackProps.builder()
                               .stackName(prefixedParamsStackName)
                               .env(awsEnvironment)
                               .build());
  }

  private static String getTimeId() {
    long timestamp = System.currentTimeMillis();
    var utc = LocalDateTime.now(ZoneId.of("UTC"));
    return Util.joinedString(Util.DASH_JOINER, utc.getYear(), utc.getMonthValue(),
                             utc.getDayOfMonth(), utc.getHour(), utc.getMinute(), utc.getSecond(),
                             timestamp);
  }
}
