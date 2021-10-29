package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.DockerRepository;
import org.wcdevs.blog.cdk.Util;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public class DockerRepositoryDeployer {
  private static final String CONSTRUCT_NAME = "DockerRepository";

  public static void main(String[] args) {
    var app = new App();

    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);
    String applicationName = Util.getValueInApp("applicationName", app);

    var stackProps = StackProps.builder()
                               .stackName(Util.string(applicationName, CONSTRUCT_NAME))
                               .env(Util.environmentFrom(accountId, region))
                               .build();
    var dockerRepositoryStack = new Stack(app, Util.string(CONSTRUCT_NAME, "Stack"), stackProps);

    var inputParams = DockerRepository.newInputParameters(Util.string(applicationName,
                                                                      CONSTRUCT_NAME),
                                                          accountId);
    DockerRepository.newInstance(dockerRepositoryStack, CONSTRUCT_NAME, inputParams);

    app.synth();
  }
}
