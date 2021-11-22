package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.ApplicationEnvironment;
import org.wcdevs.blog.cdk.CognitoStack;
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

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

public class BEElasticContainerServiceDeployer {
  private static final String CONSTRUCT_NAME = "BEECServiceApp";

  private static final String CORE_APP_DB_URL = "CORE_APP_DB_URL";
  private static final String CORE_APP_DB_USER = "CORE_APP_DB_USER";
  private static final String CORE_APP_DB_PASSWORD = "CORE_APP_DB_PASSWORD";
  private static final String CORE_APP_DB_DRIVER = "CORE_APP_DB_DRIVER";
  private static final String CORE_APP_DB_DRIVER_POSTGRES = "org.postgresql.Driver";

  private static final String CORE_APP_LISTEN_PORT = "CORE_APP_LISTEN_PORT";
  private static final String CORE_APP_MANAGEMENT_PORT = "CORE_APP_MANAGEMENT_PORT";

  private static final String ENVIRONMENT_NAME = "ENVIRONMENT_NAME";
  private static final String SPRING_PROFILES_ACTIVE = "SPRING_PROFILES_ACTIVE";
  private static final String AWS_REGION = "AWS_REGION";

  private static final String SERVICE_STACK_NAME = "be-service-stack";

  public static void main(String[] args) {
    var app = new App();

    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);
    String applicationName = Util.getValueInApp("applicationName", app);

    String environmentName = Util.getValueInApp("environmentName", app);
    var springProfile = Util.getValueOrDefault("springProfile", app, "aws");

    String dockerRepositoryName = Util.getValueInApp("dockerRepositoryName", app, false);
    String dockerImageTag = Util.getValueInApp("dockerImageTag", app, false);
    String dockerImageUrl = Util.getValueInApp("dockerImageUrl", app, false);

    var appListenPort = Util.getValueOrDefault("appPort", app, "8080");
    var appHealthCheckPath = Util.getValueOrDefault("healthCheckPath", app, "/");
    var appHealthCheckPort = Util.getValueOrDefault("healthCheckPort", app, "8080");

    var awsEnvironment = Util.environmentFrom(accountId, region);
    var appEnv = new ApplicationEnvironment(applicationName, environmentName);

    var serviceStack = serviceStack(app, appEnv, awsEnvironment);
    var parametersStack = EnvVarsUtil.parametersStack(app, SERVICE_STACK_NAME, appEnv,
                                                      awsEnvironment);

    var dbOutputParams = Database.outputParametersFrom(parametersStack, appEnv);
    var cognitoParams = CognitoStack.getOutputParameters(parametersStack,
                                                         appEnv.getEnvironmentName());

    var commonEnvVar = commonEnvVars(region, environmentName, springProfile, appListenPort,
                                     appHealthCheckPort);
    var dbEnvVar = dbEnvVars(serviceStack, dbOutputParams);
    var cognitoEnvVar = EnvVarsUtil.cognitoEnvVars(serviceStack, appEnv, cognitoParams);

    var environmentVariables = EnvVarsUtil.environmentVariables(commonEnvVar, dbEnvVar,
                                                                cognitoEnvVar);
    var secGroupIdsToGrantIngressFromEcs = secGroupIdAccessFromEcs(dbOutputParams);
    var dockerImage = dockerImage(dockerRepositoryName, dockerImageTag, dockerImageUrl);
    var inputParameters = inputParameters(dockerImage, environmentVariables, appListenPort,
                                          appHealthCheckPath, appHealthCheckPort,
                                          secGroupIdsToGrantIngressFromEcs);

    var networkOutputParameters = Network.outputParametersFrom(serviceStack,
                                                               appEnv);

    ElasticContainerService.newInstance(serviceStack, CONSTRUCT_NAME, awsEnvironment,
                                        appEnv, inputParameters,
                                        networkOutputParameters);
    app.synth();
  }

  private static Stack serviceStack(App app, ApplicationEnvironment applicationEnvironment,
                                    Environment awsEnvironment) {
    var serviceStackName = applicationEnvironment.prefixed(SERVICE_STACK_NAME);
    return new Stack(app, "BEServiceStack", StackProps.builder()
                                                      .stackName(serviceStackName)
                                                      .env(awsEnvironment)
                                                      .build());
  }

  private static List<String> secGroupIdAccessFromEcs(Database.OutputParameters dbOutput) {
    return List.of(dbOutput.getDbSecurityGroupId());
  }

  private static Map<String, String> commonEnvVars(String awsRegion, String environmentName,
                                                   String springProfile, String listenPort,
                                                   String healthCheckPort) {
    return Map.ofEntries(entry(CORE_APP_LISTEN_PORT, listenPort),
                         entry(CORE_APP_MANAGEMENT_PORT, healthCheckPort),
                         entry(ENVIRONMENT_NAME, environmentName),
                         entry(SPRING_PROFILES_ACTIVE, springProfile),
                         entry(AWS_REGION, awsRegion));
  }

  private static Map<String, String> dbEnvVars(Construct scope,
                                               Database.OutputParameters dbOutput) {
    var dbEndpointAddress = dbOutput.getEndpointAddress();
    var dbEndpointPort = dbOutput.getEndpointPort();
    var dbName = dbOutput.getDbName();
    var springDataSourceUrl = String.format("jdbc:postgresql://%s:%s/%s",
                                            dbEndpointAddress, dbEndpointPort, dbName);

    var dbSecret = Database.getDataBaseSecret(scope, dbOutput);
    var dbUsername = dbSecret.secretValueFromJson(Database.USERNAME_SECRET_HOLDER).toString();
    var dbPassword = dbSecret.secretValueFromJson(Database.PASSWORD_SECRET_HOLDER).toString();

    return Map.ofEntries(entry(CORE_APP_DB_DRIVER, CORE_APP_DB_DRIVER_POSTGRES),
                         entry(CORE_APP_DB_URL, springDataSourceUrl),
                         entry(CORE_APP_DB_USER, dbUsername),
                         entry(CORE_APP_DB_PASSWORD, dbPassword));
  }

  private static ElasticContainerService.DockerImage dockerImage(String dockerRepositoryName,
                                                                 String dockerImageTag,
                                                                 String dockerImageUrl) {
    return ElasticContainerService.DockerImage.builder()
                                              .dockerRepositoryName(dockerRepositoryName)
                                              .dockerImageTag(dockerImageTag)
                                              .dockerImageUrl(dockerImageUrl)
                                              .build();
  }

  private static ElasticContainerService.InputParameters inputParameters(
      ElasticContainerService.DockerImage dockerImage, Map<String, String> envVariables,
      String appPort, String healthCheckPath, String healthCheckPort,
      List<String> secGIdsGrantIngressFEcs
                                                                        ) {
    var defaultPort = 8080;
    return ElasticContainerService.InputParameters
        .builder()
        .dockerImage(dockerImage)
        .environmentVariables(envVariables)
        .securityGroupIdsToGrantIngressFromEcs(secGIdsGrantIngressFEcs)
        .taskRolePolicyStatements(taskRolePolicyStatements())
        .applicationPort(intValueFrom(appPort, defaultPort))
        .healthCheckPort(intValueFrom(healthCheckPort, defaultPort))
        .healthCheckPath(healthCheckPath)
        .healthCheckIntervalSeconds(60)
        .desiredInstancesCount(1)
        .build();
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
