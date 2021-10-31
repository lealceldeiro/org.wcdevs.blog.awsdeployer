package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.AECService;
import org.wcdevs.blog.cdk.ApplicationEnvironment;
import org.wcdevs.blog.cdk.Database;
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

public class AECServiceDeployer {
  private static final String CONSTRUCT_NAME = "AECServiceApp";

  private static final String SPRING_PROFILES_ACTIVE = "SPRING_PROFILES_ACTIVE";
  private static final String SPRING_DATASOURCE_URL = "SPRING_DATASOURCE_URL";
  private static final String SPRING_DATASOURCE_USERNAME = "SPRING_DATASOURCE_USERNAME";
  private static final String SPRING_DATASOURCE_PASSWORD = "SPRING_DATASOURCE_PASSWORD";
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

    var awsEnvironment = Util.environmentFrom(accountId, region);
    var applicationEnvironment = new ApplicationEnvironment(applicationName, environmentName);

    var parametersStack = parametersStack(app, applicationEnvironment, awsEnvironment);
    var serviceStack = serviceStack(app, applicationEnvironment, awsEnvironment);

    var dbOutputParameters = Database.outputParametersFrom(parametersStack,
                                                           applicationEnvironment);

    var environmentVariables = environmentVariables(serviceStack, springProfile, environmentName,
                                                    dbOutputParameters);
    var secGroupIdsToGrantIngressFromEcs = secGroupIdAccessFromEcs(dbOutputParameters);

    var dockerImage = AECService.newDockerImage(dockerRepositoryName, dockerImageTag,
                                                dockerImageUrl);
    var inputParameters = inputParameters(dockerImage, environmentVariables,
                                          secGroupIdsToGrantIngressFromEcs);

    var networkOutputParameters = Network.outputParametersFrom(serviceStack, environmentName);

    AECService.newInstance(serviceStack, CONSTRUCT_NAME, awsEnvironment, applicationEnvironment,
                           inputParameters, networkOutputParameters);

    app.synth();
  }

  private static Stack parametersStack(App app, ApplicationEnvironment applicationEnvironment,
                                       Environment awsEnvironment) {
    var timeId = getTimeId();
    var paramsStackName = applicationEnvironment.prefixed("Service-Parameters-" + timeId);

    return new Stack(app, "ServiceParametersStack-" + timeId,
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
    var serviceStackName = applicationEnvironment.prefixed("Service");
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
                  SPRING_DATASOURCE_URL, springDataSourceUrl,
                  SPRING_DATASOURCE_USERNAME, dbUsername,
                  SPRING_DATASOURCE_PASSWORD, dbPassword,
                  ENVIRONMENT_NAME, environmentName);
  }

  private static AECService.InputParameters inputParameters(AECService.DockerImage dockerImage,
                                                            Map<String, String> envVariables,
                                                            List<String> secGIdsGrantIngressFEcs) {
    var inputParameters = AECService.newInputParameters(dockerImage, envVariables,
                                                        secGIdsGrantIngressFEcs);
    inputParameters.setTaskRolePolicyStatements(taskRolePolicyStatements());
    inputParameters.setHealthCheckPath("/actuator/health");
    inputParameters.setAwsLogsDateTimeFormat("%Y-%m-%dT%H:%M:%S.%f%z");
    inputParameters.setHealthCheckIntervalSeconds(45);

    return inputParameters;
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
