import {
  GetQueueAttributesCommand,
  SQSClient,
  GetQueueAttributesResult
} from "@aws-sdk/client-sqs";
import axios from "axios";

export const handler = async (e: SqsEvent): Promise<any> => {
  const queueUrl = process.env.QUEUE_URL as string;
  const region = process.env.REGION as string;
  const githubToken = process.env.GITHUB_TOKEN as string;
  const event = new SqsEventWrapper(e);
  const latestDeploymentEvent = event.getLatestDeploymentEvent();
  const github = new GitHub(githubToken);
  const queue = new DeploymentQueue(queueUrl, region);

  console.log(`Event received: ${ JSON.stringify(latestDeploymentEvent) }`);

  if (await queue.hasWaitingEvents()) {
    console.log("Event skipped. Will handle future events waiting in the queue.");
    return;
  }

  if (await github.isWorkflowCurrentlyRunning(latestDeploymentEvent)) {
    console.log("GitHub workflow is currently running. It must be retried later!");
    throw "Workflow must be retried later.";
  }
  await github.triggerWorkflow(latestDeploymentEvent);
}

// region mapping
interface SqsEvent {
  Records: Record[];
}

interface Record {
  messageId: string;
  body: string;
  attributes: RecordAttributes;
}

interface RecordAttributes {
  SequenceNumber: string;
}

class SqsEventWrapper {
  event: SqsEvent;

  constructor(event: SqsEvent) {
    this.event = event;
  }

  getLatestDeploymentEvent(): DeploymentEvent {
    return JSON.parse(
        this.event.Records.sort((record1, record2) => {
          return (
              Number(record1.attributes.SequenceNumber) - Number(record2.attributes.SequenceNumber)
          );
        }).reverse()
            .shift().body
    );
  }
}

interface DeploymentEvent {
  commitSha: string;
  ref: string;
  owner: string;
  repo: string;
  workflowId: string;
  dockerImageTag: string;
}

class GitHub {
  static API = "https://api.github.com";
  static WORKFLOW_STATUS_COMPLETED = "completed";
  githubToken: string;

  constructor(githubToken: string) {
    this.githubToken = githubToken;
  }

  async isWorkflowCurrentlyRunning(e: DeploymentEvent): Promise<boolean> {
    console.log(
        `Querying GitHub API to check if workflow is running for event: ${ JSON.stringify(e) }`
    );

    // https://docs.github.com/en/rest/reference/actions#list-workflow-runs
    const response = await axios.get(
        `${ GitHub.API }/repos/${ e.owner }/${ e.repo }/actions/workflows/${ e.workflowId }/runs`,
        this.axiosConfig()
    );

    const inProgressWorkflowRuns = response.data['workflow_runs'] || [].filter(
        run => run.status != GitHub.WORKFLOW_STATUS_COMPLETED
    );

    return Promise.resolve(inProgressWorkflowRuns.length > 0);
  }

  async triggerWorkflow(e: DeploymentEvent) {
    console.log(
        `Triggering GitHub workflow for event: ${ JSON.stringify(e) }`
    );

    const requestData = {
      ref: e.ref,
      inputs: { "docker-image-tag": e.dockerImageTag },
    };

    // https://docs.github.com/en/rest/reference/actions#create-a-workflow-dispatch-event
    await axios.post(
        `${ GitHub.API }/repos/${ e.owner }/${ e.repo }/actions/workflows/${ e.workflowId }/dispatches`,
        requestData,
        this.axiosConfig()
    );
  }

  private axiosConfig() {
    return {
      headers: {
        Authorization: `token ${ this.githubToken }`
      }
    };
  }
}

class DeploymentQueue {
  queueUrl: string;
  sqsClient: SQSClient;

  constructor(queueUrl: string, region: string) {
    this.queueUrl = queueUrl;
    this.sqsClient = new SQSClient({ region: region });
  }

  async hasWaitingEvents(): Promise<boolean> {
    console.log(`Checking queue ${ this.queueUrl } for waiting events`);

    const params = {
      QueueUrl: this.queueUrl,
      AttributeNames: [
        "ApproximateNumberOfMessages",
        "ApproximateNumberOfMessagesDelayed",
        "ApproximateNumberOfMessagesNotVisible",
      ]
    };
    const command = new GetQueueAttributesCommand(params);
    const data: GetQueueAttributesResult = await this.sqsClient.send(command);

    if (data.Attributes == undefined) {
      throw "GetQueueAttributesResult has no attributes!";
    }

    console.log(`GetQueueAttributesResult: ${ JSON.stringify(data) }`);

    const waitingMessages: number =
        this.intValue(data.Attributes["ApproximateNumberOfMessages"])
        + this.intValue(data.Attributes["ApproximateNumberOfMessagesDelayed"])
        + this.intValue(data.Attributes["ApproximateNumberOfMessagesNotVisible"])
        - 1; // the message currently processed by this Lambda is counted as a "not visible" message

    return Promise.resolve(waitingMessages > 0);
  }

  intValue(value: string): number {
    return parseInt(value);
  }
}

// endregion
