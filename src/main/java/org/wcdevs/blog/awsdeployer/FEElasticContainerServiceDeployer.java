package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.ApplicationEnvironment;
import org.wcdevs.blog.cdk.ElasticContainerService;
import org.wcdevs.blog.cdk.Network;
import org.wcdevs.blog.cdk.Util;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FEElasticContainerServiceDeployer {
  private static final String FE_APP_LISTEN_PORT = "FE_APP_LISTEN_PORT";
  private static final String FE_APP_MANAGEMENT_PORT = "FE_APP_MANAGEMENT_PORT";
  private static final String ENVIRONMENT_NAME = "ENVIRONMENT_NAME";

  private static final String CONSTRUCT_NAME = "FEECServiceApp";
  private static final String SERVICE_STACK_NAME = "fe-service-stack";

  public static void main(String[] args) {
    var app = new App();

    String environmentName = Util.getValueInApp("environmentName", app);
    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);
    String applicationName = Util.getValueInApp("applicationName", app);
    String dockerRepositoryName = Util.getValueInApp("dockerRepositoryName", app, false);
    String dockerImageTag = Util.getValueInApp("dockerImageTag", app, false);
    String dockerImageUrl = Util.getValueInApp("dockerImageUrl", app, false);
    var appListenPort = Util.getValueOrDefault("appPort", app, "3000");
    var appHealthCheckPath = Util.getValueOrDefault("healthCheckPath", app, "/");
    var appHealthCheckPort = Util.getValueOrDefault("healthCheckPort", app, "3000");

    var awsEnvironment = Util.environmentFrom(accountId, region);
    var applicationEnvironment = new ApplicationEnvironment(applicationName, environmentName);

    var serviceStack = serviceStack(app, applicationEnvironment, awsEnvironment);
    var networkOutputParameters = Network.outputParametersFrom(serviceStack,
                                                               applicationEnvironment);

    var environmentVariables = environmentVariables(environmentName, appListenPort,
                                                    appHealthCheckPort);
    var dockerImage = ElasticContainerService.newDockerImage(dockerRepositoryName, dockerImageTag,
                                                             dockerImageUrl);
    var inputParameters = inputParameters(dockerImage, environmentVariables, appListenPort,
                                          appHealthCheckPath, appHealthCheckPort);

    ElasticContainerService.newInstance(serviceStack, CONSTRUCT_NAME, awsEnvironment,
                                        applicationEnvironment, inputParameters,
                                        networkOutputParameters);

    app.synth();
  }

  private static Stack serviceStack(App app, ApplicationEnvironment applicationEnvironment,
                                    Environment awsEnvironment) {
    var serviceStackName = applicationEnvironment.prefixed(SERVICE_STACK_NAME);
    return new Stack(app, "FEServiceStack", StackProps.builder()
                                                      .stackName(serviceStackName)
                                                      .env(awsEnvironment)
                                                      .build());
  }

  private static Map<String, String> environmentVariables(String environmentName,
                                                          String appListenPort,
                                                          String appHealthCheckPort) {
    return Map.of(FE_APP_LISTEN_PORT, appListenPort,
                  FE_APP_MANAGEMENT_PORT, appHealthCheckPort,
                  ENVIRONMENT_NAME, environmentName);
  }

  private static ElasticContainerService.InputParameters inputParameters(
      ElasticContainerService.DockerImage dockerImage, Map<String, String> envVariables,
      String appPort, String healthCheckPath, String healthCheckPort
                                                                        ) {
    var defaultPort = 8080;

    var inputParameters = ElasticContainerService.newInputParameters(dockerImage, envVariables,
                                                                     Collections.emptyList());
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
    return List.of(policyStatement("cognito-idp:*"), policyStatement("cloudwatch:PutMetricData"));
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
