package com.github.cgund98.template.infrastructure.events.consumer

import aws.sdk.kotlin.runtime.AwsServiceException
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.DeleteMessageRequest
import aws.sdk.kotlin.services.sqs.model.Message
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageResponse
import com.github.cgund98.template.infrastructure.events.registry.EventEnvelope
import com.github.cgund98.template.infrastructure.events.serializer.EventSerializer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SqsOptions(
    val queueUrl: String,
    val maxNumberOfMessages: Int = 10,
    val visibilityTimeout: Int = 30,
    val waitTimeSeconds: Int = 20,
    val errorBackoff: Long = 5000,
)

@Suppress("TooGenericExceptionCaught")
class SqsEventConsumer<EventT : EventEnvelope<Any>>(
    private val sqsClient: SqsClient,
    private val handlerBinding: HandlerBinding<EventT>,
    private val serializer: EventSerializer,
    private val sqsOptions: SqsOptions,
) : EventConsumer {
    val logger = KotlinLogging.logger {}

    // Start the consumer loop
    override suspend fun start() {
        logger.atInfo {
            message = "Starting SQS consumer"
            payload = mapOf("queueUrl" to sqsOptions.queueUrl)
        }

        while (currentCoroutineContext().isActive) {
            fetchAndHandleBatch()
        }
    }

    // Fetch a batch of messages from the queue and handle each one
    suspend fun fetchAndHandleBatch() {
        val receiveRequest =
            ReceiveMessageRequest {
                queueUrl = sqsOptions.queueUrl
                maxNumberOfMessages = sqsOptions.maxNumberOfMessages
                visibilityTimeout = sqsOptions.visibilityTimeout
                waitTimeSeconds = sqsOptions.waitTimeSeconds
            }

        val response: ReceiveMessageResponse

        try {
            response = sqsClient.receiveMessage(receiveRequest)
        } catch (e: AwsServiceException) {
            logger.atError {
                this.message = "Encountered error while fetching batch of messages"
                this.cause = e
                payload = mapOf("queueUrl" to sqsOptions.queueUrl)
            }
            delay(sqsOptions.errorBackoff)
            return
        }

        val messages = response.messages ?: return

        // Process messages in parallel to avoid Visibility Timeout expiration
        coroutineScope {
            messages.forEach { message ->
                launch {
                    try {
                        handleMessage(message)
                    } catch (e: RuntimeException) {
                        handleFailure(message, e)
                    }
                }
            }
        }

        logger.atInfo {
            message = "Finished processing batch of messages"
            payload = mapOf("queueUrl" to sqsOptions.queueUrl, "messageCount" to messages.size)
        }
    }

    // Handle a single message from the queue
    @Suppress("ThrowsCount")
    suspend fun handleMessage(message: Message) {
        val body = message.body ?: throw MessageBodyNotFoundException()
        val receiptHandle = message.receiptHandle ?: throw MessageIdNotFoundException()

        // Deserialize event
        val event: EventT
        try {
            event = serializer.deserialize(body, handlerBinding.eventType)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RuntimeException) {
            throw DeserializationException("Error deserializing message body", e)
        }

        // Apply handler logic
        try {
            handlerBinding.handler.handleEvent(event)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RuntimeException) {
            throw HandlerException("Error handling event", e)
        }

        // Delete message
        ack(receiptHandle)
    }

    private fun handleFailure(
        message: Message,
        e: Exception,
    ) {
        logger.atError {
            this.message = "Message processing failed"
            this.cause = e
            payload =
                mapOf(
                    "queueUrl" to sqsOptions.queueUrl,
                    "messageId" to message.messageId,
                )
        }
    }

    // Delete a message from the queue
    suspend fun ack(messageId: String) {
        val deleteRequest =
            DeleteMessageRequest {
                queueUrl = sqsOptions.queueUrl
                receiptHandle = messageId
            }

        sqsClient.deleteMessage(deleteRequest)
    }
}
