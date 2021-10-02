package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.DockerRepository;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

import static org.wcdevs.blog.awsdeployer.Util.environmentFrom;
import static org.wcdevs.blog.awsdeployer.Util.getValueInApp;
import static org.wcdevs.blog.awsdeployer.Util.string;

public class DockerRepositoryDeployer {
  private static final String NAME = "-docker-repository";

  public static void main(String[] args) {
    App app = new App();

    String accountId = getValueInApp("accountId", app);
    String region = getValueInApp("region", app);
    String applicationName = getValueInApp("applicationName", app);

    StackProps stackProps = StackProps.builder()
                                      .stackName(string(applicationName, NAME))
                                      .env(environmentFrom(accountId, region))
                                      .build();
    Stack dockerRepositoryStack = new Stack(app, string(NAME, "Stack"), stackProps);

    DockerRepository.InputParameters inputParameters
        = DockerRepository.newInputParameters(string(applicationName, NAME), accountId);
    DockerRepository.newInstance(dockerRepositoryStack, NAME, inputParameters);

    app.synth();
  }
}
