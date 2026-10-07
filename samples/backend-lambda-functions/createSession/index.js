import {
  RekognitionClient,
  CreateFaceLivenessSessionCommand,
} from '@aws-sdk/client-rekognition';

const SUPPORTED_CHALLENGES = ['FaceMovementAndLightChallenge', 'FaceMovementChallenge'];

// The app sends {"challenge": "<type>"} to pick the session's challenge. A body that is
// missing, unparseable, or names an unknown challenge leaves the preferences unset, so
// Rekognition picks the challenge itself.
const challengeFromBody = (event) => {
  if (!event || !event.body) return undefined;
  try {
    const body = event.isBase64Encoded
      ? Buffer.from(event.body, 'base64').toString('utf8')
      : event.body;
    const { challenge } = JSON.parse(body);
    return SUPPORTED_CHALLENGES.includes(challenge) ? challenge : undefined;
  } catch {
    return undefined;
  }
};

/**
 * @type {import('@types/aws-lambda').APIGatewayProxyHandler}
 */

export const handler = async (event) => {
  const client = new RekognitionClient({ region: 'us-east-1' });
  const challenge = challengeFromBody(event);
  const command = new CreateFaceLivenessSessionCommand(
    challenge ? { Settings: { ChallengePreferences: [{ Type: challenge }] } } : {}
  );
  const response = await client.send(command);

  return {
    statusCode: 200,
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Headers': '*',
    },
    body: JSON.stringify({ sessionId: response.SessionId }),
  };
};
