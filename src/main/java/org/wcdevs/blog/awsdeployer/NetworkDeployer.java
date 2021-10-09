package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.Network;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public class NetworkDeployer {
  private static final String NAME = "Network";

  public static void main(String[] args) {
    var app = new App();

    String environmentName = Util.getValueInApp("environmentName", app);
    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);
    String sslCertificate = Util.getValueInApp("sslCertificate", app, false);

    var inputParameters = Util.notEmpty(sslCertificate)
                          ? Network.newInputParameters(sslCertificate)
                          : Network.newInputParameters();
    var stackProps = StackProps.builder()
                               .stackName(Util.joinedString("-", environmentName, NAME))
                               .env(Util.environmentFrom(accountId, region))
                               .build();
    var stack = new Stack(app, "NetworkStack", stackProps);

    Network.newInstance(stack, NAME, environmentName, inputParameters);

    app.synth();
  }
}
