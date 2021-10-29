package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.DeploymentSequencerStack;
import software.amazon.awscdk.core.App;

public class DeploymentSequencerDeployer {
  private static final String NAME = "sequencerStack";

  public static void main(String[] args) {
    App app = new App();

    String applicationName = Util.getValueInApp("applicationName", app);
    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);
    String githubToken = Util.getValueInApp("githubToken", app);
    var awsEnvironment = Util.environmentFrom(accountId, region);

    var inputParams = DeploymentSequencerStack.InputParameters
        .builder()
        .codeDirectory("./deployment-sequencer-lambda/dist/lambda.zip")
        .githubToken(githubToken)
        .build();
    DeploymentSequencerStack.newInstance(app, NAME, awsEnvironment, applicationName, inputParams);

    app.synth();
  }
}
