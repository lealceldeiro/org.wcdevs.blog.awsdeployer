package org.wcdevs.blog.awsdeployer;

import static org.wcdevs.blog.awsdeployer.Util.environmentFrom;
import static org.wcdevs.blog.awsdeployer.Util.getValueInApp;
import static org.wcdevs.blog.awsdeployer.Util.string;
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
                                      .stackName(string("-", applicationName, NAME))
                                      .env(environmentFrom(accountId, region))
                                      .build();
    Stack dockerRepositoryStack = new Stack(app, string(NAME, "Stack"), stackProps);
    DockerRepository.InputParameter inputParameter
        = new DockerRepository.InputParameter(string("-", applicationName, NAME), accountId);

    DockerRepository dockerRepository =
        DockerRepository.newInstance(dockerRepositoryStack, NAME, inputParameter);

    String cfnOutput = "OUTPUT";
    CfnOutput.Builder.create(dockerRepository, string(NAME, cfnOutput))
                     .exportName(string("-", NAME, cfnOutput))
                     .value(cfnOutput)
                     .build();
    app.synth();
  }
}
