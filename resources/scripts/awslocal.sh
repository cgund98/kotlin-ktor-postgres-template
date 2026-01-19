#!/bin/bash
# AWS CLI wrapper for LocalStack
# Usage: awslocal.sh <aws-cli-command> [arguments...]
# Example: awslocal.sh sns list-topics
# Example: awslocal.sh sqs list-queues

ENDPOINT_URL="${AWS_ENDPOINT_URL:-http://localstack:4566}"
REGION="${AWS_REGION:-us-east-1}"

# Override AWS credentials for LocalStack (LocalStack doesn't validate these)
export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-test}"
export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-test}"
export AWS_DEFAULT_REGION="${REGION}"

# Pass all arguments to AWS CLI with LocalStack endpoint
aws --endpoint-url="$ENDPOINT_URL" --region="$REGION" "$@"

