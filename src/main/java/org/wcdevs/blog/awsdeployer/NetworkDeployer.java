package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.Network;
import org.wcdevs.blog.cdk.Util;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public class NetworkDeployer {
  private static final String CONSTRUCT_NAME = "network-stack";

  public static void main(String[] args) {
    var app = new App();

    String environmentName = Util.getValueInApp("environmentName", app);
    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);
    String applicationName = Util.getValueInApp("applicationName", app, false);
    String sslCertificate = Util.getValueInApp("sslCertificate", app, false);
    int internalPort = Util.getValueOrDefault("appInternalPort", app, 8080);
    int externalPort = Util.getValueOrDefault("appExternalPort", app, 80);
    var stackProps = StackProps.builder()
                               .stackName(Util.joinedString("-", environmentName, CONSTRUCT_NAME))
                               .env(Util.environmentFrom(accountId, region))
                               .build();
    var stack = new Stack(app, "NetworkStack", stackProps);

    var inputParams = Network.InputParameters.builder()
                                             .listeningInternalHttpPort(internalPort)
                                             .listeningExternalHttpPort(externalPort)
                                             .sslCertificateArn(sslCertificate).build();
    Network.newInstance(stack, CONSTRUCT_NAME, environmentName, /*applicationName,*/ inputParams);

    app.synth();
  }
}
