package org.wcdevs.blog.awsdeployer;

import software.amazon.awscdk.core.App;

public class DeploymentSequencerDeployer {
  public static void main(String[] args) {
    App app = new App();

    String applicationName = Util.getValueInApp("applicationName", app);
    String environmentName = Util.getValueInApp("environmentName", app);
    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);
    String githubToken = Util.getValueInApp("githubToken", app);
    var awsEnvironment = Util.environmentFrom(accountId, region);

    // TODO:
    // DeploymentSequencerStack.newInstance

    app.synth();
  }
}
