package org.wcdevs.blog.awsdeployer;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public class Bootstrapper {
  public static void main(final String[] args) {
    App app = new App();

    new Stack(app, "Bootstrap", StackProps.builder() .build());

    app.synth();
  }
}
