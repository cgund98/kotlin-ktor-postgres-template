package com.github.cgund98.template.infrastructure.events.publisher

import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sns.model.PublishRequest
import com.github.cgund98.template.infrastructure.events.registry.EventEnvelope
import com.github.cgund98.template.infrastructure.events.serializer.EventSerializer
import io.github.oshai.kotlinlogging.KotlinLogging

class SnsEventPublisher(
    private val serializer: EventSerializer,
    private val snsClient: SnsClient,
    private val topicArn: String,
) : EventPublisher {
    val logger = KotlinLogging.logger { }

    override suspend fun <T : EventEnvelope<Any>> publish(event: T) {
        logger.debug { "Publishing to topic ${this@SnsEventPublisher.topicArn} on region ${snsClient.config.region}" }
        val messageBody = serializer.serialize(event)

        val request =
            PublishRequest {
                message = messageBody
                topicArn = this@SnsEventPublisher.topicArn

                messageAttributes =
                    mapOf(
                        "event_type" to
                            aws.sdk.kotlin.services.sns.model.MessageAttributeValue {
                                dataType = "String"
                                stringValue = event.type.toString()
                            },
                    )
            }

        snsClient.publish(request)
    }
}
