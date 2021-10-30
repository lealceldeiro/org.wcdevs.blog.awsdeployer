package org.wcdevs.blog.awsdeployer;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public class Bootstrapper {
  private static final String CONSTRUCT_NAME = "BootstrapApp";

  public static void main(final String[] args) {
    App app = new App();

    new Stack(app, CONSTRUCT_NAME, StackProps.builder().build());

    app.synth();
  }
}
