package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.ApplicationEnvironment;
import org.wcdevs.blog.cdk.DeploymentSequencerStack;
import org.wcdevs.blog.cdk.Util;
import software.amazon.awscdk.App;

public class DeploymentSequencerDeployer {
  public static void main(String[] args) {
    App app = new App();

    String environmentName = Util.getValueInApp("environmentName", app);
    String applicationName = Util.getValueInApp("applicationName", app);
    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);
    String githubToken = Util.getValueInApp("githubToken", app);

    var awsEnvironment = Util.environmentFrom(accountId, region);
    var appEnv = new ApplicationEnvironment(applicationName, environmentName);
    var inputParams = DeploymentSequencerStack.InputParameters
        .builder()
        .codeDirectory("./deployment-sequencer-lambda/dist/lambda.zip")
        .githubToken(githubToken)
        .build();

    DeploymentSequencerStack.newInstance(app, awsEnvironment, appEnv, inputParams);

    app.synth();
  }
}
