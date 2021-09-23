# org.wcdevs.blog.awsdeployer
Holds the CDK applications and stack that uses CDK constructs to deploy resources [blog.wcdevs.org](https://blog.wcdevs.org)

This is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java
IDE to build and run tests.

The `cdk.json` file tells the CDK Toolkit how to execute your app.

## Useful commands

* `./mvnw package`     compile and run tests
* `cdk ls`          list all stacks in the app
* `cdk synth`       emits the synthesized CloudFormation template
* `cdk deploy`      deploy this stack to your default AWS account/region
* `cdk diff`        compare deployed stack with current state
* `cdk docs`        open CDK documentation
