# org.wcdevs.blog.awsdeployer

[![License: Apache](https://img.shields.io/badge/License-Apache%202.0-blue)](https://opensource.org/licenses/Apache-2.0) [![Maven Build](https://github.com/lealceldeiro/org.wcdevs.blog.awsdeployer/actions/workflows/maven.yml/badge.svg)](https://github.com/lealceldeiro/org.wcdevs.blog.awsdeployer/actions/workflows/maven.yml) [![CodeQL](https://github.com/lealceldeiro/org.wcdevs.blog.awsdeployer/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/lealceldeiro/org.wcdevs.blog.awsdeployer/actions/workflows/codeql-analysis.yml)

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
