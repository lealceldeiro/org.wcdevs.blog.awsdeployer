package org.wcdevs.blog.awsdeployer;

import org.wcdevs.blog.cdk.ApplicationEnvironment;
import org.wcdevs.blog.cdk.PostgreSQL;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public class PostgreSqlDeployer {
  private static final String PG_DATABASE = "PostgreSQLDatabase";

  public static void main(String[] args) {
    App app = new App();

    String applicationName = Util.getValueInApp("applicationName", app);
    String environmentName = Util.getValueInApp("environmentName", app);
    String accountId = Util.getValueInApp("accountId", app);
    String region = Util.getValueInApp("region", app);

    var awsEnvironment = Util.environmentFrom(accountId, region);
    var applicationEnvironment = new ApplicationEnvironment(applicationName, environmentName);
    var stackProps = StackProps.builder()
                             .stackName(applicationEnvironment.prefixed(PG_DATABASE))
                             .env(awsEnvironment)
                             .build();
    var databaseStack = new Stack(app, "DatabaseStack", stackProps);
    var inputParameters = PostgreSQL.newInputParameters();

    PostgreSQL.newInstance(databaseStack, PG_DATABASE, applicationEnvironment, inputParameters);

    app.synth();
  }
}
