# org.wcdevs.blog.awsdeployer

[![License: Apache](https://img.shields.io/badge/License-Apache%202.0-blue)](https://opensource.org/licenses/Apache-2.0) [![Maven Build](https://github.com/lealceldeiro/org.wcdevs.blog.awsdeployer/actions/workflows/maven.yml/badge.svg)](https://github.com/lealceldeiro/org.wcdevs.blog.awsdeployer/actions/workflows/maven.yml) [![CodeQL](https://github.com/lealceldeiro/org.wcdevs.blog.awsdeployer/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/lealceldeiro/org.wcdevs.blog.awsdeployer/actions/workflows/codeql-analysis.yml) [![Last Deployment](https://github.com/lealceldeiro/org.wcdevs.blog.awsdeployer/actions/workflows/deploy-app.yml/badge.svg)](https://github.com/lealceldeiro/org.wcdevs.blog.awsdeployer/actions/workflows/core-app-deployment.yml)

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

## Command examples

These commands represent examples of real ones that can be used to deploy to AWS from a local
environment to troubleshoot the deployers.

The values `000000000000` and `eu-central-1` should be replaced for the real AWS account id
and region. In each case, to undeploy/destroy the resource, `<resource>:destroy` should be used,
where `<resource>` is the name of the resource deployed (i.e.: `repository`, `network`).

The full list of scripts can be seen in the [package.json](./package.json) file.

* Deploy a docker repository:
```shell
npm run repository:deploy -- -c accountId=000000000000 -c region="eu-central-1"
```

* Deploy a network stack:
```shell
npm run network:deploy -- -c accountId=000000000000 -c region="eu-central-1" -c environmentName="staging"
```
