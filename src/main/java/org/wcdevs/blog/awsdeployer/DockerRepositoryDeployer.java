package org.wcdevs.blog.awsdeployer;

import static org.wcdevs.blog.awsdeployer.Util.environmentFrom;
import static org.wcdevs.blog.awsdeployer.Util.getValueInApp;

import org.wcdevs.blog.cdk.DockerRepository;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public class DockerRepositoryDeployer {
  private static final String NAME = "DockerRepository";

  public static void main(String[] args) {
    App app = new App();

    String accountId = getValueInApp("accountId", app);
    String region = getValueInApp("region", app);
    String applicationName = getValueInApp("applicationName", app);

    StackProps stackProps = StackProps.builder()
                                      .stackName(Util.joinedString("-", applicationName, NAME))
                                      .env(environmentFrom(accountId, region))
                                      .build();
    Stack dockerRepositoryStack = new Stack(app, Util.string(NAME, "Stack"), stackProps);
    DockerRepository.InputParameter inputParameter
        = new DockerRepository.InputParameter(Util.joinedString("-", applicationName, NAME), accountId);

    DockerRepository dockerRepository =
        DockerRepository.newInstance(dockerRepositoryStack, NAME, inputParameter);

    String cfnOutput = "OUTPUT";
    CfnOutput.Builder.create(dockerRepository, Util.string(NAME, cfnOutput))
                     .exportName(Util.joinedString("-", NAME, cfnOutput))
                     .value(cfnOutput)
                     .build();
    app.synth();
  }
}
