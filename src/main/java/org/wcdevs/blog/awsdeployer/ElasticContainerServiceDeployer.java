package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.ApplicationEnvironment;
import org.wcdevs.blog.cdk.Database;
import org.wcdevs.blog.cdk.ElasticContainerService;
import org.wcdevs.blog.cdk.Network;
import org.wcdevs.blog.cdk.Util;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ElasticContainerServiceDeployer {
  private static final String CONSTRUCT_NAME = "ECServiceApp";

  private static final String SPRING_PROFILES_ACTIVE = "SPRING_PROFILES_ACTIVE";
  private static final String CORE_APP_DB_URL = "CORE_APP_DB_URL";
  private static final String CORE_APP_DB_USER = "CORE_APP_DB_USER";
  private static final String CORE_APP_DB_PASSWORD = "CORE_APP_DB_PASSWORD";
  private static final String CORE_APP_DB_DRIVER = "CORE_APP_DB_DRIVER";
  private static final String CORE_APP_DB_DRIVER_POSTGRES = "org.postgresql.Driver";
  private static final String CORE_APP_LISTEN_PORT = "CORE_APP_LISTEN_PORT";
  private static final String CORE_APP_MANAGEMENT_PORT = "CORE_APP_MANAGEMENT_PORT";
  private static final String ENVIRONMENT_NAME = "ENVIRONMENT_NAME";

  public static void main(String[] args) {
    var app = new App();

    String environmentName = Util.getValueInApp("environmentName", app);
    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);
    String applicationName = Util.getValueInApp("applicationName", app);
    String springProfile = Util.getValueInApp("springProfile", app);
    String dockerRepositoryName = Util.getValueInApp("dockerRepositoryName", app, false);
    String dockerImageTag = Util.getValueInApp("dockerImageTag", app, false);
    String dockerImageUrl = Util.getValueInApp("dockerImageUrl", app, false);

    String appPort = Util.getValueInApp("appPort", app, false);
    var appListenPort = Optional.ofNullable(appPort).orElse("8080");

    String healthCheckPath = Util.getValueInApp("healthCheckPath", app, false);
    var appHealthCheckPath = Optional.ofNullable(healthCheckPath).orElse("/");

    String healthCheckPort = Util.getValueInApp("healthCheckPort", app, false);
    var appHealthCheckPort = Optional.ofNullable(healthCheckPort).orElse("8080");

    var awsEnvironment = Util.environmentFrom(accountId, region);
    var applicationEnvironment = new ApplicationEnvironment(applicationName, environmentName);

    var parametersStack = parametersStack(app, applicationEnvironment, awsEnvironment);
    var serviceStack = serviceStack(app, applicationEnvironment, awsEnvironment);

    var dbOutputParameters = Database.outputParametersFrom(parametersStack,
                                                           applicationEnvironment);

    var environmentVariables = environmentVariables(serviceStack, springProfile, environmentName,
                                                    appListenPort, appHealthCheckPort,
                                                    dbOutputParameters);
    var secGroupIdsToGrantIngressFromEcs = secGroupIdAccessFromEcs(dbOutputParameters);

    var dockerImage = ElasticContainerService.newDockerImage(dockerRepositoryName, dockerImageTag,
                                                             dockerImageUrl);
    var inputParameters = inputParameters(dockerImage, environmentVariables, appPort,
                                          appHealthCheckPath, appHealthCheckPort,
                                          secGroupIdsToGrantIngressFromEcs);

    var networkOutputParameters = Network.outputParametersFrom(serviceStack, environmentName);

    ElasticContainerService.newInstance(serviceStack, CONSTRUCT_NAME, awsEnvironment,
                                        applicationEnvironment, inputParameters,
                                        networkOutputParameters);

    app.synth();
  }

  private static Stack parametersStack(App app, ApplicationEnvironment applicationEnvironment,
                                       Environment awsEnvironment) {
    var timeId = getTimeId();
    var paramsStackName = applicationEnvironment.prefixed("parameters-service-stack" + timeId);

    return new Stack(app, "parameters-service-stack" + timeId,
                     StackProps.builder().stackName(paramsStackName).env(awsEnvironment).build());
  }

  private static String getTimeId() {
    long timestamp = System.currentTimeMillis();
    var utc = LocalDateTime.now(ZoneId.of("UTC"));
    return Util.joinedString("-", utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth(),
                             utc.getHour(), utc.getMinute(), utc.getSecond(), timestamp);
  }

  private static Stack serviceStack(App app, ApplicationEnvironment applicationEnvironment,
                                    Environment awsEnvironment) {
    var serviceStackName = applicationEnvironment.prefixed("service-stack");
    return new Stack(app, "ServiceStack", StackProps.builder()
                                                    .stackName(serviceStackName)
                                                    .env(awsEnvironment)
                                                    .build());
  }

  private static List<String> secGroupIdAccessFromEcs(Database.OutputParameters dbOutput) {
    return List.of(dbOutput.getDbSecurityGroupId());
  }

  private static Map<String, String> environmentVariables(Construct scope, String springProfile,
                                                          String environmentName,
                                                          String appListenPort,
                                                          String appHealthCheckPort,
                                                          Database.OutputParameters dbOutput) {
    var dbEndpointAddress = dbOutput.getEndpointAddress();
    var dbEndpointPort = dbOutput.getEndpointPort();
    var dbName = dbOutput.getDbName();
    var springDataSourceUrl = String.format("jdbc:postgresql://%s:%s/%s",
                                            dbEndpointAddress, dbEndpointPort, dbName);

    var dbSecret = Database.getDataBaseSecret(scope, dbOutput);
    var dbUsername = dbSecret.secretValueFromJson(Database.USERNAME_SECRET_HOLDER).toString();
    var dbPassword = dbSecret.secretValueFromJson(Database.PASSWORD_SECRET_HOLDER).toString();

    return Map.of(SPRING_PROFILES_ACTIVE, springProfile,
                  CORE_APP_DB_URL, springDataSourceUrl,
                  CORE_APP_DB_USER, dbUsername,
                  CORE_APP_DB_PASSWORD, dbPassword,
                  CORE_APP_DB_DRIVER, CORE_APP_DB_DRIVER_POSTGRES,
                  CORE_APP_LISTEN_PORT, appListenPort,
                  CORE_APP_MANAGEMENT_PORT, appHealthCheckPort,
                  ENVIRONMENT_NAME, environmentName);
  }

  private static ElasticContainerService.InputParameters inputParameters(
      ElasticContainerService.DockerImage dockerImage, Map<String, String> envVariables,
      String appPort, String healthCheckPath, String healthCheckPort,
      List<String> secGIdsGrantIngressFEcs
                                                                        ) {
    var defaultPort = 8080;

    var inputParameters = ElasticContainerService.newInputParameters(dockerImage, envVariables,
                                                                     secGIdsGrantIngressFEcs);
    inputParameters.setTaskRolePolicyStatements(taskRolePolicyStatements());
    inputParameters.setApplicationPort(intValueFrom(appPort, defaultPort));
    inputParameters.setHealthCheckPort(intValueFrom(healthCheckPort, defaultPort));
    inputParameters.setHealthCheckPath(healthCheckPath);
    inputParameters.setAwsLogsDateTimeFormat("%Y-%m-%dT%H:%M:%S.%f%z");
    inputParameters.setHealthCheckIntervalSeconds(45);

    return inputParameters;
  }

  private static int intValueFrom(String rawValue, int defaultIfError) {
    try {
      return Integer.parseInt(rawValue);
    } catch (Exception ignored) {
      // let's use the default one
    }
    return defaultIfError;
  }

  private static List<PolicyStatement> taskRolePolicyStatements() {
    var sqsActions = List.of("sqs:DeleteMessage", "sqs:GetQueueUrl",
                             "sqs:ListDeadLetterSourceQueues", "sqs:ListQueues",
                             "sqs:ListQueueTags", "sqs:ReceiveMessage", "sqs:SendMessage",
                             "sqs:ChangeMessageVisibility", "sqs:GetQueueAttributes");
    return List.of(policyStatement(sqsActions),
                   policyStatement("cognito-idp:*"),
                   policyStatement("ses:*"),
                   policyStatement("dynamodb:*"),
                   policyStatement("cloudwatch:PutMetricData"));
  }

  private static PolicyStatement policyStatement(String action) {
    return policyStatement(List.of(action));
  }

  private static PolicyStatement policyStatement(List<String> actions) {
    return PolicyStatement.Builder.create()
                                  .effect(Effect.ALLOW)
                                  .resources(List.of("*"))
                                  .actions(actions)
                                  .build();
  }
}
